package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileRequestDto;
import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileResponseDto;

import java.util.UUID;

public interface CompanyProfileService {

    CompanyProfileResponseDto getProfile(UUID userId);

    CompanyProfileResponseDto createProfile(UUID userId, CompanyProfileRequestDto request);

    CompanyProfileResponseDto updateProfile(UUID userId, CompanyProfileRequestDto request);
}
