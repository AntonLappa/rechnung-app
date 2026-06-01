package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceItemRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.CustomerRepository;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfileEntity;
import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;
import com.antonlappa.rechnungapp.repository.entity.CustomerType;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link InvoicePdfService}.
 * <p>
 * Uses the real PostgreSQL database (via Docker Compose)
 * and Liquibase migrations. Each test method runs inside
 * a transaction that is rolled back after completion.
 */
@SpringBootTest
@Transactional
class InvoicePdfServiceIntegrationTest {

    @Autowired
    private InvoicePdfService invoicePdfService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UUID userId;
    private UUID customerId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        // Create test user
        UserEntity user = UserEntity.builder()
                .email("pdf-test-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        userId = user.getId();

        // Create company profile for the user (required for PDF generation)
        CompanyProfileEntity profile = CompanyProfileEntity.builder()
                .user(user)
                .companyName("Testfirma GmbH")
                .ownerName("Max Müstermann")
                .address("Musterstraße 1\n12345 Berlin")
                .taxNumber("12/345/67890")
                .vatId("DE123456789")
                .iban("DE89 3704 0044 0532 0130 00")
                .bic("COBADEFFXXX")
                .email("info@testfirma.de")
                .phone("+49 30 123456")
                .smallBusiness(false)
                .build();
        companyProfileRepository.save(profile);

        // Create test customer
        CustomerEntity customer = CustomerEntity.builder()
                .user(user)
                .name("Kunde Übergrößen GmbH")
                .address("Kundenstraße 2\n10115 Berlin")
                .email("kunde@example.com")
                .type(CustomerType.BUSINESS)
                .build();
        customerRepository.save(customer);
        customerId = customer.getId();

        // Create a second user to test access isolation
        UserEntity otherUser = UserEntity.builder()
                .email("other-pdf-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Other")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(otherUser);
        otherUserId = otherUser.getId();
    }

    // ── Helper methods ──────────────────────────────────────────────

    private InvoiceRequestDto buildStandardInvoiceRequestDto() {
        return InvoiceRequestDto.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.of(2026, 5, 19))
                .serviceDate(LocalDate.of(2026, 5, 1))
                .vatMode(VatMode.STANDARD)
                .currency("EUR")
                .items(List.of(
                        InvoiceItemRequestDto.builder()
                                .name("Webentwicklung")
                                .description("Frontend-Implementierung mit Ümlauten: äöüß")
                                .quantity(new BigDecimal("10.00"))
                                .unit("Stunden")
                                .unitPrice(new BigDecimal("100.00"))
                                .vatPercentage(new BigDecimal("19.00"))
                                .build(),
                        InvoiceItemRequestDto.builder()
                                .name("Server-Hosting")
                                .description("Monatliche Hosting-Gebühr")
                                .quantity(new BigDecimal("1.00"))
                                .unit("Monat")
                                .unitPrice(new BigDecimal("50.00"))
                                .vatPercentage(new BigDecimal("19.00"))
                                .build()
                ))
                .build();
    }

    private InvoiceRequestDto buildKleinunternehmerInvoiceRequestDto() {
        return InvoiceRequestDto.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.of(2026, 5, 19))
                .serviceDate(LocalDate.of(2026, 5, 1))
                .vatMode(VatMode.KLEINUNTERNEHMER)
                .currency("EUR")
                .items(List.of(
                        InvoiceItemRequestDto.builder()
                                .name("Beratungsleistung")
                                .description("Strategieberatung für Geschäftsentwicklung")
                                .quantity(new BigDecimal("5.00"))
                                .unit("Stunden")
                                .unitPrice(new BigDecimal("80.00"))
                                .vatPercentage(BigDecimal.ZERO)
                                .build()
                ))
                .build();
    }

    /**
     * Creates an invoice and finalizes it, returning the finalized response.
     */
    private InvoiceResponseDto createAndFinalizeInvoice(InvoiceRequestDto request) {
        InvoiceResponseDto draft = invoiceService.createInvoice(userId, request);
        return invoiceService.finalizeInvoice(userId, draft.getId());
    }

    // ── Test cases ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Generate PDF for FINAL invoice")
    class GeneratePdf {

        @Test
        @DisplayName("should generate a valid PDF for a FINAL standard invoice")
        void shouldGeneratePdfForFinalInvoice() {
            InvoiceResponseDto finalized = createAndFinalizeInvoice(buildStandardInvoiceRequestDto());

            PdfDocument pdf = invoicePdfService.generatePdf(userId, finalized.getId());

            assertNotNull(pdf.data());
            assertTrue(pdf.data().length > 0, "PDF should not be empty");
            // PDF files start with %PDF
            String header = new String(pdf.data(), 0, Math.min(5, pdf.data().length));
            assertTrue(header.startsWith("%PDF"), "Generated file should be a valid PDF, got: " + header);
        }

        @Test
        @DisplayName("should generate a valid PDF for a Kleinunternehmer invoice")
        void shouldGeneratePdfForKleinunternehmerInvoice() {
            InvoiceResponseDto finalized = createAndFinalizeInvoice(buildKleinunternehmerInvoiceRequestDto());

            PdfDocument pdf = invoicePdfService.generatePdf(userId, finalized.getId());

            assertNotNull(pdf.data());
            assertTrue(pdf.data().length > 0, "PDF should not be empty");
            String header = new String(pdf.data(), 0, Math.min(5, pdf.data().length));
            assertTrue(header.startsWith("%PDF"), "Generated file should be a valid PDF");
        }
    }

    @Nested
    @DisplayName("Block PDF for non-FINAL invoices")
    class BlockNonFinal {

        @Test
        @DisplayName("should block PDF generation for DRAFT invoice")
        void shouldBlockPdfForDraftInvoice() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardInvoiceRequestDto());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> invoicePdfService.generatePdf(userId, draft.getId()));

            assertTrue(ex.getMessage().contains("FINAL"),
                    "Error message should mention FINAL status requirement");
        }

        @Test
        @DisplayName("should block PDF generation for CANCELLED invoice")
        void shouldBlockPdfForCancelledInvoice() {
            InvoiceResponseDto finalized = createAndFinalizeInvoice(buildStandardInvoiceRequestDto());
            invoiceService.cancelInvoice(userId, finalized.getId());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> invoicePdfService.generatePdf(userId, finalized.getId()));

            assertTrue(ex.getMessage().contains("FINAL"),
                    "Error message should mention FINAL status requirement");
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @DisplayName("should block PDF generation for another user's invoice")
        void shouldBlockPdfForOtherUsersInvoice() {
            InvoiceResponseDto finalized = createAndFinalizeInvoice(buildStandardInvoiceRequestDto());

            assertThrows(ResourceNotFoundException.class,
                    () -> invoicePdfService.generatePdf(otherUserId, finalized.getId()));
        }
    }

    @Nested
    @DisplayName("Missing Company Profile")
    class MissingProfile {

        @Test
        @DisplayName("should return error when company profile is missing")
        void shouldFailWhenNoCompanyProfile() {
            // Create invoice for the other user who has no company profile
            CustomerEntity otherCustomer = CustomerEntity.builder()
                    .user(userRepository.getReferenceById(otherUserId))
                    .name("Other Customer")
                    .address("Other Street 1\n10115 Berlin")
                    .type(CustomerType.BUSINESS)
                    .build();
            customerRepository.save(otherCustomer);

            InvoiceRequestDto request = InvoiceRequestDto.builder()
                    .customerId(otherCustomer.getId())
                    .invoiceDate(LocalDate.now())
                    .vatMode(VatMode.STANDARD)
                    .currency("EUR")
                    .items(List.of(
                            InvoiceItemRequestDto.builder()
                                    .name("Test")
                                    .quantity(BigDecimal.ONE)
                                    .unit("pcs")
                                    .unitPrice(new BigDecimal("100.00"))
                                    .vatPercentage(new BigDecimal("19.00"))
                                    .build()
                    ))
                    .build();

            InvoiceResponseDto draft = invoiceService.createInvoice(otherUserId, request);
            InvoiceResponseDto finalized = invoiceService.finalizeInvoice(otherUserId, draft.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> invoicePdfService.generatePdf(otherUserId, finalized.getId()));

            assertTrue(ex.getMessage().contains("Company profile"),
                    "Error message should mention missing company profile");
        }
    }
}
