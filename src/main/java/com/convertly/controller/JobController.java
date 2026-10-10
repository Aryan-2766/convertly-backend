package com.convertly.controller;

import com.convertly.entity.ConversionJob;
import com.convertly.repository.ConversionJobRepository;
import com.convertly.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final StorageService storageService;

    @Autowired(required = false)
    private ConversionJobRepository jobRepository;

    public JobController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * GET /api/jobs/{jobId}
     * Returns job status metadata.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        if (jobRepository != null) {
            try {
                Optional<ConversionJob> jobOpt = jobRepository.findById(jobId);
                if (jobOpt.isPresent()) {
                    return ResponseEntity.ok(jobOpt.get());
                }
            } catch (Exception ignored) {
            }
        }
        StorageService.EphemeralFileMeta meta = storageService.getMetadata(jobId);
        if (meta != null) {
            return ResponseEntity.ok(meta);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/jobs/{jobId}/download
     * Securely streams converted ephemeral file for download.
     */
    @GetMapping("/{jobId}/download")
    public ResponseEntity<Resource> downloadJobResult(@PathVariable String jobId) throws IOException {
        String storageKey = jobId;
        String downloadName = "converted-file";
        String extension = "bin";

        // Try database metadata if available
        if (jobRepository != null) {
            try {
                Optional<ConversionJob> jobOpt = jobRepository.findById(jobId);
                if (jobOpt.isPresent()) {
                    ConversionJob job = jobOpt.get();
                    if (job.getStorageKey() != null) {
                        storageKey = job.getStorageKey();
                    }
                    extension = job.getOutputFormat();
                    String baseName = job.getOriginalFilename();
                    int dotIdx = baseName.lastIndexOf('.');
                    if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
                    downloadName = baseName + "-converted." + extension;
                }
            } catch (Exception ignored) {
                // Graceful fallback to ephemeral scratch metadata
            }
        }

        // Ephemeral storage metadata (always in sync)
        StorageService.EphemeralFileMeta meta = storageService.getMetadata(storageKey);
        if (meta == null && !storageKey.equals(jobId)) {
            meta = storageService.getMetadata(jobId);
            if (meta != null) storageKey = jobId;
        }

        if (meta != null) {
            extension = meta.extension();
            String base = meta.originalFilename();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            downloadName = base + "-converted." + extension;
        }

        Resource resource = storageService.getEphemeralResource(storageKey);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = determineMediaType(extension);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    private MediaType determineMediaType(String extension) {
        if (extension == null) return MediaType.APPLICATION_OCTET_STREAM;
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
