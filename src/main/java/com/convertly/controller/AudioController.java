package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.AudioConversionResponseDto;
import com.convertly.processor.AudioVideoProcessor;
import com.convertly.service.AudioConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files/audio")
public class AudioController {

    private final AudioConversionService audioService;
    private final AudioVideoProcessor processor;

    public AudioController(AudioConversionService audioService, AudioVideoProcessor processor) {
        this.audioService = audioService;
        this.processor = processor;
    }

    /**
     * POST /api/files/audio/convert
     * Params: file (multipart), outputFormat (mp3|wav|aac|ogg|flac|m4a)
     */
    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<AudioConversionResponseDto>> convertAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam("outputFormat") String outputFormat) {

        if (!processor.isFfmpegAvailable()) {
            return ResponseEntity.status(503)
                .body(ApiResponse.error("FFmpeg is not installed on this server. " +
                    "Please install it via: winget install Gyan.FFmpeg"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No file uploaded"));
        }

        try {
            AudioConversionResponseDto result = audioService.convertAudio(file, outputFormat.toLowerCase());
            return ResponseEntity.ok(ApiResponse.ok(result, "Audio converted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Audio conversion failed: " + e.getMessage()));
        }
    }
}
