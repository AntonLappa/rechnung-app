package com.antonlappa.rechnungapp.service;

import com.antonlappa.rechnungapp.controller.dto.invoice.InvoiceItemRequestDto;
import com.antonlappa.rechnungapp.repository.entity.InvoiceEntity;
import com.antonlappa.rechnungapp.repository.entity.InvoiceItemEntity;
import com.antonlappa.rechnungapp.repository.entity.VatMode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * Dedicated service for invoice monetary calculations and VAT validation.
 * <p>
 * Encapsulates all arithmetic logic so that {@link InvoiceService}
 * stays focused on orchestration (persistence, status transitions,
 * access control).
 * <p>
 * All monetary values use {@link BigDecimal} with scale 2 and
 * {@link RoundingMode#HALF_UP}.
 */
@Service
public class InvoiceCalculationService {

    private static final Set<BigDecimal> STANDARD_VAT_RATES = Set.of(
            BigDecimal.ZERO,
            new BigDecimal("7.00"),
            new BigDecimal("19.00")
    );

    /**
     * Builds {@link InvoiceItem} entities from the request DTOs,
     * validates VAT rates against the invoice's VAT mode, and
     * computes per-item totals (totalNet, totalVat, totalGross).
     *
     * @param invoice      the parent invoice to attach items to
     * @param itemRequests the line item DTOs from the client
     * @param vatMode      the invoice's VAT mode (used for validation)
     */
    public void buildItems(InvoiceEntity invoice, List<InvoiceItemRequestDto> itemRequests, VatMode vatMode) {
        for (int i = 0; i < itemRequests.size(); i++) {
            InvoiceItemRequestDto req = itemRequests.get(i);
            validateVatRate(req.getVatPercentage(), vatMode);

            BigDecimal effectiveQuantity = effectiveQuantity(req.getQuantity(), req.getMultiplier());

            BigDecimal totalNet = effectiveQuantity
                    .multiply(req.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalVat = totalNet
                    .multiply(req.getVatPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal totalGross = totalNet.add(totalVat);

            InvoiceItemEntity item = InvoiceItemEntity.builder()
                    .invoice(invoice)
                    .position(i + 1)
                    .name(req.getName())
                    .description(req.getDescription())
                    .quantity(req.getQuantity())
                    .multiplier(req.getMultiplier())
                    .unit(req.getUnit())
                    .unitPrice(req.getUnitPrice())
                    .vatPercentage(req.getVatPercentage())
                    .totalNet(totalNet)
                    .totalVat(totalVat)
                    .totalGross(totalGross)
                    .build();

            invoice.getItems().add(item);
        }
    }

    /**
     * Returns the quantity to use for monetary calculations: {@code quantity × multiplier}
     * when a multiplier is present, otherwise {@code quantity} as-is.
     */
    public BigDecimal effectiveQuantity(BigDecimal quantity, BigDecimal multiplier) {
        return multiplier != null ? quantity.multiply(multiplier) : quantity;
    }

    /**
     * Recalculates invoice-level totals by summing all item totals.
     *
     * @param invoice the invoice whose totals should be recomputed
     */
    public void recalculateTotals(InvoiceEntity invoice) {
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;

        for (InvoiceItemEntity item : invoice.getItems()) {
            totalNet = totalNet.add(item.getTotalNet());
            totalVat = totalVat.add(item.getTotalVat());
            totalGross = totalGross.add(item.getTotalGross());
        }

        invoice.setTotalNet(totalNet.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalVat(totalVat.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalGross(totalGross.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Validates that the given VAT percentage is allowed for the
     * specified VAT mode.
     *
     * @param vatPercentage the VAT rate to validate
     * @param vatMode       the active VAT mode
     * @throws IllegalArgumentException if the rate is invalid for the mode
     */
    public void validateVatRate(BigDecimal vatPercentage, VatMode vatMode) {
        BigDecimal rate = vatPercentage.setScale(2, RoundingMode.HALF_UP);

        switch (vatMode) {
            case STANDARD -> {
                if (!STANDARD_VAT_RATES.contains(rate)) {
                    throw new IllegalArgumentException(
                            "Invalid VAT rate " + rate + "% for STANDARD mode. Allowed: 0%, 7%, 19%");
                }
            }
            case VAT_FREE, KLEINUNTERNEHMER -> {
                if (rate.compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException(
                            "VAT must be 0% for " + vatMode + " mode, but got " + rate + "%");
                }
            }
        }
    }
}
