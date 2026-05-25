package com.antonlappa.rechnungapp.controller.dto.invoice;

import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO returned when reading an invoice.
 * Includes the customer ID and all computed totals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDto {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate serviceDate;
    private InvoiceStatus status;
    private VatMode vatMode;
    private String currency;
    private BigDecimal totalNet;
    private BigDecimal totalVat;
    private BigDecimal totalGross;
    private List<InvoiceItemResponseDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
