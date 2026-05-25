package com.antonlappa.rechnungapp.mapper;

import com.antonlappa.rechnungapp.controller.dto.customer.CustomerResponseDto;
import com.antonlappa.rechnungapp.repository.entity.CustomerEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponseDto toDto(CustomerEntity customer) {
        return CustomerResponseDto.builder()
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
