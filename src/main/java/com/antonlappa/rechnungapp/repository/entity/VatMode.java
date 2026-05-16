package com.antonlappa.rechnungapp.repository.entity;

/**
 * VAT calculation mode for an invoice.
 * <ul>
 *   <li>{@code STANDARD} – normal VAT rates (0%, 7%, 19% in Germany).</li>
 *   <li>{@code VAT_FREE} – VAT-exempt transaction (e.g. intra-EU reverse charge).</li>
 *   <li>{@code KLEINUNTERNEHMER} – small business exemption per §19 UStG, no VAT charged.</li>
 * </ul>
 */
public enum VatMode {
    STANDARD,
    VAT_FREE,
    KLEINUNTERNEHMER
}
