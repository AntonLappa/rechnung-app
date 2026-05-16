package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.repository.entity.Customer;

import com.antonlappa.rechnungapp.repository.CustomerRepository;

import com.antonlappa.rechnungapp.controller.dto.CustomerRequest;
import com.antonlappa.rechnungapp.controller.dto.CustomerResponse;
import com.antonlappa.rechnungapp.repository.entity.User;
import com.antonlappa.rechnungapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for customer management.
 * <p>
 * Every operation is scoped to the authenticated user's UUID.
 * Attempting to access another user's customer returns a 404,
 * intentionally not revealing whether the resource exists.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Returns all customers belonging to the authenticated user,
     * ordered alphabetically by name.
     *
     * @param userId the authenticated user's UUID
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers(UUID userId) {
        return customerRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single customer by ID, scoped to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @throws EntityNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID userId, UUID customerId) {
        Customer customer = findCustomerForUser(userId, customerId);
        return toResponse(customer);
    }

    /**
     * Creates a new customer for the authenticated user.
     *
     * @param userId  the authenticated user's UUID
     * @param request the customer data
     */
    @Transactional
    public CustomerResponse createCustomer(UUID userId, CustomerRequest request) {
        User user = userRepository.getReferenceById(userId);

        Customer customer = Customer.builder()
                .user(user)
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .taxNumber(request.getTaxNumber())
                .vatId(request.getVatId())
                .type(request.getType())
                .build();

        customerRepository.save(customer);
        return toResponse(customer);
    }

    /**
     * Updates an existing customer belonging to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @param request    the updated customer data
     * @throws EntityNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional
    public CustomerResponse updateCustomer(UUID userId, UUID customerId, CustomerRequest request) {
        Customer customer = findCustomerForUser(userId, customerId);

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setEmail(request.getEmail());
        customer.setTaxNumber(request.getTaxNumber());
        customer.setVatId(request.getVatId());
        customer.setType(request.getType());

        customerRepository.save(customer);
        return toResponse(customer);
    }

    /**
     * Deletes a customer belonging to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @throws EntityNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional
    public void deleteCustomer(UUID userId, UUID customerId) {
        Customer customer = findCustomerForUser(userId, customerId);
        customerRepository.delete(customer);
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Finds a customer that belongs to the given user or throws 404.
     * This is the central access-control check — by requiring both IDs
     * in the query, we avoid information leakage about other users' data.
     */
    private Customer findCustomerForUser(UUID userId, UUID customerId) {
        return customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Customer not found with id: " + customerId));
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .address(customer.getAddress())
                .email(customer.getEmail())
                .taxNumber(customer.getTaxNumber())
                .vatId(customer.getVatId())
                .type(customer.getType())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
