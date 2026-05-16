package com.antonlappa.rechnungapp.controller.dto;

import com.antonlappa.rechnungapp.repository.entity.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating or updating a customer.
 * <p>
 * {@code name}, {@code address}, and {@code type} are mandatory.
 * Tax/VAT fields are optional and depend on the customer type.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Tax number must not exceed 50 characters")
    private String taxNumber;

    @Size(max = 50, message = "VAT ID must not exceed 50 characters")
    private String vatId;

    @NotNull(message = "Customer type is required (PRIVATE or BUSINESS)")
    private CustomerType type;
}
