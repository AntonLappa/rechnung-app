package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.service.CompanyProfileService;

import com.antonlappa.rechnungapp.controller.dto.CompanyProfileRequest;
import com.antonlappa.rechnungapp.controller.dto.CompanyProfileResponse;
import com.antonlappa.rechnungapp.repository.entity.User;
import com.antonlappa.rechnungapp.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing the authenticated user's company profile.
 * <p>
 * All endpoints require a valid JWT. The user's identity is extracted
 * from the security context via {@link AuthenticationPrincipal}.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code GET  /api/v1/company-profile}  – read the profile</li>
 *   <li>{@code POST /api/v1/company-profile}  – create a new profile</li>
 *   <li>{@code PUT  /api/v1/company-profile}  – update the profile</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/company-profile")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;
    private final UserRepository userRepository;

    /**
     * GET /api/v1/company-profile
     * <p>
     * Returns the authenticated user's company profile.
     */
    @GetMapping
    public ResponseEntity<CompanyProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(companyProfileService.getProfile(userId));
    }

    /**
     * POST /api/v1/company-profile
     * <p>
     * Creates a new company profile for the authenticated user.
     * Returns 409 Conflict if one already exists.
     */
    @PostMapping
    public ResponseEntity<CompanyProfileResponse> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileRequest request) {
        UUID userId = resolveUserId(userDetails);
        CompanyProfileResponse response = companyProfileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/company-profile
     * <p>
     * Updates the authenticated user's existing company profile.
     * Returns 404 if no profile exists yet.
     */
    @PutMapping
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileRequest request) {
        UUID userId = resolveUserId(userDetails);
        return ResponseEntity.ok(companyProfileService.updateProfile(userId, request));
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Resolves the authenticated user's UUID from the security principal.
     * The principal's username is the user's email (set by CustomUserDetailsService).
     */
    private UUID resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
        return user.getId();
    }
}
