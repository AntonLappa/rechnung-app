-- liquibase formatted sql

-- changeset antigravity:4
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    invoice_number VARCHAR(20),
    invoice_date DATE NOT NULL,
    service_date DATE,
    status VARCHAR(20) NOT NULL,
    vat_mode VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    total_net NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_vat NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_gross NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoices_customer_id FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT uq_invoices_user_invoice_number UNIQUE (user_id, invoice_number)
);

CREATE INDEX idx_invoices_user_id ON invoices (user_id);
CREATE INDEX idx_invoices_customer_id ON invoices (customer_id);
CREATE INDEX idx_invoices_status ON invoices (status);

-- changeset antigravity:5
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    position INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity NUMERIC(12, 4) NOT NULL,
    unit VARCHAR(30),
    unit_price NUMERIC(12, 2) NOT NULL,
    vat_percentage NUMERIC(5, 2) NOT NULL,
    total_net NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_vat NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_gross NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_items_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);
