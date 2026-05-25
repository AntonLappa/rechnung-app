package com.antonlappa.rechnungapp.controller.dto.company_profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned when reading a company profile.
 * <p>
 * Excludes the internal {@code user} relationship and exposes
 * only the data relevant to the API consumer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfileResponseDto {

    private UUID id;
    private String companyName;
    private String ownerName;
    private String address;
    private String taxNumber;
    private String vatId;
    private String iban;
    private String bic;
    private String email;
    private String phone;
    private String logoPath;
    private boolean smallBusiness;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
