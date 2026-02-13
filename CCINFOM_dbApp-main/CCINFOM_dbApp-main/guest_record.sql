DROP DATABASE IF EXISTS homestay_tour_system;
CREATE DATABASE homestay_tour_system;
USE homestay_tour_system;

CREATE TABLE guest (
    guest_id VARCHAR(2) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address VARCHAR(150) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    email_address VARCHAR(100) UNIQUE NOT NULL,
    valid_id_status ENUM('Verified','Not Verified','Pending') NOT NULL,
    trust_rating DECIMAL(2,1) NOT NULL CHECK (trust_rating BETWEEN 1.0 AND 5.0)
);

CREATE TABLE homestay (
    property_id VARCHAR(2) PRIMARY KEY,
    property_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    address VARCHAR(150) NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL
);

CREATE TABLE guide (
    guide_id VARCHAR(2) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialization VARCHAR(100),
    daily_service_rate DECIMAL(10,2) NOT NULL
);

CREATE TABLE guest_activity_transaction (
    activity_transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id VARCHAR(2) NOT NULL,
    property_id VARCHAR(2) NULL,
    guide_id VARCHAR(2) NULL,
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

    FOREIGN KEY (guest_id) REFERENCES guest(guest_id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES homestay(property_id),
    FOREIGN KEY (guide_id) REFERENCES guide(guide_id)
);

DELIMITER $$

CREATE TRIGGER trg_calculate_final_amount
BEFORE INSERT ON guest_activity_transaction
FOR EACH ROW
BEGIN
    SET NEW.final_amount = NEW.base_amount 
                           + NEW.additional_charges 
                           - NEW.discount_applied;
END$$

DELIMITER ;

INSERT INTO guest VALUES
('G1','Emma','Johnson','Manila, Philippines','+63-917-111-0001','emma.johnson@gmail.com','Verified',4.8),
('G2','Liam','Roberts','Cebu City, Philippines','+63-917-111-0002','liam_roberts@outlook.com','Pending',4.2),
('G3','Sofia','Martinez','Davao City, Philippines','+63-917-111-0003','sofia.martinez@yahoo.com','Verified',5.0),
('G4','Noah','Thompson','Baguio, Philippines','+63-917-111-0004','noah.t@icloud.com','Not Verified',3.5),
('G5','Isabella','Rossi','Makati, Philippines','+63-917-111-0005','isa.rossi@travelmail.com','Verified',4.6),
('G6','Lucas','Kim','Seoul, South Korea','+82-10-1234-5678','lucas.kim@naver.com','Pending',4.1),
('G7','Ava','Dubois','Paris, France','+33-600-123-456','ava.dubois@gmail.com','Verified',4.9),
('G8','Ethan','Muller','Berlin, Germany','+49-151-23456789','ethan.muller@outlook.de','Verified',4.3),
('G9','Mia','Patel','Mumbai, India','+91-98765-43210','mia.patel@yahoo.in','Not Verified',3.2),
('H1','Oliver','Smith','Denver, USA','+1-303-555-1212','oliver.smith@icloud.com','Verified',4.7),
('H2','Daniel','Carter','Iloilo City, Philippines','+63-917-222-0001','daniel.carter@gmail.com','Verified',4.4),
('H3','Hannah','Kim','Busan, South Korea','+82-10-9876-5432','hannah.kim@outlook.kr','Pending',3.9),
('H4','Miguel','Santos','Cagayan de Oro, Philippines','+63-917-222-0003','miguel.santos@yahoo.com','Verified',4.7),
('H5','Chloe','Anderson','Vancouver, Canada','+1-604-555-8899','chloe.anderson@icloud.com','Not Verified',3.6),
('H6','Rafael','Morales','Madrid, Spain','+34-612-345-678','rafael.morales@travelmail.es','Verified',4.8);

INSERT INTO homestay VALUES
('P1','Sea Breeze Villa','Carlos Dela Cruz','Palawan, Philippines',2000),
('P2','Mountain View Cabin','Anna Reyes','Baguio, Philippines',1500),
('P3','City Stay Inn','Mark Tan','Manila, Philippines',1800),
('P4','Island Retreat','Liza Santos','Cebu, Philippines',2500),
('P5','Lake House','Pedro Cruz','Laguna, Philippines',1700),
('P6','Forest Lodge','Maria Lim','Davao, Philippines',1600),
('P7','Sunset Resort','John Lee','Boracay, Philippines',3000),
('P8','Hilltop Haven','Grace Yu','Tagaytay, Philippines',2200),
('P9','Urban Suites','Paul Ong','Quezon City, Philippines',1900),
('Q1','Coastal Escape','Nina Cruz','La Union, Philippines',2100);

INSERT INTO guide VALUES
('D1','Juan','Garcia','City Tour',1500),
('D2','Maria','Lopez','Island Tour',2000),
('D3','Carlos','Mendoza','Mountain Trek',1800),
('D4','Ana','Santos','Historical Tour',1600),
('D5','Pedro','Reyes','Food Tour',1700),
('D6','Liza','Torres','Nature Tour',1900),
('D7','Mark','Villanueva','Adventure Tour',2100),
('D8','Sofia','Cruz','Cultural Tour',1500),
('D9','Luis','Gomez','Beach Tour',1800),
('E1','Elena','Navarro','Photography Tour',2000);

INSERT INTO guest_activity_transaction
(guest_id, property_id, guide_id, service_type, reference_id,
 activity_start_date, activity_end_date,
 base_amount, additional_charges, discount_applied,
 payment_method, payment_confirmation_status)
VALUES
('G1','P1',NULL,'Accommodation',101,'2025-09-05','2025-09-07',4000,500,200,'GCash','Paid'),

('G2',NULL,'D2','Tour',202,'2025-10-10','2025-10-10',2000,0,0,'Cash','Pending'),

('G3','P3','D3','Combined',303,'2025-11-15','2025-11-18',6000,800,500,'Credit Card','Paid'),

('G4','P4',NULL,'Accommodation',104,'2025-12-20','2025-12-22',5000,200,100,'Bank Transfer','Paid'),

('G5',NULL,'D5','Tour',205,'2026-01-12','2026-01-12',1700,150,0,'GCash','Paid'),

('G6','P6',NULL,'Accommodation',106,'2026-02-05','2026-02-07',3200,300,400,'Cash','Refunded'),

('G7','P7','D7','Combined',307,'2026-03-10','2026-03-12',7500,500,250,'Credit Card','Paid'),

('G8',NULL,'D8','Tour',208,'2025-09-25','2025-09-25',1500,0,200,'GCash','Paid'),

('H1','P2',NULL,'Accommodation',109,'2025-10-03','2025-10-05',3000,200,100,'Bank Transfer','Paid'),

('H2',NULL,'D4','Tour',210,'2025-11-08','2025-11-08',1600,0,0,'Cash','Paid');


ALTER TABLE guest 
MODIFY trust_rating DECIMAL(2,1) NOT NULL 
CHECK (trust_rating BETWEEN 1.0 AND 5.0);
