package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.customer.CustomerRequestDto;
import com.antonlappa.rechnungapp.controller.dto.customer.CustomerResponseDto;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerResponseDto> getAllCustomers(UUID userId);

    CustomerResponseDto getCustomer(UUID userId, UUID customerId);

    CustomerResponseDto createCustomer(UUID userId, CustomerRequestDto request);

    CustomerResponseDto updateCustomer(UUID userId, UUID customerId, CustomerRequestDto request);

    void deleteCustomer(UUID userId, UUID customerId);
}
