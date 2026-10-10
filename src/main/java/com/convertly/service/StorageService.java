package com.convertly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral Scratch Storage Service.
 * Ensures converted output files are stored only temporarily and automatically purged.
 * Zero permanent storage is retained on the local disk.
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${convertly.storage.scratch-dir:${java.io.tmpdir}/convertly_scratch}")
    private String scratchDirProperty;

    @Value("${convertly.storage.retention-minutes:15}")
    private int retentionMinutes;

    private Path scratchPath;

    // In-memory registry of active ephemeral files: storageKey -> FileMetadata
    private final Map<String, EphemeralFileMeta> activeFiles = new ConcurrentHashMap<>();

    public record EphemeralFileMeta(String storageKey, String originalFilename, String extension, Path filePath, Instant expiresAt) {}

    @PostConstruct
    public void init() {
        try {
            this.scratchPath = Paths.get(scratchDirProperty).toAbsolutePath().normalize();
            if (!Files.exists(scratchPath)) {
                Files.createDirectories(scratchPath);
                log.info("Initialized ephemeral scratch storage directory: {}", scratchPath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize ephemeral scratch directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    /**
     * Saves output bytes to an ephemeral temporary file.
     * Generates a safe server-side filename using the storageKey and extension.
     */
    public EphemeralFileMeta saveEphemeralFile(String storageKey, String originalFilename, String extension, byte[] data) throws IOException {
        String safeExtension = extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String safeFileName = storageKey + "." + safeExtension;
        Path targetPath = scratchPath.resolve(safeFileName).normalize();

        // Prevent directory traversal
        if (!targetPath.startsWith(scratchPath)) {
            throw new SecurityException("Storage path traversal attempted");
        }

        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(data);
        }

        Instant expiresAt = Instant.now().plusSeconds(retentionMinutes * 60L);
        EphemeralFileMeta meta = new EphemeralFileMeta(storageKey, originalFilename, safeExtension, targetPath, expiresAt);
        activeFiles.put(storageKey, meta);

        log.info("Saved ephemeral conversion result: key={}, size={} bytes, expires={}",
                storageKey, data.length, expiresAt);
        return meta;
    }

    /**
     * Retrieves an ephemeral file as a Spring Resource for download.
     */
    public Resource getEphemeralResource(String storageKey) {
        EphemeralFileMeta meta = activeFiles.get(storageKey);
        if (meta == null) {
            // Check disk fallback in case of restart
            File[] matches = scratchPath.toFile().listFiles((dir, name) -> name.startsWith(storageKey + "."));
            if (matches != null && matches.length > 0) {
                return new FileSystemResource(matches[0]);
            }
            return null;
        }

        File file = meta.filePath().toFile();
        if (file.exists() && file.canRead()) {
            return new FileSystemResource(file);
        }
        return null;
    }

    /**
     * Retrieves metadata for an ephemeral file.
     */
    public EphemeralFileMeta getMetadata(String storageKey) {
        return activeFiles.get(storageKey);
    }

    // -------------------------------------------------------------------------
    // Convenience record & methods for Phase 2 (audio/video/batch)
    // -------------------------------------------------------------------------

    /** Lightweight metadata record used by BatchService and new services. */
    public record FileMetadata(String fileName, String contentType, long sizeBytes) {}

    /**
     * Convenience store: saves bytes under the given key with a full filename and content-type.
     * Derives extension from the filename.
     */
    public void store(String storageKey, byte[] data, String fileName, String contentType) throws IOException {
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "bin";
        saveEphemeralFile(storageKey, fileName, ext, data);
    }

    /**
     * Retrieves the raw bytes for a stored file (returns null if not found or expired).
     */
    public byte[] retrieve(String storageKey) {
        try {
            EphemeralFileMeta meta = activeFiles.get(storageKey);
            if (meta != null && meta.filePath().toFile().exists()) {
                return Files.readAllBytes(meta.filePath());
            }
            // Disk fallback
            File[] matches = scratchPath.toFile().listFiles((dir, name) -> name.startsWith(storageKey + "."));
            if (matches != null && matches.length > 0) {
                return Files.readAllBytes(matches[0].toPath());
            }
        } catch (IOException e) {
            log.warn("Failed to retrieve file for key {}: {}", storageKey, e.getMessage());
        }
        return null;
    }

    /**
     * Returns the ephemeral scratch directory path (for processors that write temp files themselves).
     */
    public Path getScratchDirectory() {
        return scratchPath;
    }

    /**
     * Deletes a temporary file immediately (e.g. after download).
     */
    public boolean deleteEphemeralFile(String storageKey) {
        EphemeralFileMeta meta = activeFiles.remove(storageKey);
        if (meta != null) {
            try {
                return Files.deleteIfExists(meta.filePath());
            } catch (IOException e) {
                log.warn("Failed to delete temp file {}: {}", meta.filePath(), e.getMessage());
            }
        }
        return false;
    }

    /**
     * Scheduled cleanup job: purges temporary files exceeding retention window.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void purgeExpiredScratchFiles() {
        Instant now = Instant.now();
        activeFiles.entrySet().removeIf(entry -> {
            EphemeralFileMeta meta = entry.getValue();
            if (now.isAfter(meta.expiresAt())) {
                try {
                    Files.deleteIfExists(meta.filePath());
                    log.info("Purged expired ephemeral scratch file: {}", meta.filePath().getFileName());
                } catch (IOException e) {
                    log.warn("Error purging scratch file {}: {}", meta.filePath(), e.getMessage());
                }
                return true;
            }
            return false;
        });
    }
}
