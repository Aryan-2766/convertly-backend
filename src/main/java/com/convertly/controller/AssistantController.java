package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.dto.AssistantResponseDto;
import com.convertly.service.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * POST /api/assistant/suggest
     * Returns format and settings recommendation.
     * Zero external API cost by default using local rule engine.
     */
    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<AssistantResponseDto>> getSuggestion(
            @RequestBody(required = false) Map<String, String> body
    ) {
        String query = body != null ? body.getOrDefault("query", "") : "";
        AssistantResponseDto response = assistantService.getRecommendation(query);
        return ResponseEntity.ok(ApiResponse.ok(response, "Recommendation generated"));
    }
}
