package com.convertly.service;

import com.convertly.entity.ConversionJob;
import com.convertly.repository.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bundles multiple converted files into a single ZIP archive for batch download.
 */
@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private final StorageService storageService;
    private final ConversionJobRepository jobRepository;

    public BatchService(StorageService storageService, ConversionJobRepository jobRepository) {
        this.storageService = storageService;
        this.jobRepository = jobRepository;
    }

    /**
     * Builds a ZIP containing all files referenced by the given job IDs.
     * Jobs not found in storage are silently skipped with a warning log.
     *
     * @param jobIds list of job UUIDs
     * @return ZIP bytes
     */
    public byte[] buildZip(List<String> jobIds) throws Exception {
        if (jobIds == null || jobIds.isEmpty()) {
            throw new IllegalArgumentException("No job IDs provided");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            int added = 0;
            for (String jobId : jobIds) {
                // Validate UUID-ish format to prevent path traversal
                if (!jobId.matches("[a-zA-Z0-9\\-]{8,64}")) {
                    log.warn("Skipping invalid job ID format: {}", jobId);
                    continue;
                }

                String fileName = resolveFileName(jobId);
                if (fileName == null) {
                    log.warn("Job {} not found in storage or DB, skipping", jobId);
                    continue;
                }

                byte[] fileBytes = storageService.retrieve(jobId);
                if (fileBytes == null) {
                    log.warn("Job {} metadata found but file missing, skipping", jobId);
                    continue;
                }

                String entryName = deduplicateName(fileName, added);
                ZipEntry entry = new ZipEntry(entryName);
                entry.setSize(fileBytes.length);
                zos.putNextEntry(entry);
                zos.write(fileBytes);
                zos.closeEntry();
                added++;
            }

            if (added == 0) {
                throw new IllegalArgumentException(
                    "No valid files found for the provided job IDs. " +
                    "Files may have expired (15-minute TTL).");
            }
        }
        return baos.toByteArray();
    }

    /** Resolves the output filename for a job: checks in-memory storage first, then DB. */
    private String resolveFileName(String jobId) {
        StorageService.EphemeralFileMeta meta = storageService.getMetadata(jobId);
        if (meta != null) return meta.originalFilename();
        return tryDbFileName(jobId);
    }

    private String tryDbFileName(String jobId) {
        try {
            return jobRepository.findById(jobId)
                .map(ConversionJob::getOriginalFilename)
                .orElse(null);
        } catch (Exception e) {
            log.warn("DB fallback failed for {}: {}", jobId, e.getMessage());
            return null;
        }
    }

    /** Avoids duplicate filenames inside the ZIP by appending _N. */
    private String deduplicateName(String name, int index) {
        if (index == 0) return name;
        int dot = name.lastIndexOf('.');
        if (dot < 0) return name + "_" + index;
        return name.substring(0, dot) + "_" + index + name.substring(dot);
    }
}
