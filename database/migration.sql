-- Run this on your existing customers DB
ALTER TABLE bills ADD COLUMN paid BOOLEAN DEFAULT FALSE;

-- Run this if you already imported suppliers_db.sql
ALTER TABLE supplier_receipts ADD COLUMN paid BOOLEAN DEFAULT FALSE;
