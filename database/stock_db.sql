-- ============================================================
-- stock_db.sql  —  Dump for `stock` database
-- Purpose: Stores product catalogue / inventory (quantity, rack, prices)
--          Used by ProductScreen, CustomerScreen and SupplierScreen
--          Main.kt:565 SELECT * FROM products ORDER BY product_name
-- Restore: mysql -u root -proot < database/stock_db.sql
-- ============================================================

DROP DATABASE IF EXISTS stock;
CREATE DATABASE stock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stock;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    type VARCHAR(50) DEFAULT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    supplied_by VARCHAR(100) DEFAULT NULL,
    warranty VARCHAR(50) DEFAULT NULL,
    rack INT NOT NULL CHECK (rack BETWEEN 1 AND 30),
    cost_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Seed Data — 15 products (matches suppliers stock_id 1..15)
-- quantity = current stock, cost_price = buy price, selling_price = sell price
-- ----------------------------
INSERT INTO products (id, product_name, type, quantity, supplied_by, warranty, rack, cost_price, selling_price) VALUES
(1, 'Brake Pad Set', 'Brakes', 50, 'Rajesh Auto Parts', '1 Year', 5, 450.00, 650.00),
(2, 'Oil Filter', 'Filters', 100, 'Sharma Motors', '6 Months', 2, 120.00, 200.00),
(3, 'Air Filter', 'Filters', 80, 'Rajesh Auto Parts', '6 Months', 3, 250.00, 400.00),
(4, 'Spark Plug Set', 'Ignition', 60, 'Singh Enterprises', '1 Year', 8, 350.00, 550.00),
(5, 'Clutch Plate', 'Transmission', 30, 'Gupta Traders', '2 Years', 12, 800.00, 1200.00),
(6, 'Alternator', 'Electrical', 20, 'Patel Spares Co.', '2 Years', 15, 1500.00, 2200.00),
(7, 'Radiator Hose', 'Cooling', 70, 'Sharma Motors', '1 Year', 7, 300.00, 480.00),
(8, 'Headlight Assembly', 'Lighting', 40, 'Rajesh Auto Parts', '1 Year', 10, 600.00, 950.00),
(9, 'Shock Absorber', 'Suspension', 25, 'Singh Enterprises', '2 Years', 18, 1200.00, 1800.00),
(10, 'Timing Belt', 'Engine', 35, 'Gupta Traders', '1 Year', 6, 450.00, 700.00),
(11, 'Wheel Bearing', 'Wheels', 45, 'Patel Spares Co.', '1 Year', 14, 500.00, 800.00),
(12, 'Battery 12V', 'Electrical', 30, 'Sharma Motors', '3 Years', 20, 1800.00, 2800.00),
(13, 'Fuel Pump', 'Fuel System', 15, 'Rajesh Auto Parts', '2 Years', 22, 1400.00, 2100.00),
(14, 'Rear Brake Drum', 'Brakes', 55, 'Singh Enterprises', '1 Year', 4, 700.00, 1100.00),
(15, 'AC Compressor', 'AC', 10, 'Gupta Traders', '2 Years', 25, 3500.00, 5200.00);
