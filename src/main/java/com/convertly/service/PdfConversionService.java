package com.convertly.service;

import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.entity.ConversionJob;
import com.convertly.processor.PdfProcessor;
import com.convertly.repository.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfConversionService {

    private static final Logger log = LoggerFactory.getLogger(PdfConversionService.class);

    private final PdfProcessor pdfProcessor;
    private final StorageService storageService;

    @Autowired(required = false)
    private ConversionJobRepository jobRepository;

    @Value("${convertly.storage.retention-minutes:15}")
    private int retentionMinutes;

    public PdfConversionService(PdfProcessor pdfProcessor, StorageService storageService) {
        this.pdfProcessor = pdfProcessor;
        this.storageService = storageService;
    }

    /**
     * Merges multiple PDF files into one combined PDF.
     */
    public ImageConversionResponseDto mergePdfs(List<MultipartFile> files) throws IOException {
        if (files == null || files.size() < 2) {
            throw new IllegalArgumentException("At least 2 PDF files are required for merging.");
        }

        List<byte[]> pdfByteList = new ArrayList<>();
        long totalInputBytes = 0;
        List<String> originalNames = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("One of the uploaded PDF files is empty: " + file.getOriginalFilename());
            }
            byte[] bytes = file.getBytes();
            pdfByteList.add(bytes);
            totalInputBytes += bytes.length;
            originalNames.add(file.getOriginalFilename());
        }

        String jobId = UUID.randomUUID().toString();
        String primaryFilename = "merged-" + files.size() + "-documents.pdf";

        recordJob(jobId, "PDF_MERGE", primaryFilename, "pdf", "pdf", totalInputBytes, "PROCESSING");

        try {
            PdfProcessor.PdfProcessResult result = pdfProcessor.mergePdfs(pdfByteList);
            storageService.saveEphemeralFile(jobId, primaryFilename, "pdf", result.data());

            recordJobSuccess(jobId, (long) result.data().length);

            return new ImageConversionResponseDto(
                    jobId,
                    primaryFilename,
                    "pdf (" + files.size() + " files)",
                    "pdf",
                    totalInputBytes,
                    result.byteSize(),
                    0,
                    result.pageCount(), // store page count in height field for display
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
     * Splits a PDF according to page range expression.
     */
    public ImageConversionResponseDto splitPdf(MultipartFile file, String pageRange) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a PDF file to split.");
        }

        byte[] inputBytes = file.getBytes();
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String jobId = UUID.randomUUID().toString();

        recordJob(jobId, "PDF_SPLIT", originalFilename, "pdf", "pdf", (long) inputBytes.length, "PROCESSING");

        try {
            PdfProcessor.PdfProcessResult result = pdfProcessor.splitPdf(inputBytes, pageRange);
            String outputName = "split-" + originalFilename;
            storageService.saveEphemeralFile(jobId, outputName, "pdf", result.data());

            recordJobSuccess(jobId, (long) result.data().length);

            return new ImageConversionResponseDto(
                    jobId,
                    outputName,
                    "pdf",
                    "pdf",
                    inputBytes.length,
                    result.byteSize(),
                    0,
                    result.pageCount(),
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
