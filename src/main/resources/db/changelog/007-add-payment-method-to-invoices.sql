--liquibase formatted sql

--changeset antonlappa:007-add-payment-method-to-invoices
ALTER TABLE invoices ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER';
