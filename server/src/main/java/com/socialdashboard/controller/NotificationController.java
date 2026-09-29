package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<Dtos.NotificationResponse>> list(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean unread) {
        return ResponseEntity.ok(
                notificationService.list(AuthController.userId(principal), page, size, type, unread));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Dtos.NotificationResponse>> unread(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(notificationService.unread(AuthController.userId(principal)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(AuthController.userId(principal))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Dtos.NotificationResponse> markRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(AuthController.userId(principal), id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Long>> markAllRead(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(AuthController.userId(principal))));
    }
}
