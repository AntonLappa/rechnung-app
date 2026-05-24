package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.InvoiceItemRequest;
import com.antonlappa.rechnungapp.controller.dto.InvoiceRequest;
import com.antonlappa.rechnungapp.controller.dto.InvoiceResponse;
import com.antonlappa.rechnungapp.repository.CompanyProfileRepository;
import com.antonlappa.rechnungapp.repository.CustomerRepository;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CompanyProfile;
import com.antonlappa.rechnungapp.repository.entity.Customer;
import com.antonlappa.rechnungapp.repository.entity.CustomerType;
import com.antonlappa.rechnungapp.repository.entity.User;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import jakarta.persistence.EntityNotFoundException;
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
        User user = User.builder()
                .email("pdf-test-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        userId = user.getId();

        // Create company profile for the user (required for PDF generation)
        CompanyProfile profile = CompanyProfile.builder()
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
        Customer customer = Customer.builder()
                .user(user)
                .name("Kunde Übergrößen GmbH")
                .address("Kundenstraße 2\n10115 Berlin")
                .email("kunde@example.com")
                .type(CustomerType.BUSINESS)
                .build();
        customerRepository.save(customer);
        customerId = customer.getId();

        // Create a second user to test access isolation
        User otherUser = User.builder()
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

    private InvoiceRequest buildStandardInvoiceRequest() {
        return InvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.of(2026, 5, 19))
                .serviceDate(LocalDate.of(2026, 5, 1))
                .vatMode(VatMode.STANDARD)
                .currency("EUR")
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .position(1)
                                .name("Webentwicklung")
                                .description("Frontend-Implementierung mit Ümlauten: äöüß")
                                .quantity(new BigDecimal("10.00"))
                                .unit("Stunden")
                                .unitPrice(new BigDecimal("100.00"))
                                .vatPercentage(new BigDecimal("19.00"))
                                .build(),
                        InvoiceItemRequest.builder()
                                .position(2)
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

    private InvoiceRequest buildKleinunternehmerInvoiceRequest() {
        return InvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.of(2026, 5, 19))
                .serviceDate(LocalDate.of(2026, 5, 1))
                .vatMode(VatMode.KLEINUNTERNEHMER)
                .currency("EUR")
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .position(1)
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
    private InvoiceResponse createAndFinalizeInvoice(InvoiceRequest request) {
        InvoiceResponse draft = invoiceService.createInvoice(userId, request);
        return invoiceService.finalizeInvoice(userId, draft.getId());
    }

    // ── Test cases ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Generate PDF for FINAL invoice")
    class GeneratePdf {

        @Test
        @DisplayName("should generate a valid PDF for a FINAL standard invoice")
        void shouldGeneratePdfForFinalInvoice() {
            InvoiceResponse finalized = createAndFinalizeInvoice(buildStandardInvoiceRequest());

            byte[] pdf = invoicePdfService.generatePdf(userId, finalized.getId());

            assertNotNull(pdf);
            assertTrue(pdf.length > 0, "PDF should not be empty");
            // PDF files start with %PDF
            String header = new String(pdf, 0, Math.min(5, pdf.length));
            assertTrue(header.startsWith("%PDF"), "Generated file should be a valid PDF, got: " + header);
        }

        @Test
        @DisplayName("should generate a valid PDF for a Kleinunternehmer invoice")
        void shouldGeneratePdfForKleinunternehmerInvoice() {
            InvoiceResponse finalized = createAndFinalizeInvoice(buildKleinunternehmerInvoiceRequest());

            byte[] pdf = invoicePdfService.generatePdf(userId, finalized.getId());

            assertNotNull(pdf);
            assertTrue(pdf.length > 0, "PDF should not be empty");
            String header = new String(pdf, 0, Math.min(5, pdf.length));
            assertTrue(header.startsWith("%PDF"), "Generated file should be a valid PDF");
        }
    }

    @Nested
    @DisplayName("Block PDF for non-FINAL invoices")
    class BlockNonFinal {

        @Test
        @DisplayName("should block PDF generation for DRAFT invoice")
        void shouldBlockPdfForDraftInvoice() {
            InvoiceResponse draft = invoiceService.createInvoice(userId, buildStandardInvoiceRequest());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> invoicePdfService.generatePdf(userId, draft.getId()));

            assertTrue(ex.getMessage().contains("FINAL"),
                    "Error message should mention FINAL status requirement");
        }

        @Test
        @DisplayName("should block PDF generation for CANCELLED invoice")
        void shouldBlockPdfForCancelledInvoice() {
            InvoiceResponse finalized = createAndFinalizeInvoice(buildStandardInvoiceRequest());
            invoiceService.cancelInvoice(userId, finalized.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
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
            InvoiceResponse finalized = createAndFinalizeInvoice(buildStandardInvoiceRequest());

            assertThrows(EntityNotFoundException.class,
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
            Customer otherCustomer = Customer.builder()
                    .user(userRepository.getReferenceById(otherUserId))
                    .name("Other Customer")
                    .address("Other Street 1\n10115 Berlin")
                    .type(CustomerType.BUSINESS)
                    .build();
            customerRepository.save(otherCustomer);

            InvoiceRequest request = InvoiceRequest.builder()
                    .customerId(otherCustomer.getId())
                    .invoiceDate(LocalDate.now())
                    .vatMode(VatMode.STANDARD)
                    .currency("EUR")
                    .items(List.of(
                            InvoiceItemRequest.builder()
                                    .position(1)
                                    .name("Test")
                                    .quantity(BigDecimal.ONE)
                                    .unit("pcs")
                                    .unitPrice(new BigDecimal("100.00"))
                                    .vatPercentage(new BigDecimal("19.00"))
                                    .build()
                    ))
                    .build();

            InvoiceResponse draft = invoiceService.createInvoice(otherUserId, request);
            InvoiceResponse finalized = invoiceService.finalizeInvoice(otherUserId, draft.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> invoicePdfService.generatePdf(otherUserId, finalized.getId()));

            assertTrue(ex.getMessage().contains("Company profile"),
                    "Error message should mention missing company profile");
        }
    }
}
