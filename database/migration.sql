-- Deprecated: fresh dumps (stock_db.sql, suppliers_db.sql, customers_db.sql, insurance_db.sql)
-- already include `paid` columns. Run only if you imported dumps created before 2026-09.

-- Run this on your existing customers DB if `bills.paid` is missing:
-- ALTER TABLE bills ADD COLUMN paid BOOLEAN DEFAULT FALSE;

-- Run this if you already imported old suppliers_db.sql without `paid`:
-- ALTER TABLE supplier_receipts ADD COLUMN paid BOOLEAN DEFAULT FALSE;
