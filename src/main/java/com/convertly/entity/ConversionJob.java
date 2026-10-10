package com.convertly.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for tracking file conversion jobs and metadata in Aiven Cloud MySQL.
 * Strictly metadata only; file bytes are never stored in the database.
 */
@Entity
@Table(name = "conversion_jobs")
public class ConversionJob {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 32)
    private String operation; // e.g. IMAGE_CONVERT, IMAGE_RESIZE, PDF_MERGE

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "input_format", length = 16)
    private String inputFormat;

    @Column(name = "output_format", length = 16)
    private String outputFormat;

    @Column(name = "input_size_bytes")
    private Long inputSizeBytes;

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "storage_key", length = 64)
    private String storageKey; // Internal UUID for ephemeral scratch storage

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public ConversionJob() {
        this.createdAt = Instant.now();
        this.status = "PENDING";
    }

    public ConversionJob(String id, String operation, String originalFilename, String inputFormat,
                         String outputFormat, Long inputSizeBytes, String storageKey) {
        this.id = id;
        this.operation = operation;
        this.originalFilename = originalFilename;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.inputSizeBytes = inputSizeBytes;
        this.storageKey = storageKey;
        this.status = "PROCESSING";
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(900); // 15-minute expiration
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Long getInputSizeBytes() {
        return inputSizeBytes;
    }

    public void setInputSizeBytes(Long inputSizeBytes) {
        this.inputSizeBytes = inputSizeBytes;
    }

    public Long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public void setOutputSizeBytes(Long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
