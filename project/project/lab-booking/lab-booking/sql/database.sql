-- ICT Lab Resource Booking System
-- Run: mysql -u root -p < database.sql

CREATE DATABASE IF NOT EXISTS lab_booking CHARACTER SET utf8mb4;
USE lab_booking;

CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    student_id    VARCHAR(20) UNIQUE,          -- NULL for admins
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,           -- SHA-256 hex
    role          ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE devices (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    serial_no  VARCHAR(50) NOT NULL UNIQUE,
    status     ENUM('AVAILABLE','BOOKED','ISSUED','DAMAGED') NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE bookings (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    device_id    INT NOT NULL,
    request_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date     DATE NOT NULL,
    issue_date   DATETIME NULL,
    return_date  DATETIME NULL,
    purpose      VARCHAR(255),
    status       ENUM('PENDING','APPROVED','REJECTED','ISSUED','RETURNED') NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (user_id)   REFERENCES users(id),
    FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE TABLE penalties (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    type       ENUM('LATE','DAMAGE') NOT NULL,
    amount     DECIMAL(8,2) NOT NULL,
    paid       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

-- Seed admin (email: admin@mbstu.ac.bd, password: admin123)
INSERT INTO users (name, student_id, email, password_hash, role) VALUES
('Lab Assistant', NULL, 'admin@mbstu.ac.bd',
 '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN');

-- Seed devices
INSERT INTO devices (name, category, serial_no) VALUES
('Arduino Uno R3',        'ARDUINO',    'ARD-UNO-001'),
('Arduino Uno R3',        'ARDUINO',    'ARD-UNO-002'),
('Arduino Mega 2560',     'ARDUINO',    'ARD-MEGA-001'),
('DHT11 Temp Sensor',     'SENSOR',     'SEN-DHT-001'),
('HC-SR04 Ultrasonic',    'SENSOR',     'SEN-USND-001'),
('PIR Motion Sensor',     'SENSOR',     'SEN-PIR-001'),
('Breadboard 830pt',      'ELECTRONIC', 'ELC-BB-001'),
('Digital Multimeter',    'ELECTRONIC', 'ELC-DMM-001'),
('Soldering Iron 60W',    'ELECTRONIC', 'ELC-SLD-001'),
('Raspberry Pi 4 (4GB)',  'OTHER',      'OTH-RPI-001');

-- Student material catalog and multi-item requests
-- Run once on an existing lab_booking database before starting the updated app.
USE lab_booking;

-- Accept both the original categories and those used by the existing admin panel.
ALTER TABLE devices MODIFY category VARCHAR(30) NOT NULL DEFAULT 'OTHER';

CREATE TABLE IF NOT EXISTS lab_subjects (
    name VARCHAR(100) PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS material_variants (
    device_name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    material_name VARCHAR(100) NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (device_name, category)
);
CREATE TABLE IF NOT EXISTS subject_materials (
    subject_name VARCHAR(100) NOT NULL,
    material_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (subject_name, material_name),
    FOREIGN KEY (subject_name) REFERENCES lab_subjects(name)
);
CREATE TABLE IF NOT EXISTS request_batches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    request_code VARCHAR(36) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    subject_name VARCHAR(100) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    due_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Add column only when absent, so the migration is safe to run again.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='bookings' AND column_name='request_batch_id')=0,
    'ALTER TABLE bookings ADD COLUMN request_batch_id INT NULL, ADD CONSTRAINT fk_booking_batch FOREIGN KEY (request_batch_id) REFERENCES request_batches(id)',
    'SELECT 1');
PREPARE upgrade_stmt FROM @ddl;
EXECUTE upgrade_stmt;
DEALLOCATE PREPARE upgrade_stmt;

INSERT IGNORE INTO lab_subjects VALUES ('EEE Lab'), ('IoT Project'), ('Robotics Lab');
-- These are catalog labels only: they do NOT create available physical units.
-- Add actual stock using the existing admin panel with the matching device name.
INSERT IGNORE INTO material_variants VALUES
('Breadboard 830pt', 'ELECTRONIC', 'Breadboard', '830-point'),
('Breadboard 1 Part', 'BOARD', 'Breadboard', '1 part'),
('Breadboard 2 Part', 'BOARD', 'Breadboard', '2 parts'),
('Breadboard 3 Part', 'BOARD', 'Breadboard', '3 parts'),
('Arduino Uno R3', 'ARDUINO', 'Arduino', 'Uno R3'),
('Arduino Uno R3', 'BOARD', 'Arduino', 'Uno R3'),
('Arduino Mega 2560', 'ARDUINO', 'Arduino', 'Mega 2560'),
('Arduino Mega 2560', 'BOARD', 'Arduino', 'Mega 2560'),
('DHT11 Temp Sensor', 'SENSOR', 'Sensor', 'DHT11 temperature'),
('HC-SR04 Ultrasonic', 'SENSOR', 'Sensor', 'HC-SR04 ultrasonic'),
('PIR Motion Sensor', 'SENSOR', 'Sensor', 'PIR motion');
INSERT IGNORE INTO subject_materials VALUES
('EEE Lab', 'Breadboard'), ('EEE Lab', 'Sensor'), ('EEE Lab', 'Arduino'),
('EEE Lab', 'Digital Multimeter'), ('EEE Lab', 'Soldering Iron 60W'),
('IoT Project', 'Breadboard'), ('IoT Project', 'Sensor'), ('IoT Project', 'Arduino'),
('IoT Project', 'Raspberry Pi 4 (4GB)'),
('Robotics Lab', 'Breadboard'), ('Robotics Lab', 'Sensor'), ('Robotics Lab', 'Arduino');
