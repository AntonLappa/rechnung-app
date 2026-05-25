package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.controller.dto.MeResponse;
import com.antonlappa.rechnungapp.controller.dto.auth.AuthResponseDto;
import com.antonlappa.rechnungapp.controller.dto.auth.LoginRequestDto;
import com.antonlappa.rechnungapp.controller.dto.auth.RegisterRequestDto;
import com.antonlappa.rechnungapp.service.AuthService;
import com.antonlappa.rechnungapp.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 * <p>
 * {@code /api/v1/auth/login} and {@code /api/v1/auth/register} are publicly accessible.
 * {@code /api/v1/auth/me} requires a valid JWT.
 *
 * @see com.antonlappa.rechnungapp.config.SecurityConfig
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticatedUserResolver userResolver;

    /**
     * POST /api/v1/auth/register
     * <p>
     * Creates a new user account and returns a JWT.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login
     * <p>
     * Authenticates an existing user and returns a JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * GET /api/v1/auth/me
     * <p>
     * Returns information about the currently authenticated user,
     * including their onboarding status (company profile completion).
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = userResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(authService.getMe(userId));
    }
}
