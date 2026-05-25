package com.antonlappa.rechnungapp.service;

import java.util.UUID;

/**
 * Service responsible for generating invoice PDFs.
 */
public interface InvoicePdfService {

    /**
     * Generates a PDF for the given invoice.
     *
     * @param userId    the authenticated user's UUID (for access control)
     * @param invoiceId the invoice to render
     * @return the PDF document containing bytes and filename
     */
    PdfDocument generatePdf(UUID userId, UUID invoiceId);
}
