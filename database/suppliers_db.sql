CREATE DATABASE IF NOT EXISTS suppliers;
USE suppliers;

CREATE TABLE IF NOT EXISTS supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
);

CREATE TABLE IF NOT EXISTS supplier_receipts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT NOT NULL,
    receipt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total DECIMAL(10, 2) DEFAULT 0.00,
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS supplier_receipt_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    receipt_id INT NOT NULL,
    stock_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    cost_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (receipt_id) REFERENCES supplier_receipts(id) ON DELETE CASCADE
);

INSERT INTO supplier (name, phone) VALUES
('Rajesh Auto Parts', '9876543210'),
('Sharma Motors', '8765432109'),
('Singh Enterprises', '7654321098'),
('Gupta Traders', '6543210987'),
('Patel Spares Co.', '5432109876');

INSERT INTO supplier_receipts (supplier_id, receipt_date, total) VALUES
(1, '2026-07-01 10:30:00', 12500.00),
(1, '2026-07-15 11:00:00', 8500.00),
(2, '2026-07-05 09:45:00', 22000.00),
(3, '2026-07-10 14:20:00', 5600.00),
(4, '2026-07-20 16:00:00', 18200.00);

INSERT INTO supplier_receipt_items (receipt_id, stock_id, quantity, cost_price) VALUES
(1, 1, 10, 450.00),
(1, 2, 5, 1200.00),
(2, 3, 8, 650.00),
(3, 1, 20, 420.00),
(3, 4, 15, 800.00),
(4, 5, 6, 300.00),
(5, 2, 10, 1150.00),
(5, 6, 4, 1500.00);
