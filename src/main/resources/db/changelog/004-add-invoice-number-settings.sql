-- liquibase formatted sql

-- changeset antigravity:7
ALTER TABLE company_profiles
    ADD COLUMN invoice_number_prefix VARCHAR(20),
    ADD COLUMN invoice_number_start INTEGER NOT NULL DEFAULT 1;
