package com.antonlappa.rechnungapp.controller.dto.invoice;

import com.antonlappa.rechnungapp.repository.entity.PaymentMethod;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating or updating an invoice.
 * <p>
 * Includes the nested list of line items. The invoice is always
 * created as DRAFT — status and invoice number are managed by the
 * service layer, not the client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequestDto {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate serviceDate;

    @NotNull(message = "VAT mode is required")
    private VatMode vatMode;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @NotEmpty(message = "At least one invoice item is required")
    @Valid
    private List<InvoiceItemRequestDto> items;
}
