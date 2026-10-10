package com.convertly.controller;

import com.convertly.service.BatchService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * GET /api/batch/download?jobIds=id1,id2,id3
     * Returns a ZIP archive containing all specified converted files.
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadBatch(@RequestParam("jobIds") String jobIds) {
        if (jobIds == null || jobIds.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<String> ids = Arrays.stream(jobIds.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        try {
            byte[] zipBytes = batchService.buildZip(ids);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"convertly-batch.zip\"")
                .body(zipBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
