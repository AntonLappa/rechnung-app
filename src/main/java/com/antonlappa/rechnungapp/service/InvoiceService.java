package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.InvoiceItemResponse;
import com.antonlappa.rechnungapp.controller.dto.InvoiceRequest;
import com.antonlappa.rechnungapp.controller.dto.InvoiceResponse;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.CustomerRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.Customer;
import com.antonlappa.rechnungapp.repository.entity.Invoice;
import com.antonlappa.rechnungapp.repository.entity.InvoiceItem;
import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for invoice management.
 * <p>
 * Handles creation, update, finalization, cancellation, and deletion
 * of invoices. Monetary calculations are delegated to
 * {@link InvoiceCalculationService}.
 * <p>
 * Every operation is scoped to the authenticated user's UUID.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final InvoiceCalculationService calculationService;

    // ── Queries ─────────────────────────────────────────────────────

    /**
     * Returns all invoices for the authenticated user, newest first.
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices(UUID userId) {
        return invoiceRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single invoice by ID, scoped to the authenticated user.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID userId, UUID invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);
        return toResponse(invoice);
    }

    // ── Commands ────────────────────────────────────────────────────

    /**
     * Creates a new DRAFT invoice with calculated totals.
     */
    @Transactional
    public InvoiceResponse createInvoice(UUID userId, InvoiceRequest request) {
        // Verify customer belongs to this user
        Customer customer = customerRepository.findByIdAndUserId(request.getCustomerId(), userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        Invoice invoice = Invoice.builder()
                .user(userRepository.getReferenceById(userId))
                .customer(customer)
                .invoiceDate(request.getInvoiceDate())
                .serviceDate(request.getServiceDate())
                .status(InvoiceStatus.DRAFT)
                .vatMode(request.getVatMode())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .build();

        // Build and validate items
        calculationService.buildItems(invoice, request.getItems(), request.getVatMode());

        // Calculate invoice totals from items
        calculationService.recalculateTotals(invoice);

        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    /**
     * Updates an existing DRAFT invoice. FINAL and CANCELLED invoices
     * cannot be edited.
     */
    @Transactional
    public InvoiceResponse updateInvoice(UUID userId, UUID invoiceId, InvoiceRequest request) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be edited. Current status: " + invoice.getStatus());
        }

        // Verify customer belongs to this user
        Customer customer = customerRepository.findByIdAndUserId(request.getCustomerId(), userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        invoice.setCustomer(customer);
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setServiceDate(request.getServiceDate());
        invoice.setVatMode(request.getVatMode());
        invoice.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EUR");

        // Replace items
        invoice.getItems().clear();
        calculationService.buildItems(invoice, request.getItems(), request.getVatMode());

        // Recalculate totals
        calculationService.recalculateTotals(invoice);

        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    /**
     * Finalizes a DRAFT invoice: assigns a sequential invoice number
     * and locks it from further edits.
     */
    @Transactional
    public InvoiceResponse finalizeInvoice(UUID userId, UUID invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be finalized. Current status: " + invoice.getStatus());
        }

        // Generate invoice number: YYYY-NNNN
        int year = LocalDate.now().getYear();
        long count = invoiceRepository.countFinalInvoicesForUserInYear(userId, year);
        String invoiceNumber = String.format("%d-%04d", year, count + 1);

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.FINAL);

        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    /**
     * Cancels a FINAL invoice. DRAFT and already CANCELLED invoices
     * cannot be cancelled.
     */
    @Transactional
    public InvoiceResponse cancelInvoice(UUID userId, UUID invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.FINAL) {
            throw new IllegalStateException(
                    "Only FINAL invoices can be cancelled. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    /**
     * Deletes a DRAFT invoice. FINAL and CANCELLED invoices cannot
     * be deleted (they must be kept for audit/tax purposes).
     */
    @Transactional
    public void deleteInvoice(UUID userId, UUID invoiceId) {
        Invoice invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be deleted. Current status: " + invoice.getStatus());
        }

        invoiceRepository.delete(invoice);
    }

    // ── Private helpers ─────────────────────────────────────────────

    private Invoice findInvoiceForUser(UUID userId, UUID invoiceId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Invoice not found with id: " + invoiceId));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .customerId(invoice.getCustomer().getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .serviceDate(invoice.getServiceDate())
                .status(invoice.getStatus())
                .vatMode(invoice.getVatMode())
                .currency(invoice.getCurrency())
                .totalNet(invoice.getTotalNet())
                .totalVat(invoice.getTotalVat())
                .totalGross(invoice.getTotalGross())
                .items(itemResponses)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .position(item.getPosition())
                .name(item.getName())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .vatPercentage(item.getVatPercentage())
                .totalNet(item.getTotalNet())
                .totalVat(item.getTotalVat())
                .totalGross(item.getTotalGross())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
