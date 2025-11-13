-- Tabla de facturas
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    client_id BIGINT NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tabla de items de factura
CREATE TABLE invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoices_client_id ON invoices(client_id);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_date ON invoices(invoice_date);
CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);

-- Insertar factura de ejemplo
INSERT INTO invoices (id, invoice_number, client_id, client_email, invoice_date, due_date, subtotal, tax, total, status)
VALUES (1, 'INV-2025-001', 1, 'admin@invoices.com', CURRENT_DATE, CURRENT_DATE + 30, 1000.00, 190.00, 1190.00, 'PENDING');

INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, total)
VALUES
    (1, 'Consultoría de Software', 10, 100.00, 1000.00);
