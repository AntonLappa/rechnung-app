package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    List<InvoiceResponseDto> getAllInvoices(UUID userId);

    InvoiceResponseDto getInvoice(UUID userId, UUID invoiceId);

    InvoiceResponseDto createInvoice(UUID userId, InvoiceRequestDto request);

    InvoiceResponseDto updateInvoice(UUID userId, UUID invoiceId, InvoiceRequestDto request);

    InvoiceResponseDto finalizeInvoice(UUID userId, UUID invoiceId);

    void deleteInvoice(UUID userId, UUID invoiceId);

    InvoiceResponseDto cancelInvoice(UUID userId, UUID invoiceId);
}
