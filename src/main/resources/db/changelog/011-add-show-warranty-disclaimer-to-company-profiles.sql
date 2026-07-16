--liquibase formatted sql

--changeset antonlappa:011-add-show-warranty-disclaimer-to-company-profiles
ALTER TABLE company_profiles ADD COLUMN show_warranty_disclaimer BOOLEAN NOT NULL DEFAULT true;
