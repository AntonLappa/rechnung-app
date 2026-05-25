package com.antonlappa.rechnungapp.mapper;

import com.antonlappa.rechnungapp.controller.dto.company_profile.CompanyProfileResponseDto;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyProfileMapper {

    public CompanyProfileResponseDto toDto(CompanyProfileEntity profile) {
        return CompanyProfileResponseDto.builder()
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
