package com.antonlappa.rechnungapp.controller.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating or updating an invoice line item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemRequestDto {

    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @Positive(message = "Multiplier must be positive")
    private BigDecimal multiplier;

    @Size(max = 20, message = "Multiplier unit must not exceed 20 characters")
    private String multiplierUnit;

    @Size(max = 30, message = "Unit must not exceed 30 characters")
    private String unit;

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    @NotNull(message = "VAT percentage is required")
    @PositiveOrZero(message = "VAT percentage must be zero or positive")
    private BigDecimal vatPercentage;
}
