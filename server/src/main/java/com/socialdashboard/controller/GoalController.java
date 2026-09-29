package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public ResponseEntity<List<Dtos.GoalResponse>> list(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(goalService.list(AuthController.userId(principal)));
    }

    @PostMapping
    public ResponseEntity<Dtos.GoalResponse> create(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody Dtos.CreateGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.create(AuthController.userId(principal), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dtos.GoalResponse> update(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.UpdateGoalRequest request) {
        return ResponseEntity.ok(goalService.update(AuthController.userId(principal), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Object principal, @PathVariable Long id) {
        goalService.delete(AuthController.userId(principal), id);
        return ResponseEntity.noContent().build();
    }
}
