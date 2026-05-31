-- liquibase formatted sql

-- changeset antigravity:9
ALTER TABLE company_profiles
    ADD COLUMN street      VARCHAR(255),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN city        VARCHAR(100);
