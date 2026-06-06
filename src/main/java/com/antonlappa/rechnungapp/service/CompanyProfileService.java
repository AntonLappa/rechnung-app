package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileRequestDto;
import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CompanyProfileService {

    CompanyProfileResponseDto getProfile(UUID userId);

    CompanyProfileResponseDto createProfile(UUID userId, CompanyProfileRequestDto request);

    CompanyProfileResponseDto updateProfile(UUID userId, CompanyProfileRequestDto request);

    CompanyProfileResponseDto uploadLogo(UUID userId, MultipartFile file);
}
