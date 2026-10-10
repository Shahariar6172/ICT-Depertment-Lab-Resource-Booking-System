-- Optional manual upgrade for an existing database (the updated app also upgrades at startup).
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
