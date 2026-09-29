package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public ResponseEntity<Page<Dtos.PostSummary>> list(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long accountId) {
        return ResponseEntity.ok(postService.search(
                AuthController.userId(principal), page, size, sort, direction, platform, type, accountId));
    }

    @GetMapping("/hashtags")
    public ResponseEntity<List<PostService.HashtagStat>> hashtags(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(
                postService.hashtagAnalytics(AuthController.userId(principal), platform, limit));
    }
}
