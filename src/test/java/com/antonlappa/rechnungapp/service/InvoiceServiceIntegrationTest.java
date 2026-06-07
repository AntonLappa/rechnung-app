package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.exception.BusinessRuleException;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceItemRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceRequestDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.CustomerRepository;
import com.antonlappa.rechnungapp.repository.InvoiceRepository;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;
import com.antonlappa.rechnungapp.repository.entity.CustomerType;
import com.antonlappa.rechnungapp.repository.entity.InvoiceStatus;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.entity.UserRole;
import com.antonlappa.rechnungapp.repository.entity.PaymentMethod;
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
 * Integration tests for {@link InvoiceService}.
 * <p>
 * Uses the real PostgreSQL database (via Docker Compose)
 * and Liquibase migrations. Each test method runs inside
 * a transaction that is rolled back after completion.
 */
@SpringBootTest
@Transactional
class InvoiceServiceIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UUID userId;
    private UUID customerId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        // Create test user
        UserEntity user = UserEntity.builder()
                .email("test-invoice-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(user);
        userId = user.getId();

        // Create test customer for the user
        CustomerEntity customer = CustomerEntity.builder()
                .user(user)
                .name("Test Customer GmbH")
                .address("Musterstraße 1, 10115 Berlin")
                .email("customer@example.com")
                .type(CustomerType.BUSINESS)
                .build();
        customerRepository.save(customer);
        customerId = customer.getId();

        // Create a second user to test access isolation
        UserEntity otherUser = UserEntity.builder()
                .email("other-" + UUID.randomUUID() + "@example.com")
                .passwordHash("$2a$10$encodedPasswordHash")
                .firstName("Other")
                .lastName("User")
                .role(UserRole.USER)
                .build();
        userRepository.save(otherUser);
        otherUserId = otherUser.getId();
    }

    // ── Helper methods ──────────────────────────────────────────────

    private InvoiceRequestDto buildStandardDraftRequest() {
        return InvoiceRequestDto.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.of(2026, 5, 16))
                .serviceDate(LocalDate.of(2026, 5, 1))
                .vatMode(VatMode.STANDARD)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .currency("EUR")
                .items(List.of(
                        InvoiceItemRequestDto.builder()
                                .name("Web Development")
                                .description("Frontend implementation")
                                .quantity(new BigDecimal("10.00"))
                                .unit("hours")
                                .unitPrice(new BigDecimal("100.00"))
                                .vatPercentage(new BigDecimal("19.00"))
                                .build(),
                        InvoiceItemRequestDto.builder()
                                .name("Server Hosting")
                                .description("Monthly hosting fee")
                                .quantity(new BigDecimal("1.00"))
                                .unit("month")
                                .unitPrice(new BigDecimal("50.00"))
                                .vatPercentage(new BigDecimal("19.00"))
                                .build()
                ))
                .build();
    }

    // ── Test cases ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Create Draft Invoice")
    class CreateDraftInvoice {

        @Test
        @DisplayName("should create a DRAFT invoice with correct status and no invoice number")
        void shouldCreateDraftInvoice() {
            InvoiceRequestDto request = buildStandardDraftRequest();

            InvoiceResponseDto response = invoiceService.createInvoice(userId, request);

            assertNotNull(response.getId());
            assertEquals(InvoiceStatus.DRAFT, response.getStatus());
            assertNull(response.getInvoiceNumber(), "DRAFT invoices should not have an invoice number");
            assertEquals(customerId, response.getCustomerId());
            assertEquals(VatMode.STANDARD, response.getVatMode());
            assertEquals("EUR", response.getCurrency());
            assertEquals(2, response.getItems().size());
        }

        @Test
        @DisplayName("should reject invoice with invalid customer ID")
        void shouldRejectInvalidCustomer() {
            InvoiceRequestDto request = buildStandardDraftRequest();
            request.setCustomerId(UUID.randomUUID()); // non-existent

            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.createInvoice(userId, request));
        }
    }

    @Nested
    @DisplayName("Calculate Totals")
    class CalculateTotals {

        @Test
        @DisplayName("should calculate item and invoice totals correctly with 19% VAT")
        void shouldCalculateTotalsCorrectly() {
            InvoiceRequestDto request = buildStandardDraftRequest();

            InvoiceResponseDto response = invoiceService.createInvoice(userId, request);

            // Item 1: 10 * 100 = 1000.00 net, 190.00 VAT, 1190.00 gross
            var item1 = response.getItems().stream()
                    .filter(i -> i.getPosition() == 1).findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("1000.00").compareTo(item1.getTotalNet()));
            assertEquals(0, new BigDecimal("190.00").compareTo(item1.getTotalVat()));
            assertEquals(0, new BigDecimal("1190.00").compareTo(item1.getTotalGross()));

            // Item 2: 1 * 50 = 50.00 net, 9.50 VAT, 59.50 gross
            var item2 = response.getItems().stream()
                    .filter(i -> i.getPosition() == 2).findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("50.00").compareTo(item2.getTotalNet()));
            assertEquals(0, new BigDecimal("9.50").compareTo(item2.getTotalVat()));
            assertEquals(0, new BigDecimal("59.50").compareTo(item2.getTotalGross()));

            // Invoice totals: 1050.00 net, 199.50 VAT, 1249.50 gross
            assertEquals(0, new BigDecimal("1050.00").compareTo(response.getTotalNet()));
            assertEquals(0, new BigDecimal("199.50").compareTo(response.getTotalVat()));
            assertEquals(0, new BigDecimal("1249.50").compareTo(response.getTotalGross()));
        }

        @Test
        @DisplayName("should calculate 0% VAT for Kleinunternehmer mode")
        void shouldCalculateKleinunternehmerTotals() {
            InvoiceRequestDto request = InvoiceRequestDto.builder()
                    .customerId(customerId)
                    .invoiceDate(LocalDate.now())
                    .vatMode(VatMode.KLEINUNTERNEHMER)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .items(List.of(
                            InvoiceItemRequestDto.builder()
                                    .name("Consulting")
                                    .quantity(new BigDecimal("5.00"))
                                    .unit("hours")
                                    .unitPrice(new BigDecimal("80.00"))
                                    .vatPercentage(BigDecimal.ZERO)
                                    .build()
                    ))
                    .build();

            InvoiceResponseDto response = invoiceService.createInvoice(userId, request);

            assertEquals(0, new BigDecimal("400.00").compareTo(response.getTotalNet()));
            assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalVat()));
            assertEquals(0, new BigDecimal("400.00").compareTo(response.getTotalGross()));
        }

        @Test
        @DisplayName("should reject non-zero VAT for Kleinunternehmer mode")
        void shouldRejectNonZeroVatForKleinunternehmer() {
            InvoiceRequestDto request = InvoiceRequestDto.builder()
                    .customerId(customerId)
                    .invoiceDate(LocalDate.now())
                    .vatMode(VatMode.KLEINUNTERNEHMER)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .items(List.of(
                            InvoiceItemRequestDto.builder()
                                    .name("Invalid Item")
                                    .quantity(BigDecimal.ONE)
                                    .unit("pcs")
                                    .unitPrice(new BigDecimal("100.00"))
                                    .vatPercentage(new BigDecimal("19.00"))
                                    .build()
                    ))
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> invoiceService.createInvoice(userId, request));
        }

        @Test
        @DisplayName("should reject invalid VAT rate for STANDARD mode")
        void shouldRejectInvalidVatRate() {
            InvoiceRequestDto request = InvoiceRequestDto.builder()
                    .customerId(customerId)
                    .invoiceDate(LocalDate.now())
                    .vatMode(VatMode.STANDARD)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .items(List.of(
                            InvoiceItemRequestDto.builder()
                                    .name("Invalid Rate Item")
                                    .quantity(BigDecimal.ONE)
                                    .unit("pcs")
                                    .unitPrice(new BigDecimal("100.00"))
                                    .vatPercentage(new BigDecimal("10.00")) // not 0, 7, or 19
                                    .build()
                    ))
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> invoiceService.createInvoice(userId, request));
        }
    }

    @Nested
    @DisplayName("Finalize Invoice")
    class FinalizeInvoice {

        @Test
        @DisplayName("should finalize a DRAFT invoice and assign an invoice number")
        void shouldFinalizeAndAssignNumber() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            assertNull(draft.getInvoiceNumber());

            InvoiceResponseDto finalized = invoiceService.finalizeInvoice(userId, draft.getId());

            assertEquals(InvoiceStatus.FINAL, finalized.getStatus());
            assertNotNull(finalized.getInvoiceNumber());
            assertTrue(finalized.getInvoiceNumber().matches("\\d{4}-\\d{4}"),
                    "Invoice number should match YYYY-NNNN format, got: " + finalized.getInvoiceNumber());
        }

        @Test
        @DisplayName("should generate sequential invoice numbers")
        void shouldGenerateSequentialNumbers() {
            InvoiceResponseDto draft1 = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            InvoiceResponseDto draft2 = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            InvoiceResponseDto final1 = invoiceService.finalizeInvoice(userId, draft1.getId());
            InvoiceResponseDto final2 = invoiceService.finalizeInvoice(userId, draft2.getId());

            int year = LocalDate.now().getYear();
            assertEquals(year + "-0001", final1.getInvoiceNumber());
            assertEquals(year + "-0002", final2.getInvoiceNumber());
        }

        @Test
        @DisplayName("should not allow finalizing an already FINAL invoice")
        void shouldRejectDoubleFinalizing() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            invoiceService.finalizeInvoice(userId, draft.getId());

            assertThrows(BusinessRuleException.class,
                    () -> invoiceService.finalizeInvoice(userId, draft.getId()));
        }
    }

    @Nested
    @DisplayName("Edit Invoice")
    class EditInvoice {

        @Test
        @DisplayName("should block editing of FINAL invoices")
        void shouldBlockEditOfFinalInvoice() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            invoiceService.finalizeInvoice(userId, draft.getId());

            InvoiceRequestDto updateRequest = buildStandardDraftRequest();

            assertThrows(BusinessRuleException.class,
                    () -> invoiceService.updateInvoice(userId, draft.getId(), updateRequest));
        }

        @Test
        @DisplayName("should allow editing a DRAFT invoice")
        void shouldAllowEditOfDraft() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            InvoiceRequestDto updateRequest = InvoiceRequestDto.builder()
                    .customerId(customerId)
                    .invoiceDate(LocalDate.of(2026, 6, 1))
                    .vatMode(VatMode.STANDARD)
                    .paymentMethod(PaymentMethod.BANK_TRANSFER)
                    .items(List.of(
                            InvoiceItemRequestDto.builder()
                                    .name("Updated Item")
                                    .quantity(new BigDecimal("20.00"))
                                    .unit("hours")
                                    .unitPrice(new BigDecimal("150.00"))
                                    .vatPercentage(new BigDecimal("19.00"))
                                    .build()
                    ))
                    .build();

            InvoiceResponseDto updated = invoiceService.updateInvoice(userId, draft.getId(), updateRequest);

            assertEquals(1, updated.getItems().size());
            assertEquals("Updated Item", updated.getItems().getFirst().getName());
            assertEquals(0, new BigDecimal("3000.00").compareTo(updated.getTotalNet()));
        }
    }

    @Nested
    @DisplayName("Cancel Invoice")
    class CancelInvoice {

        @Test
        @DisplayName("should cancel a FINAL invoice")
        void shouldCancelFinalInvoice() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            invoiceService.finalizeInvoice(userId, draft.getId());

            InvoiceResponseDto cancelled = invoiceService.cancelInvoice(userId, draft.getId());

            assertEquals(InvoiceStatus.CANCELLED, cancelled.getStatus());
            assertNotNull(cancelled.getInvoiceNumber(), "Cancelled invoice should retain its number");
        }

        @Test
        @DisplayName("should not allow cancelling a DRAFT invoice")
        void shouldRejectCancelOfDraft() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            assertThrows(BusinessRuleException.class,
                    () -> invoiceService.cancelInvoice(userId, draft.getId()));
        }

        @Test
        @DisplayName("should not allow editing a CANCELLED invoice")
        void shouldBlockEditOfCancelled() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            invoiceService.finalizeInvoice(userId, draft.getId());
            invoiceService.cancelInvoice(userId, draft.getId());

            assertThrows(BusinessRuleException.class,
                    () -> invoiceService.updateInvoice(userId, draft.getId(), buildStandardDraftRequest()));
        }
    }

    @Nested
    @DisplayName("Delete Invoice")
    class DeleteInvoice {

        @Test
        @DisplayName("should delete a DRAFT invoice")
        void shouldDeleteDraft() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            assertDoesNotThrow(() -> invoiceService.deleteInvoice(userId, draft.getId()));

            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.getInvoice(userId, draft.getId()));
        }

        @Test
        @DisplayName("should not allow deleting a FINAL invoice")
        void shouldRejectDeleteOfFinal() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());
            invoiceService.finalizeInvoice(userId, draft.getId());

            assertThrows(BusinessRuleException.class,
                    () -> invoiceService.deleteInvoice(userId, draft.getId()));
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @DisplayName("should not allow another user to access an invoice")
        void shouldPreventCrossUserAccess() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.getInvoice(otherUserId, draft.getId()));
        }

        @Test
        @DisplayName("should not allow another user to finalize an invoice")
        void shouldPreventCrossUserFinalize() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.finalizeInvoice(otherUserId, draft.getId()));
        }

        @Test
        @DisplayName("should not allow another user to delete an invoice")
        void shouldPreventCrossUserDelete() {
            InvoiceResponseDto draft = invoiceService.createInvoice(userId, buildStandardDraftRequest());

            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.deleteInvoice(otherUserId, draft.getId()));
        }
    }
}
