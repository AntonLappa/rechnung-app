package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.service.AuthenticatedUserResolver;
import com.antonlappa.rechnungapp.service.InvoicePdfService;
import com.antonlappa.rechnungapp.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for invoice management.
 * <p>
 * All endpoints require a valid JWT. The user's identity is extracted
 * from the security context via {@link AuthenticationPrincipal}.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code POST   /api/v1/invoices}               – create a draft invoice</li>
 *   <li>{@code GET    /api/v1/invoices}                – list all invoices</li>
 *   <li>{@code GET    /api/v1/invoices/{id}}           – get a single invoice</li>
 *   <li>{@code PUT    /api/v1/invoices/{id}}           – update a draft invoice</li>
 *   <li>{@code POST   /api/v1/invoices/{id}/finalize}  – finalize a draft invoice</li>
 *   <li>{@code POST   /api/v1/invoices/{id}/cancel}    – cancel a final invoice</li>
 *   <li>{@code DELETE /api/v1/invoices/{id}}           – delete a draft invoice</li>
 *   <li>{@code GET    /api/v1/invoices/{id}/pdf}       – download invoice PDF</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * GET /api/v1/invoices
     * Returns all invoices for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<InvoiceResponseDto>> getAllInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(invoiceService.getAllInvoices(userId));
    }

    /**
     * GET /api/v1/invoices/{id}
     * Returns a single invoice by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(invoiceService.getInvoice(userId, id));
    }

    /**
     * POST /api/v1/invoices
     * Creates a new DRAFT invoice.
     */
    @PostMapping
    public ResponseEntity<InvoiceResponseDto> createInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InvoiceRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        InvoiceResponseDto response = invoiceService.createInvoice(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/invoices/{id}
     * Updates an existing DRAFT invoice.
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody InvoiceRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(invoiceService.updateInvoice(userId, id, request));
    }

    /**
     * POST /api/v1/invoices/{id}/finalize
     * Finalizes a DRAFT invoice — assigns an invoice number and locks it.
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<InvoiceResponseDto> finalizeInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(invoiceService.finalizeInvoice(userId, id));
    }

    /**
     * POST /api/v1/invoices/{id}/cancel
     * Cancels a FINAL invoice.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(invoiceService.cancelInvoice(userId, id));
    }

    /**
     * DELETE /api/v1/invoices/{id}
     * Deletes a DRAFT invoice.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        invoiceService.deleteInvoice(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/invoices/{id}/pdf
     * Downloads a PDF for a FINAL invoice.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        
        var pdfDocument = invoicePdfService.generatePdf(userId, id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", pdfDocument.filename());
        headers.setContentLength(pdfDocument.data().length);

        return new ResponseEntity<>(pdfDocument.data(), headers, HttpStatus.OK);
    }

    // ── Private helpers ──────────────────────────────────────────────

}
