package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<Dtos.OverviewResponse> overview(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(
                statsService.overview(AuthController.userId(principal), range, platform));
    }

    @GetMapping("/timeseries")
    public ResponseEntity<Dtos.TimeSeriesResponse> timeSeries(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String metrics) {
        return ResponseEntity.ok(
                statsService.timeSeries(AuthController.userId(principal), range, platform, metrics));
    }

    @GetMapping("/reports")
    public ResponseEntity<Dtos.ReportResponse> report(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) String platform) {
        return ResponseEntity.ok(
                statsService.report(AuthController.userId(principal), range, platform));
    }
}
