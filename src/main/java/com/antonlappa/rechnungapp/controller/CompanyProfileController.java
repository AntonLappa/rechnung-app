package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.service.CompanyProfileService;

import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileRequestDto;
import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileResponseDto;
import com.antonlappa.rechnungapp.service.AuthenticatedUserResolver;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * GET /api/v1/company-profile
     * <p>
     * Returns the authenticated user's company profile.
     */
    @GetMapping
    public ResponseEntity<CompanyProfileResponseDto> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(companyProfileService.getProfile(userId));
    }

    /**
     * POST /api/v1/company-profile
     * <p>
     * Creates a new company profile for the authenticated user.
     * Returns 409 Conflict if one already exists.
     */
    @PostMapping
    public ResponseEntity<CompanyProfileResponseDto> createProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        CompanyProfileResponseDto response = companyProfileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/company-profile
     * <p>
     * Updates the authenticated user's existing company profile.
     * Returns 404 if no profile exists yet.
     */
    @PutMapping
    public ResponseEntity<CompanyProfileResponseDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(companyProfileService.updateProfile(userId, request));
    }

    /**
     * POST /api/v1/company-profile/logo
     * <p>
     * Uploads a company logo (JPEG, PNG, GIF or WebP, max 2 MB).
     * Stores the file in the configured S3 bucket and saves the object key
     * in the company profile. Returns the updated profile.
     */
    @PostMapping(value = "/logo", consumes = "multipart/form-data")
    public ResponseEntity<CompanyProfileResponseDto> uploadLogo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(companyProfileService.uploadLogo(userId, file));
    }
}
