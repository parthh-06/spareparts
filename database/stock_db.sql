CREATE DATABASE IF NOT EXISTS stock;
USE stock;

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    type VARCHAR(50) DEFAULT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    supplied_by VARCHAR(100) DEFAULT NULL,
    warranty VARCHAR(50) DEFAULT NULL,
    rack INT NOT NULL CHECK (rack BETWEEN 1 AND 30),
    cost_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);

INSERT INTO products (product_name, type, quantity, supplied_by, warranty, rack, cost_price, selling_price) VALUES
('Brake Pad Set', 'Brakes', 50, 'Rajesh Auto Parts', '1 Year', 5, 450.00, 650.00),
('Oil Filter', 'Filters', 100, 'Sharma Motors', '6 Months', 2, 120.00, 200.00),
('Air Filter', 'Filters', 80, 'Rajesh Auto Parts', '6 Months', 3, 250.00, 400.00),
('Spark Plug Set', 'Ignition', 60, 'Singh Enterprises', '1 Year', 8, 350.00, 550.00),
('Clutch Plate', 'Transmission', 30, 'Gupta Traders', '2 Years', 12, 800.00, 1200.00),
('Alternator', 'Electrical', 20, 'Patel Spares Co.', '2 Years', 15, 1500.00, 2200.00),
('Radiator Hose', 'Cooling', 70, 'Sharma Motors', '1 Year', 7, 300.00, 480.00),
('Headlight Assembly', 'Lighting', 40, 'Rajesh Auto Parts', '1 Year', 10, 600.00, 950.00),
('Shock Absorber', 'Suspension', 25, 'Singh Enterprises', '2 Years', 18, 1200.00, 1800.00),
('Timing Belt', 'Engine', 35, 'Gupta Traders', '1 Year', 6, 450.00, 700.00),
('Wheel Bearing', 'Wheels', 45, 'Patel Spares Co.', '1 Year', 14, 500.00, 800.00),
('Battery 12V', 'Electrical', 30, 'Sharma Motors', '3 Years', 20, 1800.00, 2800.00),
('Fuel Pump', 'Fuel System', 15, 'Rajesh Auto Parts', '2 Years', 22, 1400.00, 2100.00),
('Rear Brake Drum', 'Brakes', 55, 'Singh Enterprises', '1 Year', 4, 700.00, 1100.00),
('AC Compressor', 'AC', 10, 'Gupta Traders', '2 Years', 25, 3500.00, 5200.00);
