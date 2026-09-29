package com.socialdashboard.controller;

import com.socialdashboard.dto.Dtos;
import com.socialdashboard.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<Dtos.AccountResponse>> list(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(accountService.list(AuthController.userId(principal)));
    }

    @PostMapping
    public ResponseEntity<Dtos.AccountResponse> connect(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody Dtos.CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.connect(AuthController.userId(principal), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dtos.AccountResponse> update(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id,
            @Valid @RequestBody Dtos.UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.update(AuthController.userId(principal), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        accountService.disconnect(AuthController.userId(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Dtos.AccountResponse> sync(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(accountService.sync(AuthController.userId(principal), id));
    }

    @PostMapping("/sync")
    public ResponseEntity<Dtos.AccountResponse> syncAll(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(accountService.sync(AuthController.userId(principal), null));
    }
}
