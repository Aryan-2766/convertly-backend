package com.convertly.service;

import com.convertly.dto.AssistantResponseDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Format Recommendation and Advisory Service.
 * Implements local zero-cost rules first, in accordance with the Master Prompt cost-control requirement.
 */
@Service
public class AssistantService {

    public AssistantResponseDto getRecommendation(String query) {
        String q = (query != null ? query.trim().toLowerCase(Locale.ROOT) : "");

        if (q.contains("web") || q.contains("website") || q.contains("page speed") || q.contains("faster")) {
            return new AssistantResponseDto(
                    query,
                    "WEBP",
                    "WebP Format Recommendation",
                    "WebP is modern and optimized for the web. It delivers 25-34% smaller file sizes than comparable JPEG images while maintaining crisp visual quality and supporting transparent backgrounds.",
                    Arrays.asList("Use 80-85% compression quality for the optimal balance of quality and size.", "Enable 'Keep Aspect Ratio' when resizing for responsive layouts."),
                    "LOCAL_RULE_ENGINE"
            );
        }

        if (q.contains("transparent") || q.contains("logo") || q.contains("icon") || q.contains("graphic") || q.contains("badge")) {
            return new AssistantResponseDto(
                    query,
                    "PNG",
                    "PNG Lossless Recommendation",
                    "PNG uses lossless compression (DEFLATE). It is ideal for logos, icons, diagrams, and images with transparent alpha channels where photographic compression would cause blurriness.",
                    Arrays.asList("PNG preserves sharp lines and crisp typography.", "For photos without transparency, consider WebP or JPG instead to reduce size."),
                    "LOCAL_RULE_ENGINE"
            );
        }

        if (q.contains("photo") || q.contains("camera") || q.contains("picture") || q.contains("print") || q.contains("jpg") || q.contains("jpeg")) {
            return new AssistantResponseDto(
                    query,
                    "JPG",
                    "JPEG Photographic Recommendation",
                    "JPEG is the universal standard for continuous-tone photography. It offers strong lossy compression with universal compatibility across all devices and legacy browsers.",
                    Arrays.asList("Set quality to 85% for standard photos.", "Note: JPEG does not support transparency; transparent areas will be filled with clean white."),
                    "LOCAL_RULE_ENGINE"
            );
        }

        if (q.contains("pdf") || q.contains("document") || q.contains("merge") || q.contains("split") || q.contains("contract")) {
            return new AssistantResponseDto(
                    query,
                    "PDF",
                    "PDF Document Guidance",
                    "PDF preserves fonts, page layout, and vector elements across all operating systems. Use Convertly's PDF Merger to combine invoices or reports, and the Splitter to extract specific page ranges.",
                    Arrays.asList("Use page ranges like '1-3, 5' in the Splitter.", "Ensure PDFs are unencrypted before processing."),
                    "LOCAL_RULE_ENGINE"
            );
        }

        if (q.contains("word") || q.contains("docx") || q.contains("office")) {
            return new AssistantResponseDto(
                    query,
                    "PDF",
                    "DOCX to PDF Conversion",
                    "Converting DOCX to PDF locks in pagination and font rendering so reviewers on mobile and desktop view identical formatting.",
                    Arrays.asList("Check that standard fonts are embedded in your document.", "Convertly uses local headless conversion with zero external cloud leaks."),
                    "LOCAL_RULE_ENGINE"
            );
        }

        // General default advisory
        return new AssistantResponseDto(
                query,
                "WEBP",
                "General File Optimization",
                "For images on the modern web, convert to WebP at 85% quality. For documents, PDF is the safest universal format. All conversions on Convertly run locally with zero cloud API charges.",
                Arrays.asList("Pick 'Image Converter' for JPG/PNG/WebP transformations.", "Use 'PDF Merger' to organize multi-page submissions."),
                "LOCAL_RULE_ENGINE"
        );
    }
}
