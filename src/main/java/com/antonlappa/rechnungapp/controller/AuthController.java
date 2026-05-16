package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.service.AuthService;

import com.antonlappa.rechnungapp.controller.dto.AuthResponse;
import com.antonlappa.rechnungapp.controller.dto.LoginRequest;
import com.antonlappa.rechnungapp.controller.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 * <p>
 * All paths under {@code /api/v1/auth/**} are publicly accessible
 * (see {@link com.antonlappa.rechnungapp.config.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * <p>
     * Creates a new user account and returns a JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login
     * <p>
     * Authenticates an existing user and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
