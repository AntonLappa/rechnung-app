package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.repository.entity.CompanyProfile;

import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;

import com.antonlappa.rechnungapp.controller.dto.CompanyProfileRequest;
import com.antonlappa.rechnungapp.controller.dto.CompanyProfileResponse;
import com.antonlappa.rechnungapp.repository.entity.User;
import com.antonlappa.rechnungapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for company profile management.
 * <p>
 * A user can have at most one company profile. The service enforces
 * this invariant and ensures that every read/write is scoped to the
 * authenticated user.
 */
@Service
@RequiredArgsConstructor
public class CompanyProfileService {

    private final CompanyProfileRepository companyProfileRepository;
    private final UserRepository userRepository;

    /**
     * Returns the authenticated user's company profile.
     *
     * @param userId the authenticated user's UUID
     * @throws EntityNotFoundException if no profile exists yet
     */
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(UUID userId) {
        CompanyProfile profile = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Company profile not found"));
        return toResponse(profile);
    }

    /**
     * Creates a new company profile for the authenticated user.
     *
     * @param userId  the authenticated user's UUID
     * @param request the profile data
     * @throws IllegalStateException if the user already has a profile
     */
    @Transactional
    public CompanyProfileResponse createProfile(UUID userId, CompanyProfileRequest request) {
        if (companyProfileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("Company profile already exists. Use PUT to update.");
        }

        User user = userRepository.getReferenceById(userId);

        CompanyProfile profile = CompanyProfile.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .ownerName(request.getOwnerName())
                .address(request.getAddress())
                .taxNumber(request.getTaxNumber())
                .vatId(request.getVatId())
                .iban(request.getIban())
                .bic(request.getBic())
                .email(request.getEmail())
                .phone(request.getPhone())
                .logoPath(request.getLogoPath())
                .smallBusiness(request.getSmallBusiness())
                .build();

        companyProfileRepository.save(profile);
        return toResponse(profile);
    }

    /**
     * Updates the authenticated user's existing company profile.
     *
     * @param userId  the authenticated user's UUID
     * @param request the updated profile data
     * @throws EntityNotFoundException if no profile exists yet
     */
    @Transactional
    public CompanyProfileResponse updateProfile(UUID userId, CompanyProfileRequest request) {
        CompanyProfile profile = companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Company profile not found. Create one first with POST."));

        profile.setCompanyName(request.getCompanyName());
        profile.setOwnerName(request.getOwnerName());
        profile.setAddress(request.getAddress());
        profile.setTaxNumber(request.getTaxNumber());
        profile.setVatId(request.getVatId());
        profile.setIban(request.getIban());
        profile.setBic(request.getBic());
        profile.setEmail(request.getEmail());
        profile.setPhone(request.getPhone());
        profile.setLogoPath(request.getLogoPath());
        profile.setSmallBusiness(request.getSmallBusiness());

        companyProfileRepository.save(profile);
        return toResponse(profile);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private CompanyProfileResponse toResponse(CompanyProfile profile) {
        return CompanyProfileResponse.builder()
                .id(profile.getId())
                .companyName(profile.getCompanyName())
                .ownerName(profile.getOwnerName())
                .address(profile.getAddress())
                .taxNumber(profile.getTaxNumber())
                .vatId(profile.getVatId())
                .iban(profile.getIban())
                .bic(profile.getBic())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .logoPath(profile.getLogoPath())
                .smallBusiness(profile.isSmallBusiness())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
