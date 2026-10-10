package com.convertly.controller;

import com.convertly.dto.ApiResponse;
import com.convertly.entity.ConversionJob;
import com.convertly.repository.ConversionJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired(required = false)
    private ConversionJobRepository jobRepository;

    /**
     * GET /api/history
     * Returns recent conversion jobs recorded in Aiven Cloud MySQL.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversionJob>>> getHistory() {
        if (jobRepository != null) {
            try {
                List<ConversionJob> history = jobRepository.findTop20ByOrderByCreatedAtDesc();
                return ResponseEntity.ok(ApiResponse.ok(history, "Conversion history loaded successfully"));
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(Collections.emptyList(), "History unavailable"));
    }
}
