-- liquibase formatted sql

-- changeset antigravity:8
ALTER TABLE company_profiles
    ADD COLUMN registration_country VARCHAR(100),
    ADD COLUMN bank_name            VARCHAR(100);
