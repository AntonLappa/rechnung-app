--liquibase formatted sql

--changeset antonlappa:008-add-storno-support
ALTER TABLE invoices
    ADD COLUMN storno_of_invoice_id UUID REFERENCES invoices(id);
