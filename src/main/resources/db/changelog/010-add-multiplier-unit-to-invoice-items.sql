--liquibase formatted sql

--changeset antonlappa:010-add-multiplier-unit-to-invoice-items
ALTER TABLE invoice_items ADD COLUMN multiplier_unit VARCHAR(20);
