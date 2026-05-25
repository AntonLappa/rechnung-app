package com.antonlappa.rechnungapp.mapper;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceItemResponseDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper component that translates database entities into API transfer objects (DTOs)
 * for invoice resources.
 */
@Component
public class InvoiceMapper {

    public InvoiceResponseDto toDto(InvoiceEntity invoice) {
        if (invoice == null) {
            return null;
        }

        List<InvoiceItemResponseDto> itemResponses = invoice.getItems() != null ? invoice.getItems().stream()
                .map(this::toItemDto)
                .toList() : List.of();

        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : null)
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .serviceDate(invoice.getServiceDate())
                .status(invoice.getStatus())
                .vatMode(invoice.getVatMode())
                .currency(invoice.getCurrency())
                .totalNet(invoice.getTotalNet())
                .totalVat(invoice.getTotalVat())
                .totalGross(invoice.getTotalGross())
                .items(itemResponses)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    public InvoiceItemResponseDto toItemDto(InvoiceItemEntity item) {
        if (item == null) {
            return null;
        }

        return InvoiceItemResponseDto.builder()
                .id(item.getId())
                .position(item.getPosition())
                .name(item.getName())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .unitPrice(item.getUnitPrice())
                .vatPercentage(item.getVatPercentage())
                .totalNet(item.getTotalNet())
                .totalVat(item.getTotalVat())
                .totalGross(item.getTotalGross())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
