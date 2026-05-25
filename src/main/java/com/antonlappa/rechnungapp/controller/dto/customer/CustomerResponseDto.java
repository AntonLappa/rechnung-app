package com.antonlappa.rechnungapp.controller.dto.customer;

import com.antonlappa.rechnungapp.repository.entity.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned when reading a customer.
 * <p>
 * Excludes the internal {@code user} relationship and exposes
 * only the data relevant to the API consumer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponseDto {

    private UUID id;
    private String name;
    private String address;
    private String email;
    private String taxNumber;
    private String vatId;
    private CustomerType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
