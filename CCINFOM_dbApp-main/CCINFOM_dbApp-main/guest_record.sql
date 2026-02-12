DROP DATABASE IF EXISTS homestay_tour_system;
CREATE DATABASE homestay_tour_system;
USE homestay_tour_system;

CREATE TABLE guest (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    address VARCHAR(150) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    email_address VARCHAR(100) UNIQUE NOT NULL,
    valid_id_status ENUM('Verified','Not Verified','Pending') NOT NULL,
    trust_rating INT NOT NULL CHECK (trust_rating BETWEEN 1 AND 5)
);

CREATE TABLE homestay (
    property_id INT AUTO_INCREMENT PRIMARY KEY,
    property_name VARCHAR(100) NOT NULL,
    host_name VARCHAR(100) NOT NULL,
    address VARCHAR(150) NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL
);

CREATE TABLE guide (
    guide_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialization VARCHAR(100),
    daily_service_rate DECIMAL(10,2) NOT NULL
);

CREATE TABLE guest_activity_transaction (
    activity_transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    property_id INT NULL,
    guide_id INT NULL,
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
    SET NEW.final_amount = NEW.base_amount + NEW.additional_charges - NEW.discount_applied;
END$$
DELIMITER ;

INSERT INTO guest 
(first_name,last_name,address,contact_number,email_address,valid_id_status,trust_rating)
VALUES
('Emma','Johnson','New York','+1-202-555-0143','emma@email.com','Verified',5),
('Liam','Roberts','Los Angeles','+1-202-555-0178','liam@email.com','Pending',4),
('Sofia','Martinez','Madrid','+34-123-456','sofia@email.com','Verified',5),
('Noah','Thompson','London','+44-123-456','noah@email.com','Not Verified',3),
('Isabella','Rossi','Rome','+39-111-222','isa@email.com','Verified',4),
('Lucas','Kim','Seoul','+82-123','lucas@email.com','Pending',4),
('Ava','Dubois','Paris','+33-444','ava@email.com','Verified',5),
('Ethan','Muller','Berlin','+49-555','ethan@email.com','Verified',4),
('Mia','Patel','Mumbai','+91-666','mia@email.com','Not Verified',3),
('Oliver','Smith','Denver','+1-303','oliver@email.com','Verified',5);

INSERT INTO homestay (property_name,host_name,address,price_per_night) VALUES
('Sea Breeze Villa','Carlos Dela Cruz','Palawan',2000),
('Mountain View Cabin','Anna Reyes','Baguio',1500),
('City Stay Inn','Mark Tan','Manila',1800),
('Island Retreat','Liza Santos','Cebu',2500),
('Lake House','Pedro Cruz','Laguna',1700),
('Forest Lodge','Maria Lim','Davao',1600),
('Sunset Resort','John Lee','Boracay',3000),
('Hilltop Haven','Grace Yu','Tagaytay',2200),
('Urban Suites','Paul Ong','Quezon City',1900),
('Coastal Escape','Nina Cruz','La Union',2100);

INSERT INTO guide (first_name,last_name,specialization,daily_service_rate) VALUES
('Juan','Garcia','City Tour',1500),
('Maria','Lopez','Island Tour',2000),
('Carlos','Mendoza','Mountain Trek',1800),
('Ana','Santos','Historical Tour',1600),
('Pedro','Reyes','Food Tour',1700),
('Liza','Torres','Nature Tour',1900),
('Mark','Villanueva','Adventure Tour',2100),
('Sofia','Cruz','Cultural Tour',1500),
('Luis','Gomez','Beach Tour',1800),
('Elena','Navarro','Photography Tour',2000);

INSERT INTO guest_activity_transaction
(guest_id, property_id, guide_id, service_type, reference_id,
 activity_start_date, activity_end_date,
 base_amount, additional_charges, discount_applied,
 payment_method, payment_confirmation_status)
VALUES

-- 1 Accommodation Only (May)
(1, 1, 1, 'Accommodation', 101,'2025-05-01','2025-05-03', 4000, 500, 200,'GCash','Paid'),

-- 2 Tour Only (June)
(2, 1, 1, 'Tour', 202,'2025-06-10','2025-06-10', 2000, 0, 0,'Cash','Pending'),

-- 3 Combined (July)
(3, 3, 3, 'Combined', 303,'2025-07-15','2025-07-18',6000, 800, 500,'Credit Card','Paid'),

-- 4 Accommodation (August)
(4, 4, 1, 'Accommodation', 104,'2025-08-20','2025-08-22',5000, 200, 100,'Bank Transfer','Paid'),

-- 5 Tour (September)
(5, 1, 5, 'Tour', 205,'2025-09-25','2025-09-25',1700, 150, 0,'GCash','Paid'),

-- 6 Accommodation (October)
(6, 6, 1, 'Accommodation', 106,'2025-10-05','2025-10-07',3200, 300, 400,'Cash','Refunded'),

-- 7 Combined (November)
(7, 7, 7, 'Combined', 307,'2025-11-12','2025-11-14',7500, 500, 250,'Credit Card','Paid'),

-- 8 Tour (December)
(8, 1, 8, 'Tour', 208,'2025-12-28','2025-12-28',1500, 0, 200,'GCash','Paid');


