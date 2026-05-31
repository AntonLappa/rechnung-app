package com.antonlappa.rechnungapp.controller.dto.company_profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a company profile.
 * <p>
 * Only {@code companyName}, {@code ownerName}, and {@code address}
 * are mandatory — the remaining fields are optional and depend
 * on the seller's business type and preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfileRequestDto {

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @NotBlank(message = "Owner name is required")
    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @NotBlank(message = "Address is required")
    private String address;

    @Size(max = 50, message = "Tax number must not exceed 50 characters")
    private String taxNumber;

    @Size(max = 50, message = "VAT ID must not exceed 50 characters")
    private String vatId;

    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @Size(max = 100, message = "Registration court must not exceed 100 characters")
    private String registrationCourt;

    @Size(max = 34, message = "IBAN must not exceed 34 characters")
    private String iban;

    @Size(max = 11, message = "BIC must not exceed 11 characters")
    private String bic;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 500, message = "Logo path must not exceed 500 characters")
    private String logoPath;

    @NotNull(message = "smallBusiness flag is required")
    private Boolean smallBusiness;
}
