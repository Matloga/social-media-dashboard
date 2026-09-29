package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.StatsService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportExportController {

    private final StatsService statsService;

    public ReportExportController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/export.csv")
    public ResponseEntity<Resource> exportCsv(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(required = false) String platform) {
        Dtos.ReportResponse report = statsService.report(AuthController.userId(principal), range, platform);
        String csv = toCsv(report);
        String filename = "social-report-" + report.range() + "-"
                + (platform == null ? "all" : platform.toLowerCase()) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.getBytes(StandardCharsets.UTF_8).length)
                .body(new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private String toCsv(Dtos.ReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Social Media Analytics Report\n");
        sb.append("Period,").append(report.rangeLabel()).append('\n');
        sb.append("From,").append(report.from()).append('\n');
        sb.append("To,").append(report.to()).append('\n');
        sb.append("Generated,").append(report.generatedAt()).append('\n');
        sb.append('\n');

        sb.append("Summary\n");
        sb.append("Metric,Value\n");
        sb.append("Total Reach,").append(report.totalReach()).append('\n');
        sb.append("Total Impressions,").append(report.totalImpressions()).append('\n');
        sb.append("Total Followers,").append(report.totalFollowers()).append('\n');
        sb.append("Follower Growth,").append(report.followerGrowth()).append('\n');
        sb.append("Total Likes,").append(report.totalLikes()).append('\n');
        sb.append("Total Comments,").append(report.totalComments()).append('\n');
        sb.append("Total Shares,").append(report.totalShares()).append('\n');
        sb.append("Engagement Rate %,").append(report.engagementRate()).append('\n');
        sb.append("Posts Published,").append(report.postsPublished()).append('\n');
        sb.append('\n');

        if (!report.byPlatform().isEmpty()) {
            sb.append("By Platform\n");
            sb.append("Platform,Username,Followers,Likes,Comments,Engagement Rate %,Share Of Followers %\n");
            for (Dtos.PlatformBreakdown p : report.byPlatform()) {
                sb.append(p.platform()).append(',')
                        .append(escape(p.username())).append(',')
                        .append(p.followers()).append(',')
                        .append(p.likes()).append(',')
                        .append(p.comments()).append(',')
                        .append(p.engagementRate()).append(',')
                        .append(p.shareOfFollowers()).append('\n');
            }
            sb.append('\n');
        }

        sb.append("Daily Trend\n");
        sb.append("Date,Followers,Likes,Comments,Shares,Impressions,Reach,Engagement Rate %\n");
        for (Dtos.MetricPoint p : report.trend()) {
            sb.append(p.date()).append(',')
                    .append(p.followers()).append(',')
                    .append(p.likes()).append(',')
                    .append(p.comments()).append(',')
                    .append(p.shares()).append(',')
                    .append(p.impressions()).append(',')
                    .append(p.reach()).append(',')
                    .append(p.engagementRate()).append('\n');
        }
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
