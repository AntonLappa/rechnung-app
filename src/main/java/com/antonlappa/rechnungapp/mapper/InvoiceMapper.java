package com.antonlappa.rechnungapp.mapper;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceItemResponseDto;
import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceResponseDto;
import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceResponseDto toDto(InvoiceEntity invoice) {
        List<InvoiceItemResponseDto> itemResponses = invoice.getItems().stream()
                .map(this::toItemDto)
                .toList();

        return InvoiceResponseDto.builder()
                .id(invoice.getId())
                .customerId(invoice.getCustomer().getId())
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

    private InvoiceItemResponseDto toItemDto(InvoiceItemEntity item) {
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
