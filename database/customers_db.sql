-- ============================================================
-- customers_db.sql  -  Dump for `customers` database
-- Restore: mysql -u root -proot < database/customers_db.sql
-- ============================================================

DROP DATABASE IF EXISTS customers;
CREATE DATABASE customers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE customers;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS bill_items;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS customer_vehicles;
DROP TABLE IF EXISTS customer;

CREATE TABLE customer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
    UNIQUE KEY uq_customer_vehicle (customer_id, vehicle_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bill_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- Seed: 5 customers, 6 bills, 11 items + vehicles
INSERT INTO customer (id, name, phone) VALUES
(1, 'Amit Shah', '9876500001'),
(2, 'Sunita Singh', '9876500002'),
(3, 'Rohan Mehta', '9876500003'),
(4, 'Priya Sharma', '9876500004'),
(5, 'Vikram Patel', '9876500005');

INSERT INTO customer_vehicles (id, customer_id, vehicle_number) VALUES
(1, 1, 'MH-12-AB-1234'),
(2, 1, 'MH-12-CD-5678'),
(3, 2, 'DL-08-CA-4567'),
(4, 3, 'GJ-01-RT-2345'),
(5, 3, 'GJ-01-RT-9999'),
(6, 3, 'GJ-05-MN-1111'),
(7, 4, 'KA-05-MJ-6789'),
(8, 5, 'TN-10-XY-2222');

INSERT INTO bills (bill_id, customer_id, bill_date, paid) VALUES
(1, 1, '2026-07-10 11:00:00', FALSE),
(2, 2, '2026-07-12 14:30:00', TRUE),
(3, 1, '2026-07-15 09:45:00', FALSE),
(4, 3, '2026-07-18 16:20:00', FALSE),
(5, 4, '2026-07-20 10:00:00', TRUE),
(6, 5, '2026-07-22 12:15:00', FALSE);

INSERT INTO bill_items (id, bill_id, item_name, quantity, price) VALUES
(1, 1, 'Brake Pad Set', 2, 650.00),
(2, 1, 'Oil Filter', 1, 200.00),
(3, 2, 'Air Filter', 1, 400.00),
(4, 2, 'Spark Plug Set', 2, 550.00),
(5, 3, 'Clutch Plate', 1, 1200.00),
(6, 4, 'Oil Filter', 4, 200.00),
(7, 4, 'Radiator Hose', 2, 480.00),
(8, 5, 'Battery 12V', 1, 2800.00),
(9, 5, 'Headlight Assembly', 2, 950.00),
(10, 6, 'Wheel Bearing', 2, 800.00),
(11, 6, 'Timing Belt', 1, 700.00);
