package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.service.DocumentConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files/documents")
public class DocumentController {

    private final DocumentConversionService documentService;

    public DocumentController(DocumentConversionService documentService) {
        this.documentService = documentService;
    }

    /**
     * POST /api/files/documents/docx-to-pdf
     * Converts DOCX documents to standard PDF.
     */
    @PostMapping("/docx-to-pdf")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> convertDocxToPdf(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        ImageConversionResponseDto result = documentService.convertDocxToPdf(file);
        return ResponseEntity.ok(ApiResponse.ok(result, "DOCX converted to PDF successfully"));
    }

    /**
     * POST /api/files/documents/pptx-to-pdf
     * Converts PPTX presentations to standard PDF.
     */
    @PostMapping("/pptx-to-pdf")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> convertPptxToPdf(
            @RequestParam("file") MultipartFile file
    ) throws IOException, InterruptedException {
        ImageConversionResponseDto result = documentService.convertPptxToPdf(file);
        return ResponseEntity.ok(ApiResponse.ok(result, "PPTX converted to PDF successfully"));
    }
}
