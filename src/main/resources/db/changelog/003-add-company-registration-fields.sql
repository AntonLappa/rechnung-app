-- liquibase formatted sql

-- changeset antigravity:6
ALTER TABLE company_profiles
    ADD COLUMN registration_number VARCHAR(50),
    ADD COLUMN registration_court VARCHAR(100);
