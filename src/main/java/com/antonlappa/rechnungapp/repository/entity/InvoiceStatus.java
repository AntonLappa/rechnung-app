package com.antonlappa.rechnungapp.repository.entity;

/**
 * Lifecycle status of an invoice.
 * <ul>
 *   <li>{@code DRAFT} – editable, no invoice number assigned yet.</li>
 *   <li>{@code FINAL} – locked, invoice number generated, can only be cancelled.</li>
 *   <li>{@code CANCELLED} – permanently locked, kept for audit trail.</li>
 * </ul>
 */
public enum InvoiceStatus {
    DRAFT,
    FINAL,
    CANCELLED
}
