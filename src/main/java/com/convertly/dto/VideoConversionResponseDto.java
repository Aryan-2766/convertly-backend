package com.convertly.dto;

public class VideoConversionResponseDto {
    private String jobId;
    private String outputFileName;
    private long outputSizeBytes;
    private long inputSizeBytes;
    private String inputFormat;
    private String outputFormat;
    private String resolution;

    public VideoConversionResponseDto() {}

    public VideoConversionResponseDto(String jobId, String outputFileName, long outputSizeBytes,
                                       long inputSizeBytes, String inputFormat, String outputFormat,
                                       String resolution) {
        this.jobId = jobId;
        this.outputFileName = outputFileName;
        this.outputSizeBytes = outputSizeBytes;
        this.inputSizeBytes = inputSizeBytes;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.resolution = resolution;
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
    public long getOutputSizeBytes() { return outputSizeBytes; }
    public void setOutputSizeBytes(long outputSizeBytes) { this.outputSizeBytes = outputSizeBytes; }
    public long getInputSizeBytes() { return inputSizeBytes; }
    public void setInputSizeBytes(long inputSizeBytes) { this.inputSizeBytes = inputSizeBytes; }
    public String getInputFormat() { return inputFormat; }
    public void setInputFormat(String inputFormat) { this.inputFormat = inputFormat; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
