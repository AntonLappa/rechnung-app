--liquibase formatted sql

--changeset antonlappa:009-add-multiplier-to-invoice-items
ALTER TABLE invoice_items ADD COLUMN multiplier DECIMAL(12,4);
