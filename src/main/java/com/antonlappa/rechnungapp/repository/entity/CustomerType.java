package com.antonlappa.rechnungapp.repository.entity;

/**
 * Distinguishes between private individuals and business customers.
 * <p>
 * Business customers typically have a VAT ID and different
 * invoicing rules (e.g., reverse-charge mechanism within the EU).
 */
public enum CustomerType {
    PRIVATE,
    BUSINESS
}
