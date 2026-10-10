-- ICT Lab Resource Booking System
-- Run: mysql -u root -p < database.sql


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
    category   ENUM('ARDUINO','SENSOR','ELECTRONIC','OTHER') NOT NULL DEFAULT 'OTHER',
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