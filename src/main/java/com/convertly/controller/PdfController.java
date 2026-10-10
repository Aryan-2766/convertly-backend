package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.ImageConversionResponseDto;
import com.convertly.service.PdfConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files/pdfs")
public class PdfController {

    private final PdfConversionService pdfService;

    public PdfController(PdfConversionService pdfService) {
        this.pdfService = pdfService;
    }

    /**
     * POST /api/files/pdfs/merge
     * Merges multiple PDF files into one.
     */
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> mergePdfs(
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        ImageConversionResponseDto result = pdfService.mergePdfs(files);
        return ResponseEntity.ok(ApiResponse.ok(result, "PDFs merged successfully"));
    }

    /**
     * POST /api/files/pdfs/split
     * Extracts selected page ranges from a PDF.
     */
    @PostMapping("/split")
    public ResponseEntity<ApiResponse<ImageConversionResponseDto>> splitPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "pageRange", defaultValue = "all") String pageRange
    ) throws IOException {
        ImageConversionResponseDto result = pdfService.splitPdf(file, pageRange);
        return ResponseEntity.ok(ApiResponse.ok(result, "PDF split successfully"));
    }
}
