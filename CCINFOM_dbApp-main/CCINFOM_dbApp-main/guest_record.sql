
DROP DATABASE IF EXISTS homestay_tour_system;
CREATE DATABASE homestay_tour_system;
USE homestay_tour_system;


CREATE TABLE guest (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address VARCHAR(150) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    email_address VARCHAR(100) NOT NULL UNIQUE,
    valid_id_status ENUM('Verified','Not Verified','Pending') NOT NULL,
    trust_rating DECIMAL(2,1) NOT NULL CHECK (trust_rating BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE guest_activity_transaction (
    activity_transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    service_type ENUM('Accommodation','Tour','Combined') NOT NULL,
    reference_id INT NOT NULL,
    activity_start_date DATE NOT NULL,
    activity_end_date DATE NOT NULL,
    base_amount DECIMAL(10,2) NOT NULL,
    additional_charges DECIMAL(10,2) DEFAULT 0,
    discount_applied DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('Cash','GCash','Credit Card','Bank Transfer') NOT NULL,
    payment_confirmation_status ENUM('Paid','Pending','Refunded') NOT NULL,

    CHECK (activity_end_date >= activity_start_date),
    CHECK (final_amount = base_amount + additional_charges - discount_applied),

    FOREIGN KEY (guest_id) REFERENCES guest(guest_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

INSERT INTO guest 
(first_name, last_name, address, contact_number, email_address, valid_id_status, trust_rating)
VALUES
('Emma','Johnson','New York','+1-202-555-0143','emma.johnson@email.com','Verified',4.8),
('Liam','Roberts','Los Angeles','+1-202-555-0178','liam.roberts@email.com','Pending',4.5),
('Sofia','Martínez','Madrid','+34-655-123-456','sofia.martinez@email.es','Verified',4.9),
('Noah','Thompson','London','+44-7700-900123','noah.thompson@email.co.uk','Not Verified',4.2),
('Isabella','Rossi','Rome','+39-347-123-789','isabella.rossi@email.it','Verified',4.7),
('Lucas','Kim','Seoul','+82-10-1234-5678','lucas.kim@email.kr','Pending',4.1),
('Ava','Dubois','Paris','+33-6-12-34-56-78','ava.dubois@email.fr','Verified',4.6),
('Ethan','Müller','Berlin','+49-151-23456789','ethan.mueller@email.de','Verified',4.4),
('Mia','Patel','Mumbai','+91-98765-43210','mia.patel@email.in','Not Verified',4.7),
('Oliver','Smith','Denver','+1-303-555-0199','oliver.smith@email.com','Verified',4.5);


INSERT INTO guest_activity_transaction
(guest_id, service_type, reference_id, activity_start_date, activity_end_date,
 base_amount, additional_charges, discount_applied, final_amount,
 payment_method, payment_confirmation_status)
VALUES

-- 1
(1,'Accommodation',101,'2025-05-01','2025-05-03',
 3000,500,200,3300,'GCash','Paid'),

-- 2
(2,'Tour',202,'2025-05-10','2025-05-10',
 1500,0,0,1500,'Cash','Pending'),

-- 3
(3,'Combined',303,'2025-05-15','2025-05-18',
 5000,800,500,5300,'Credit Card','Paid'),

-- 4
(4,'Accommodation',104,'2025-05-20','2025-05-22',
 2800,200,100,2900,'Bank Transfer','Paid'),

-- 5
(5,'Tour',205,'2025-05-25','2025-05-25',
 1800,150,0,1950,'GCash','Paid'),

-- 6
(6,'Accommodation',106,'2025-05-05','2025-05-07',
 3200,300,400,3100,'Cash','Refunded'),

-- 7
(7,'Combined',307,'2025-05-12','2025-05-14',
 4500,500,250,4750,'Credit Card','Paid'),

-- 8
(8,'Tour',208,'2025-05-28','2025-05-28',
 2000,0,200,1800,'GCash','Paid'),

-- 9
(9,'Accommodation',109,'2025-05-18','2025-05-20',
 3500,400,300,3600,'Bank Transfer','Pending'),

-- 10
(10,'Combined',310,'2025-05-22','2025-05-24',
 4800,600,400,5000,'Credit Card','Paid');

SELECT 
    g.guest_id,
    CONCAT(g.first_name,' ',g.last_name) AS guest_name,
    gat.service_type,
    gat.activity_start_date,
    gat.activity_end_date,
    gat.base_amount,
    gat.additional_charges,
    gat.discount_applied,
    gat.final_amount,
    gat.payment_confirmation_status
FROM guest g
JOIN guest_activity_transaction gat 
    ON g.guest_id = gat.guest_id
WHERE MONTH(gat.activity_start_date) = 5
AND YEAR(gat.activity_start_date) = 2025
ORDER BY gat.activity_start_date DESC;

