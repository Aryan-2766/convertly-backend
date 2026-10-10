package com.convertly.service;

import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.entity.ConversionJob;
import com.convertly.processor.DocumentProcessor;
import com.convertly.repository.ConversionJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentConversionService {

    private final DocumentProcessor documentProcessor;
    private final StorageService storageService;

    @Autowired(required = false)
    private ConversionJobRepository jobRepository;

    @Value("${convertly.storage.retention-minutes:15}")
    private int retentionMinutes;

    public DocumentConversionService(DocumentProcessor documentProcessor, StorageService storageService) {
        this.documentProcessor = documentProcessor;
        this.storageService = storageService;
    }

    public ImageConversionResponseDto convertDocxToPdf(MultipartFile file) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a DOCX file to convert.");
        }
        return processOfficeDocument(file, "DOCX_TO_PDF", "docx", false);
    }

    public ImageConversionResponseDto convertPptxToPdf(MultipartFile file) throws IOException, InterruptedException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a PPTX presentation to convert.");
        }
        return processOfficeDocument(file, "PPTX_TO_PDF", "pptx", true);
    }

    private ImageConversionResponseDto processOfficeDocument(MultipartFile file, String operation, String inFmt, boolean isPptx) throws IOException, InterruptedException {
        byte[] inputBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : ("document." + inFmt);
        String jobId = UUID.randomUUID().toString();

        recordJob(jobId, operation, originalFilename, inFmt, "pdf", (long) inputBytes.length, "PROCESSING");

        try {
            DocumentProcessor.DocumentProcessResult result = isPptx
                    ? documentProcessor.convertPptxToPdf(inputBytes, originalFilename)
                    : documentProcessor.convertDocxToPdf(inputBytes, originalFilename);

            String baseName = originalFilename;
            int dotIdx = baseName.lastIndexOf('.');
            if (dotIdx > 0) baseName = baseName.substring(0, dotIdx);
            String outputName = baseName + ".pdf";

            storageService.saveEphemeralFile(jobId, outputName, "pdf", result.pdfData());
            recordJobSuccess(jobId, result.byteSize());

            return new ImageConversionResponseDto(
                    jobId,
                    originalFilename,
                    inFmt,
                    "pdf",
                    inputBytes.length,
                    result.byteSize(),
                    0,
                    0,
                    "/api/jobs/" + jobId + "/download",
                    "COMPLETED",
                    retentionMinutes
            );
        } catch (Exception e) {
            recordJobFailure(jobId, e.getMessage());
            throw e;
        }
    }

    private void recordJob(String jobId, String operation, String filename, String inFmt, String outFmt, Long size, String status) {
        if (jobRepository != null) {
            try {
                ConversionJob job = new ConversionJob(jobId, operation, filename, inFmt, outFmt, size, jobId);
                job.setStatus(status);
                jobRepository.save(job);
            } catch (Exception ignored) {}
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
            } catch (Exception ignored) {}
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
            } catch (Exception ignored) {}
        }
    }
}
