package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.service.ImageConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files/images")
public class ImageController {

    private final ImageConversionService conversionService;

    public ImageController(ImageConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * POST /api/files/images/convert
     * Converts image between JPG, PNG, WebP, and BMP formats.
     */
    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> convertImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetFormat", defaultValue = "webp") String targetFormat,
            @RequestParam(value = "quality", defaultValue = "0.85") float quality
    ) throws IOException {
        ImageConversionResponseDto result = conversionService.processImageFormatConversion(file, targetFormat, quality);
        return ResponseEntity.ok(ApiResponse.ok(result, "Image converted successfully"));
    }

    /**
     * POST /api/files/images/resize
     * Resizes image with optional aspect ratio preservation, format conversion, and quality adjustment.
     */
    @PostMapping("/resize")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> resizeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "width", defaultValue = "0") int width,
            @RequestParam(value = "height", defaultValue = "0") int height,
            @RequestParam(value = "keepAspectRatio", defaultValue = "true") boolean keepAspectRatio,
            @RequestParam(value = "targetFormat", required = false) String targetFormat,
            @RequestParam(value = "quality", defaultValue = "0.85") float quality
    ) throws IOException {
        ImageConversionResponseDto result = conversionService.processImageResize(
                file, width, height, keepAspectRatio, targetFormat, quality
        );
        return ResponseEntity.ok(ApiResponse.ok(result, "Image resized and processed successfully"));
    }
}
