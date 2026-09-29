package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Dtos.AuthResponse> register(@Valid @RequestBody Dtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Dtos.AuthResponse> login(@Valid @RequestBody Dtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Tokens are stateless, so logout is a client-side token drop. The endpoint
     * exists so the client has one clear call to make and so a token blocklist
     * can be introduced here later without an API change.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<Dtos.UserResponse> me(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(authService.profile(userId(principal)));
    }

    @PutMapping("/profile")
    public ResponseEntity<Dtos.UserResponse> updateProfile(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody Dtos.UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId(principal), request));
    }

    static Long userId(Object principal) {
        if (principal instanceof com.socialdashboard.security.UserPrincipal p) {
            return p.getId();
        }
        throw new org.springframework.security.authentication.BadCredentialsException("Not authenticated");
    }
}
