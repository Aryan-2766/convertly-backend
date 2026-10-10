package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.HealthStatusDto;
import com.convertly.dto.ToolMetadataDto;
import com.convertly.processor.AudioVideoProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired
    private AudioVideoProcessor audioVideoProcessor;

    @Value("${convertly.storage.ephemeral:true}")
    private boolean ephemeralStorage;

    @Value("${convertly.storage.retention-minutes:15}")
    private int retentionMinutes;


    /**
     * GET /api/health
     * Honest health check verifying Spring Boot status and Aiven Cloud MySQL connection.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthStatusDto>> getHealth() {
        String dbStatus = "NOT_CONFIGURED";
        String dbDetails = "No datasource initialized";

        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                dbStatus = "CONNECTED";
                dbDetails = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion()
                        + " (URL: " + metaData.getURL().replaceAll("password=[^&]*", "password=***") + ")";
            } catch (Exception e) {
                dbStatus = "DISCONNECTED";
                dbDetails = "Failed to connect to MySQL: " + e.getMessage();
            }
        }

        String storageInfo = ephemeralStorage
                ? "Ephemeral scratch space (transient, auto-purged every " + retentionMinutes + " min, zero permanent storage)"
                : "Standard filesystem storage";

        String overallStatus = "CONNECTED".equals(dbStatus) ? "UP" : "DEGRADED";

        HealthStatusDto healthDto = new HealthStatusDto(
                overallStatus,
                "Convertly Backend Service v1.0",
                dbStatus,
                dbDetails,
                storageInfo
        );

        return ResponseEntity.ok(ApiResponse.ok(healthDto, "Health check completed successfully"));
    }

    /**
     * GET /api/tools
     * Returns the registry of available file operations and format support.
     */
    @GetMapping("/tools")
    public ResponseEntity<ApiResponse<List<ToolMetadataDto>>> getTools() {
        List<ToolMetadataDto> tools = new ArrayList<>();

        // Image Conversion
        tools.add(new ToolMetadataDto(
                "image-convert",
                "Image Format Converter",
                "Convert images between JPG, PNG, WebP, and BMP with high fidelity.",
                "IMAGES",
                "MVP",
                Arrays.asList("jpg", "jpeg", "png", "webp", "bmp"),
                Arrays.asList("jpg", "jpeg", "png", "webp", "bmp"),
                true
        ));

        // Image Resizing
        tools.add(new ToolMetadataDto(
                "image-resize",
                "Image Resizer & Compressor",
                "Resize dimensions and adjust quality settings for optimal web performance.",
                "IMAGES",
                "MVP",
                Arrays.asList("jpg", "jpeg", "png", "webp"),
                Arrays.asList("jpg", "jpeg", "png", "webp"),
                true
        ));

        // PDF Merge
        tools.add(new ToolMetadataDto(
                "pdf-merge",
                "PDF Merger",
                "Combine multiple PDF documents into a single organized PDF file.",
                "PDFS",
                "MVP",
                Arrays.asList("pdf"),
                Arrays.asList("pdf"),
                true
        ));

        // PDF Split
        tools.add(new ToolMetadataDto(
                "pdf-split",
                "PDF Splitter",
                "Extract specific page ranges or split a PDF into separate files.",
                "PDFS",
                "MVP",
                Arrays.asList("pdf"),
                Arrays.asList("pdf"),
                true
        ));

        // Document: DOCX to PDF
        tools.add(new ToolMetadataDto(
                "docx-to-pdf",
                "DOCX to PDF",
                "Convert Microsoft Word documents (.docx) into standard PDF format.",
                "DOCUMENTS",
                "MVP",
                Arrays.asList("docx"),
                Arrays.asList("pdf"),
                true
        ));

        // Phase 2: Audio Converter
        boolean ffmpegAvailable = audioVideoProcessor.isFfmpegAvailable();
        tools.add(new ToolMetadataDto(
                "audio-convert",
                "Audio Converter",
                "Convert audio files between MP3, WAV, AAC, OGG and FLAC using FFmpeg.",
                "AUDIO",
                "PHASE_2",
                Arrays.asList("mp3", "wav", "aac", "ogg", "flac", "m4a"),
                Arrays.asList("mp3", "wav", "aac", "ogg", "flac"),
                ffmpegAvailable
        ));

        // Phase 2: Video Converter & Extract Audio
        tools.add(new ToolMetadataDto(
                "video-convert",
                "Video Converter & Audio Extractor",
                "Convert video formats (MP4, MKV, AVI, MOV, WebM) or extract audio track using FFmpeg.",
                "VIDEO",
                "PHASE_2",
                Arrays.asList("mp4", "mkv", "avi", "mov", "webm"),
                Arrays.asList("mp4", "mkv", "avi", "mov", "webm", "mp3"),
                ffmpegAvailable
        ));

        return ResponseEntity.ok(ApiResponse.ok(tools, "Tool definitions loaded successfully"));
    }
}

