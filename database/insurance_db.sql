CREATE DATABASE IF NOT EXISTS insurance;
USE insurance;

CREATE TABLE IF NOT EXISTS insurance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    driving_license VARCHAR(50) DEFAULT NULL,
    pan_number VARCHAR(20) DEFAULT NULL,
    rc_number VARCHAR(50) DEFAULT NULL,
    insurance_company VARCHAR(100) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    coverage DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    expiry_date DATE NOT NULL
);

INSERT INTO insurance (customer_name, vehicle_number, driving_license, pan_number, rc_number, insurance_company, policy_name, coverage, expiry_date) VALUES
('Amit Shah', 'MH-01-AB-1234', 'DL-1234567890', 'ABCDE1234F', 'RC-1001', 'New India Assurance', 'Comprehensive', 500000.00, '2027-06-15'),
('Sunita Singh', 'MH-02-CD-5678', 'DL-0987654321', 'FGHIJ5678K', 'RC-1002', 'ICICI Lombard', 'Third Party', 250000.00, '2026-12-31'),
('Vikram Patel', 'GJ-03-EF-9012', 'DL-1122334455', 'KLMNO9012P', 'RC-1003', 'Bajaj Allianz', 'Comprehensive', 750000.00, '2027-03-20'),
('Priya Shukla', 'DL-04-GH-3456', 'DL-5566778899', 'PQRST3456R', 'RC-1004', 'HDFC Ergo', 'Zero Dep', 600000.00, '2026-09-10'),
('Rohit Patil', 'MH-05-IJ-7890', 'DL-9988776655', 'UVWXY7890S', 'RC-1005', 'Tata AIG', 'Comprehensive', 800000.00, '2027-11-05'),
('Neha Joshi', 'KA-06-KL-2345', 'DL-4433221100', 'ZABCD2345T', 'RC-1006', 'New India Assurance', 'Third Party', 300000.00, '2026-08-25'),
('Deepak Namey', 'UP-07-MN-6789', 'DL-7766554433', 'EFGHI6789U', 'RC-1007', 'ICICI Lombard', 'Comprehensive', 1000000.00, '2028-01-15'),
('Kavita Reddy', 'TS-08-OP-0123', 'DL-2233445566', 'JKLMN0123V', 'RC-1008', 'Bajaj Allianz', 'Zero Dep', 550000.00, '2027-04-30'),
('Suresh Nair', 'KL-09-QR-4567', 'DL-6677889900', 'OPQRS4567W', 'RC-1009', 'HDFC Ergo', 'Comprehensive', 900000.00, '2027-07-20'),
('Pooja Shinde', 'MH-10-ST-8901', 'DL-3344556677', 'TUVWX8901X', 'RC-1010', 'Tata AIG', 'Third Party', 275000.00, '2026-11-12');
