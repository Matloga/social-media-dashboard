package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.InsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping
    public ResponseEntity<List<Dtos.Insight>> list(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(
                insightsService.insights(AuthController.userId(principal), range, platform));
    }
}
