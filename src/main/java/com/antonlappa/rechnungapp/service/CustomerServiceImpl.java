package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;

import com.antonlappa.rechnungapp.repository.CustomerRepository;

import com.antonlappa.rechnungapp.controller.dto.customer.CustomerRequestDto;
import com.antonlappa.rechnungapp.controller.dto.customer.CustomerResponseDto;
import com.antonlappa.rechnungapp.repository.entity.UserEntity;
import com.antonlappa.rechnungapp.repository.UserRepository;
import com.antonlappa.rechnungapp.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import com.antonlappa.rechnungapp.mapper.CustomerMapper;
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
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;

    /**
     * Returns all customers belonging to the authenticated user,
     * ordered alphabetically by name.
     *
     * @param userId the authenticated user's UUID
     */
    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers(UUID userId) {
        return customerRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(customerMapper::toDto)
                .toList();
    }

    /**
     * Returns a single customer by ID, scoped to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @throws ResourceNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomer(UUID userId, UUID customerId) {
        CustomerEntity customer = findCustomerForUser(userId, customerId);
        return customerMapper.toDto(customer);
    }

    /**
     * Creates a new customer for the authenticated user.
     *
     * @param userId  the authenticated user's UUID
     * @param request the customer data
     */
    @Transactional
    public CustomerResponseDto createCustomer(UUID userId, CustomerRequestDto request) {
        UserEntity user = userRepository.getReferenceById(userId);

        CustomerEntity customer = CustomerEntity.builder()
                .user(user)
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .taxNumber(request.getTaxNumber())
                .vatId(request.getVatId())
                .type(request.getType())
                .build();

        customerRepository.save(customer);
        return customerMapper.toDto(customer);
    }

    /**
     * Updates an existing customer belonging to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @param request    the updated customer data
     * @throws ResourceNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional
    public CustomerResponseDto updateCustomer(UUID userId, UUID customerId, CustomerRequestDto request) {
        CustomerEntity customer = findCustomerForUser(userId, customerId);

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setEmail(request.getEmail());
        customer.setTaxNumber(request.getTaxNumber());
        customer.setVatId(request.getVatId());
        customer.setType(request.getType());

        customerRepository.save(customer);
        return customerMapper.toDto(customer);
    }

    /**
     * Deletes a customer belonging to the authenticated user.
     *
     * @param userId     the authenticated user's UUID
     * @param customerId the customer's UUID
     * @throws ResourceNotFoundException if the customer does not exist
     *                                 or does not belong to this user
     */
    @Transactional
    public void deleteCustomer(UUID userId, UUID customerId) {
        CustomerEntity customer = findCustomerForUser(userId, customerId);
        customerRepository.delete(customer);
    }

    // ── Private helpers ──────────────────────────────────────────────

    /**
     * Finds a customer that belongs to the given user or throws 404.
     * This is the central access-control check — by requiring both IDs
     * in the query, we avoid information leakage about other users' data.
     */
    private CustomerEntity findCustomerForUser(UUID userId, UUID customerId) {
        return customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + customerId));
    }
}
