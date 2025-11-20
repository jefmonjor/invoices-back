-- Add settlement_number to invoices table
ALTER TABLE invoices ADD COLUMN settlement_number VARCHAR(50);

-- Extend invoice_number length to support more formats (PostgreSQL syntax)
ALTER TABLE invoices ALTER COLUMN invoice_number TYPE VARCHAR(50);

-- Add extended fields to invoice_items table
ALTER TABLE invoice_items ADD COLUMN item_date DATE;
ALTER TABLE invoice_items ADD COLUMN vehicle_plate VARCHAR(50);
ALTER TABLE invoice_items ADD COLUMN order_number VARCHAR(50);
ALTER TABLE invoice_items ADD COLUMN zone VARCHAR(100);
ALTER TABLE invoice_items ADD COLUMN gas_percentage DECIMAL(5, 2);

-- Create indexes for commonly queried fields
CREATE INDEX idx_invoice_items_item_date ON invoice_items(item_date);
CREATE INDEX idx_invoice_items_vehicle_plate ON invoice_items(vehicle_plate);
CREATE INDEX idx_invoice_items_zone ON invoice_items(zone);
CREATE INDEX idx_invoices_settlement_number ON invoices(settlement_number);
