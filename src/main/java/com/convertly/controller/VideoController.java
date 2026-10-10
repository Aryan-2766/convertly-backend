package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.VideoConversionResponseDto;
import com.convertly.processor.AudioVideoProcessor;
import com.convertly.service.VideoConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files/video")
public class VideoController {

    private final VideoConversionService videoService;
    private final AudioVideoProcessor processor;

    public VideoController(VideoConversionService videoService, AudioVideoProcessor processor) {
        this.videoService = videoService;
        this.processor = processor;
    }

    private ResponseEntity<ApiResponse<VideoConversionResponseDto>> ffmpegMissingResponse() {
        return ResponseEntity.status(503)
            .body(ApiResponse.error("FFmpeg is not installed on this server. " +
                "Please install it via: winget install Gyan.FFmpeg"));
    }

    /**
     * POST /api/files/video/convert
     * Params: file (multipart), outputFormat, resolution (optional: original|720p|480p|360p)
     */
    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<VideoConversionResponseDto>> convertVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("outputFormat") String outputFormat,
            @RequestParam(value = "resolution", defaultValue = "original") String resolution) {

        if (!processor.isFfmpegAvailable()) return ffmpegMissingResponse();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No file uploaded"));
        }

        try {
            VideoConversionResponseDto result = videoService.convertVideo(file, outputFormat.toLowerCase(), resolution);
            return ResponseEntity.ok(ApiResponse.ok(result, "Video converted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Video conversion failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/files/video/extract-audio
     * Param: file (multipart video) → returns MP3 audio
     */
    @PostMapping("/extract-audio")
    public ResponseEntity<ApiResponse<VideoConversionResponseDto>> extractAudio(
            @RequestParam("file") MultipartFile file) {

        if (!processor.isFfmpegAvailable()) return ffmpegMissingResponse();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No file uploaded"));
        }

        try {
            VideoConversionResponseDto result = videoService.extractAudio(file);
            return ResponseEntity.ok(ApiResponse.ok(result, "Audio extracted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Audio extraction failed: " + e.getMessage()));
        }
    }
}
