package com.convertly.service;

import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.entity.ConversionJob;
import com.convertly.processor.ImageProcessor;
import com.convertly.repository.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class ImageConversionService {

    private static final Logger log = LoggerFactory.getLogger(ImageConversionService.class);

    private final ImageProcessor imageProcessor;
    private final StorageService storageService;

    @Autowired(required = false)
    private ConversionJobRepository jobRepository;

    @Value("${convertly.storage.retention-minutes:15}")
    private int retentionMinutes;

    public ImageConversionService(ImageProcessor imageProcessor, StorageService storageService) {
        this.imageProcessor = imageProcessor;
        this.storageService = storageService;
    }

    /**
     * Converts an uploaded image to the requested format.
     */
    public ImageConversionResponseDto processImageFormatConversion(MultipartFile file, String targetFormat, float quality) throws IOException {
        validateMultipartFile(file);

        byte[] inputBytes = file.getBytes();
        String detectedFormat = imageProcessor.detectImageFormat(inputBytes);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());

        String jobId = UUID.randomUUID().toString();
        String storageKey = jobId;

        recordJobInitial(jobId, "IMAGE_CONVERT", originalFilename, detectedFormat, targetFormat, (long) inputBytes.length, storageKey);

        try {
            ImageProcessor.ImageProcessResult result = imageProcessor.convertFormat(inputBytes, targetFormat, quality);
            storageService.saveEphemeralFile(storageKey, originalFilename, result.outputFormat(), result.data());

            recordJobSuccess(jobId, (long) result.data().length);

            return new ImageConversionResponseDto(
                    jobId,
                    originalFilename,
                    detectedFormat,
                    result.outputFormat(),
                    inputBytes.length,
                    result.data().length,
                    result.width(),
                    result.height(),
                    "/api/jobs/" + jobId + "/download",
                    "COMPLETED",
                    retentionMinutes
            );
        } catch (Exception e) {
            recordJobFailure(jobId, e.getMessage());
            throw e;
        }
    }

    /**
     * Resizes and converts an uploaded image.
     */
    public ImageConversionResponseDto processImageResize(MultipartFile file, int width, int height,
                                                         boolean keepAspectRatio, String targetFormat, float quality) throws IOException {
        validateMultipartFile(file);

        byte[] inputBytes = file.getBytes();
        String detectedFormat = imageProcessor.detectImageFormat(inputBytes);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());

        String outFormat = (targetFormat != null && !targetFormat.isBlank()) ? targetFormat : detectedFormat;
        String jobId = UUID.randomUUID().toString();
        String storageKey = jobId;

        recordJobInitial(jobId, "IMAGE_RESIZE", originalFilename, detectedFormat, outFormat, (long) inputBytes.length, storageKey);

        try {
            ImageProcessor.ImageProcessResult result = imageProcessor.resizeImage(
                    inputBytes, width, height, keepAspectRatio, outFormat, quality
            );
            storageService.saveEphemeralFile(storageKey, originalFilename, result.outputFormat(), result.data());

            recordJobSuccess(jobId, (long) result.data().length);

            return new ImageConversionResponseDto(
                    jobId,
                    originalFilename,
                    detectedFormat,
                    result.outputFormat(),
                    inputBytes.length,
                    result.data().length,
                    result.width(),
                    result.height(),
                    "/api/jobs/" + jobId + "/download",
                    "COMPLETED",
                    retentionMinutes
            );
        } catch (Exception e) {
            recordJobFailure(jobId, e.getMessage());
            throw e;
        }
    }

    private void validateMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided or file is empty");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 50MB maximum limit");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed-image";
        }
        // Keep only alphanumeric, dashes, dots, underscores
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
    }

    private void recordJobInitial(String jobId, String operation, String filename, String inFmt, String outFmt, Long size, String storageKey) {
        if (jobRepository != null) {
            try {
                ConversionJob job = new ConversionJob(jobId, operation, filename, inFmt, outFmt, size, storageKey);
                jobRepository.save(job);
            } catch (Exception e) {
                log.warn("Could not record initial job in MySQL (proceeding with ephemeral conversion): {}", e.getMessage());
            }
        }
    }

    private void recordJobSuccess(String jobId, Long outSize) {
        if (jobRepository != null) {
            try {
                jobRepository.findById(jobId).ifPresent(job -> {
                    job.setOutputSizeBytes(outSize);
                    job.setStatus("COMPLETED");
                    job.setCompletedAt(Instant.now());
                    jobRepository.save(job);
                });
            } catch (Exception e) {
                log.warn("Could not update completed job in MySQL: {}", e.getMessage());
            }
        }
    }

    private void recordJobFailure(String jobId, String error) {
        if (jobRepository != null) {
            try {
                jobRepository.findById(jobId).ifPresent(job -> {
                    job.setStatus("FAILED");
                    job.setErrorMessage(error != null && error.length() > 500 ? error.substring(0, 500) : error);
                    job.setCompletedAt(Instant.now());
                    jobRepository.save(job);
                });
            } catch (Exception e) {
                log.warn("Could not update failed job in MySQL: {}", e.getMessage());
            }
        }
    }
}
