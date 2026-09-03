-- ============================================================
-- suppliers_db.sql  —  Dump for `suppliers` database
-- Purpose: Stores suppliers, purchase receipts and receipt line items.
--          Used by SupplierScreen in Main.kt:325
--          JOIN chain: supplier -> supplier_receipts -> supplier_receipt_items -> stock.products
-- Restore: mysql -u root -proot < database/suppliers_db.sql
-- Note: stock_id logically references stock.products.id (cross-DB, app-enforced)
-- ============================================================

DROP DATABASE IF EXISTS suppliers;
CREATE DATABASE suppliers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE suppliers;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS supplier_receipt_items;
DROP TABLE IF EXISTS supplier_receipts;
DROP TABLE IF EXISTS supplier;

CREATE TABLE supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_receipts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10, 2) DEFAULT 0.00,
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_receipt_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    receipt_id INT NOT NULL,
    stock_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    cost_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (receipt_id) REFERENCES supplier_receipts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Seed Data
-- ----------------------------
INSERT INTO supplier (id, name, phone) VALUES
(1, 'Rajesh Auto Parts', '9876543210'),
(2, 'Sharma Motors', '8765432109'),
(3, 'Singh Enterprises', '7654321098'),
(4, 'Gupta Traders', '6543210987'),
(5, 'Patel Spares Co.', '5432109876');

INSERT INTO supplier_receipts (id, supplier_id, receipt_date, total, paid) VALUES
(1, 1, '2026-07-01 10:30:00', 12500.00, FALSE),
(2, 1, '2026-07-15 11:00:00', 8500.00, TRUE),
(3, 2, '2026-07-05 09:45:00', 22000.00, FALSE),
(4, 3, '2026-07-10 14:20:00', 5600.00, FALSE),
(5, 4, '2026-07-20 16:00:00', 18200.00, TRUE);

-- stock_id must match stock.products.id from stock_db.sql
INSERT INTO supplier_receipt_items (id, receipt_id, stock_id, quantity, cost_price) VALUES
(1, 1, 1, 10, 450.00),
(2, 1, 6, 5, 1500.00),
(3, 2, 3, 8, 250.00),
(4, 3, 1, 20, 420.00),
(5, 3, 4, 15, 350.00),
(6, 4, 5, 6, 800.00),
(7, 5, 2, 10, 120.00),
(8, 5, 6, 4, 1500.00);
