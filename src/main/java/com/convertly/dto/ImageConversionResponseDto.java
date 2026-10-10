package com.convertly.dto;

public class ImageConversionResponseDto {

    private String jobId;
    private String originalFilename;
    private String inputFormat;
    private String outputFormat;
    private long inputSizeBytes;
    private long outputSizeBytes;
    private double sizeReductionPercent;
    private int width;
    private int height;
    private String downloadUrl;
    private String status;
    private int expiresInMinutes;

    public ImageConversionResponseDto() {}

    public ImageConversionResponseDto(String jobId, String originalFilename, String inputFormat,
                                      String outputFormat, long inputSizeBytes, long outputSizeBytes,
                                      int width, int height, String downloadUrl, String status, int expiresInMinutes) {
        this.jobId = jobId;
        this.originalFilename = originalFilename;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.inputSizeBytes = inputSizeBytes;
        this.outputSizeBytes = outputSizeBytes;
        this.width = width;
        this.height = height;
        this.downloadUrl = downloadUrl;
        this.status = status;
        this.expiresInMinutes = expiresInMinutes;

        if (inputSizeBytes > 0) {
            double diff = (double) (inputSizeBytes - outputSizeBytes) / inputSizeBytes * 100.0;
            this.sizeReductionPercent = Math.round(diff * 10.0) / 10.0;
        } else {
            this.sizeReductionPercent = 0.0;
        }
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public long getInputSizeBytes() {
        return inputSizeBytes;
    }

    public void setInputSizeBytes(long inputSizeBytes) {
        this.inputSizeBytes = inputSizeBytes;
    }

    public long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public void setOutputSizeBytes(long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }

    public double getSizeReductionPercent() {
        return sizeReductionPercent;
    }

    public void setSizeReductionPercent(double sizeReductionPercent) {
        this.sizeReductionPercent = sizeReductionPercent;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(int expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }
}
