package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.CustomerRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;
import com.antonlappa.rechnungapp.mapper.InvoiceMapper;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceCalculationService calculationService;

    // ── Queries ─────────────────────────────────────────────────────

    /**
     * Returns all invoices for the authenticated user, newest first.
     * Optionally filters by status and/or customer.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoices(UUID userId, InvoiceStatus status, UUID customerId) {
        List<InvoiceEntity> invoices;

        if (status != null && customerId != null) {
            invoices = invoiceRepository.findAllByUserIdAndStatusAndCustomerIdOrderByCreatedAtDesc(userId, status, customerId);
        } else if (status != null) {
            invoices = invoiceRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else if (customerId != null) {
            invoices = invoiceRepository.findAllByUserIdAndCustomerIdOrderByCreatedAtDesc(userId, customerId);
        } else {
            invoices = invoiceRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        }

        return invoices.stream()
                .map(invoiceMapper::toDto)
                .toList();
    }

    /**
     * Returns a single invoice by ID, scoped to the authenticated user.
     */
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoice(UUID userId, UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceForUser(userId, invoiceId);
        return invoiceMapper.toDto(invoice);
    }

    // ── Commands ────────────────────────────────────────────────────

    /**
     * Creates a new DRAFT invoice with calculated totals.
     */
    @Transactional
    public InvoiceResponseDto createInvoice(UUID userId, InvoiceRequestDto request) {
        // Verify customer belongs to this user
        CustomerEntity customer = customerRepository.findByIdAndUserId(request.getCustomerId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        InvoiceEntity invoice = InvoiceEntity.builder()
                .user(userRepository.getReferenceById(userId))
                .customer(customer)
                .invoiceDate(request.getInvoiceDate())
                .serviceDate(request.getServiceDate())
                .status(InvoiceStatus.DRAFT)
                .vatMode(request.getVatMode())
                .paymentMethod(request.getPaymentMethod())
                .currency(request.getCurrency() != null ? request.getCurrency() : "EUR")
                .build();

        // Build and validate items
        calculationService.buildItems(invoice, request.getItems(), request.getVatMode());

        // Calculate invoice totals from items
        calculationService.recalculateTotals(invoice);

        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoice);
    }

    /**
     * Updates an existing DRAFT invoice. FINAL and CANCELLED invoices
     * cannot be edited.
     */
    @Transactional
    public InvoiceResponseDto updateInvoice(UUID userId, UUID invoiceId, InvoiceRequestDto request) {
        InvoiceEntity invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be edited. Current status: " + invoice.getStatus());
        }

        // Verify customer belongs to this user
        CustomerEntity customer = customerRepository.findByIdAndUserId(request.getCustomerId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + request.getCustomerId()));

        invoice.setCustomer(customer);
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setServiceDate(request.getServiceDate());
        invoice.setVatMode(request.getVatMode());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setCurrency(request.getCurrency() != null ? request.getCurrency() : "EUR");

        // Replace items
        invoice.getItems().clear();
        calculationService.buildItems(invoice, request.getItems(), request.getVatMode());

        // Recalculate totals
        calculationService.recalculateTotals(invoice);

        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoice);
    }

    /**
     * Finalizes a DRAFT invoice: assigns a sequential invoice number
     * and locks it from further edits.
     *
     * The number is generated from the user's CompanyProfile settings
     * (invoiceNumberPrefix, invoiceNumberStart). The sequence always
     * advances past the highest existing number so duplicates are impossible.
     */
    @Transactional
    public InvoiceResponseDto finalizeInvoice(UUID userId, UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be finalized. Current status: " + invoice.getStatus());
        }

        CompanyProfileEntity profile = companyProfileRepository.findByUserId(userId).orElse(null);
        String customPrefix = (profile != null && profile.getInvoiceNumberPrefix() != null)
                ? profile.getInvoiceNumberPrefix().trim()
                : null;
        String prefix = (customPrefix != null && !customPrefix.isEmpty())
                ? customPrefix
                : String.valueOf(LocalDate.now().getYear());
        int startNumber = (profile != null) ? profile.getInvoiceNumberStart() : 1;

        int maxExisting = invoiceRepository.findAllInvoiceNumbersByUserId(userId).stream()
                .mapToInt(InvoiceServiceImpl::parseSequenceSuffix)
                .max()
                .orElse(0);

        int next = Math.max(maxExisting + 1, startNumber);
        invoice.setInvoiceNumber(formatInvoiceNumber(prefix, next));
        invoice.setStatus(InvoiceStatus.FINAL);

        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoice);
    }

    /**
     * Cancels a FINAL invoice. DRAFT and already CANCELLED invoices
     * cannot be cancelled.
     */
    @Transactional
    public InvoiceResponseDto cancelInvoice(UUID userId, UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.FINAL) {
            throw new BusinessRuleException(
                    "Only FINAL invoices can be cancelled. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        return invoiceMapper.toDto(invoice);
    }

    /**
     * Deletes a DRAFT invoice. FINAL and CANCELLED invoices cannot
     * be deleted (they must be kept for audit/tax purposes).
     */
    @Transactional
    public void deleteInvoice(UUID userId, UUID invoiceId) {
        InvoiceEntity invoice = findInvoiceForUser(userId, invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT invoices can be deleted. Current status: " + invoice.getStatus());
        }

        invoiceRepository.delete(invoice);
    }

    // ── Private helpers ─────────────────────────────────────────────

    private InvoiceEntity findInvoiceForUser(UUID userId, UUID invoiceId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with id: " + invoiceId));
    }

    private static String formatInvoiceNumber(String prefix, int sequence) {
        String seq = String.format("%04d", sequence);
        return (prefix != null && !prefix.isEmpty()) ? prefix + "-" + seq : seq;
    }

    // Parses the trailing numeric segment (after the last '-') from an invoice number.
    // Returns 0 if the number cannot be parsed (e.g. legacy or unexpected format).
    private static int parseSequenceSuffix(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            return 0;
        }
        int lastDash = invoiceNumber.lastIndexOf('-');
        String numPart = (lastDash >= 0) ? invoiceNumber.substring(lastDash + 1) : invoiceNumber;
        try {
            return Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
