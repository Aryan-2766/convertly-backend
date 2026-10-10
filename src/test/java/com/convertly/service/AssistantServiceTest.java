package com.convertly.service;

import com.convertly.dto.AssistantResponseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AssistantServiceTest {

    private final AssistantService assistantService = new AssistantService();

    @Test
    void testDesktopResolutionAdvice() {
        AssistantResponseDto response = assistantService.getRecommendation("what would be the best photo size for desktop");
        assertNotNull(response);
        assertTrue(response.getRecommendedFormat().contains("1920") || response.getRecommendedFormat().contains("FHD"), "Should recommend 1080p/FHD desktop dimensions");
        assertFalse(response.getTips().isEmpty());
    }

    @Test
    void testLegalDocumentAdvice() {
        AssistantResponseDto response = assistantService.getRecommendation("which doc format is best for legal use");
        assertNotNull(response);
        assertTrue(response.getRecommendedFormat().contains("PDF"), "Should recommend PDF/A for legal compliance");
        assertNotNull(response.getExplanation());
    }

    @Test
    void testPrintMediaAdvice() {
        AssistantResponseDto response = assistantService.getRecommendation("what resolution for poster print 300 dpi");
        assertNotNull(response);
        assertTrue(response.getRecommendedFormat().contains("300 DPI"));
    }
}
