package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for invoice operations.
 */
public interface InvoiceService {

    /**
     * Returns all invoices for the authenticated user, newest first.
     * Optionally filters by status and/or customer.
     */
    List<InvoiceResponseDto> getAllInvoices(UUID userId, InvoiceStatus status, UUID customerId);

    /**
     * Returns a single invoice by ID, scoped to the authenticated user.
     */
    InvoiceResponseDto getInvoice(UUID userId, UUID invoiceId);

    /**
     * Creates a new DRAFT invoice with calculated totals.
     */
    InvoiceResponseDto createInvoice(UUID userId, InvoiceRequestDto request);

    /**
     * Updates an existing DRAFT invoice. FINAL and CANCELLED invoices
     * cannot be edited.
     */
    InvoiceResponseDto updateInvoice(UUID userId, UUID invoiceId, InvoiceRequestDto request);

    /**
     * Finalizes a DRAFT invoice: assigns a sequential invoice number
     * and locks it from further edits.
     */
    InvoiceResponseDto finalizeInvoice(UUID userId, UUID invoiceId);

    /**
     * Deletes a DRAFT invoice. FINAL and CANCELLED invoices cannot
     * be deleted (they must be kept for audit/tax purposes).
     */
    void deleteInvoice(UUID userId, UUID invoiceId);

    /**
     * Cancels a FINAL invoice. DRAFT and already CANCELLED invoices
     * cannot be cancelled.
     */
    InvoiceResponseDto cancelInvoice(UUID userId, UUID invoiceId);
}
