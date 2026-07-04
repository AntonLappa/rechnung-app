package com.antonlappa.rechnungapp.controller.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned when reading an invoice line item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponseDto {

    private UUID id;
    private Integer position;
    private String name;
    private String description;
    private BigDecimal quantity;
    private BigDecimal multiplier;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal vatPercentage;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal totalGross;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
