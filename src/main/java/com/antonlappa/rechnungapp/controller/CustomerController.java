package com.antonlappa.rechnungapp.controller;

import com.antonlappa.rechnungapp.service.CustomerService;

import com.antonlappa.rechnungapp.controller.dto.customer.CustomerRequestDto;
import com.antonlappa.rechnungapp.controller.dto.customer.CustomerResponseDto;
import com.antonlappa.rechnungapp.service.AuthenticatedUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * REST controller for managing the authenticated user's customers.
 * <p>
 * All endpoints require a valid JWT. The user's identity is extracted
 * from the security context via {@link AuthenticationPrincipal}.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code GET    /api/v1/customers}       – list all customers</li>
 *   <li>{@code GET    /api/v1/customers/{id}}   – get a single customer</li>
 *   <li>{@code POST   /api/v1/customers}        – create a customer</li>
 *   <li>{@code PUT    /api/v1/customers/{id}}   – update a customer</li>
 *   <li>{@code DELETE /api/v1/customers/{id}}   – delete a customer</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * GET /api/v1/customers
     * <p>
     * Returns all customers belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponseDto>> getAllCustomers(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(customerService.getAllCustomers(userId));
    }

    /**
     * GET /api/v1/customers/{id}
     * <p>
     * Returns a single customer by ID.
     * Returns 404 if the customer does not exist or does not belong to this user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> getCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(customerService.getCustomer(userId, id));
    }

    /**
     * POST /api/v1/customers
     * <p>
     * Creates a new customer for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CustomerRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        CustomerResponseDto response = customerService.createCustomer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/customers/{id}
     * <p>
     * Updates an existing customer.
     * Returns 404 if the customer does not exist or does not belong to this user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequestDto request) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        return ResponseEntity.ok(customerService.updateCustomer(userId, id, request));
    }

    /**
     * DELETE /api/v1/customers/{id}
     * <p>
     * Deletes a customer.
     * Returns 404 if the customer does not exist or does not belong to this user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        UUID userId = authenticatedUserResolver.resolveUserId(userDetails);
        customerService.deleteCustomer(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ──────────────────────────────────────────────

}
