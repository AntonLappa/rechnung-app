package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;

import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;

import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileRequestDto;
import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileResponseDto;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import com.antonlappa.rechnungapp.mapper.CompanyProfileMapper;
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
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private final CompanyProfileRepository companyProfileRepository;
    private final UserRepository userRepository;
    private final CompanyProfileMapper companyProfileMapper;

    /**
     * Returns the authenticated user's company profile.
     *
     * @param userId the authenticated user's UUID
     * @throws ResourceNotFoundException if no profile exists yet
     */
    @Transactional(readOnly = true)
    public CompanyProfileResponseDto getProfile(UUID userId) {
        CompanyProfileEntity profile = findProfileForUser(userId);
        return companyProfileMapper.toDto(profile);
    }

    /**
     * Creates a new company profile for the authenticated user.
     *
     * @param userId  the authenticated user's UUID
     * @param request the profile data
     * @throws BusinessRuleException if the user already has a profile
     */
    @Transactional
    public CompanyProfileResponseDto createProfile(UUID userId, CompanyProfileRequestDto request) {
        if (companyProfileRepository.existsByUserId(userId)) {
            throw new BusinessRuleException("Company profile already exists. Use PUT to update.");
        }

        UserEntity user = userRepository.getReferenceById(userId);

        CompanyProfileEntity profile = CompanyProfileEntity.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .ownerName(request.getOwnerName())
                .address(request.getAddress())
                .taxNumber(request.getTaxNumber())
                .vatId(request.getVatId())
                .registrationNumber(request.getRegistrationNumber())
                .registrationCourt(request.getRegistrationCourt())
                .iban(request.getIban())
                .bic(request.getBic())
                .email(request.getEmail())
                .phone(request.getPhone())
                .logoPath(request.getLogoPath())
                .smallBusiness(request.getSmallBusiness())
                .build();

        companyProfileRepository.save(profile);
        return companyProfileMapper.toDto(profile);
    }

    /**
     * Updates the authenticated user's existing company profile.
     *
     * @param userId  the authenticated user's UUID
     * @param request the updated profile data
     * @throws ResourceNotFoundException if no profile exists yet
     */
    @Transactional
    public CompanyProfileResponseDto updateProfile(UUID userId, CompanyProfileRequestDto request) {
        CompanyProfileEntity profile = findProfileForUser(userId);

        profile.setCompanyName(request.getCompanyName());
        profile.setOwnerName(request.getOwnerName());
        profile.setAddress(request.getAddress());
        profile.setTaxNumber(request.getTaxNumber());
        profile.setVatId(request.getVatId());
        profile.setRegistrationNumber(request.getRegistrationNumber());
        profile.setRegistrationCourt(request.getRegistrationCourt());
        profile.setIban(request.getIban());
        profile.setBic(request.getBic());
        profile.setEmail(request.getEmail());
        profile.setPhone(request.getPhone());
        profile.setLogoPath(request.getLogoPath());
        profile.setSmallBusiness(request.getSmallBusiness());

        companyProfileRepository.save(profile);
        return companyProfileMapper.toDto(profile);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private CompanyProfileEntity findProfileForUser(UUID userId) {
        return companyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company profile not found for user: " + userId));
    }
}
