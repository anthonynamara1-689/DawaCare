

DROP DATABASE IF EXISTS medical_adherence_db;

CREATE DATABASE medical_adherence_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE medical_adherence_db;

CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,          -- BCrypt/PBKDF2, never plain text
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(120) UNIQUE,
    phone          VARCHAR(20),
    role   ENUM('ADMIN','DOCTOR','PATIENT','CAREGIVER') NOT NULL,
    status ENUM('PENDING','ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at  DATETIME NULL
);


CREATE TABLE patients (
    patient_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL UNIQUE,
    date_of_birth DATE,
    gender ENUM('MALE','FEMALE','OTHER'),
    address      VARCHAR(255),
    emergency_contact_name  VARCHAR(120),
    emergency_contact_phone VARCHAR(20),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE doctors (
    doctor_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL UNIQUE,
    specialization VARCHAR(120),
    license_number VARCHAR(80) NOT NULL UNIQUE,
    hospital_name  VARCHAR(150),
    verified_by    INT NULL,                      
    verified_at    DATETIME NULL,
    FOREIGN KEY (user_id)     REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (verified_by) REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE caregivers (
    caregiver_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);


CREATE TABLE doctor_patients (
    doctor_patient_id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id   INT NOT NULL,
    patient_id  INT NOT NULL,
    status ENUM('PENDING','ACTIVE','ENDED') NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)   ON DELETE RESTRICT,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE RESTRICT,
    UNIQUE (doctor_id, patient_id)
);

CREATE TABLE caregiver_patients (
    caregiver_patient_id INT AUTO_INCREMENT PRIMARY KEY,
    caregiver_id  INT NOT NULL,
    patient_id    INT NOT NULL,
    relationship_to_patient VARCHAR(100),
    status ENUM('PENDING','ACTIVE','REVOKED') NOT NULL DEFAULT 'PENDING',
    requested_by ENUM('PATIENT','CAREGIVER') NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at  DATETIME NULL,
    FOREIGN KEY (caregiver_id) REFERENCES caregivers(caregiver_id) ON DELETE RESTRICT,
    FOREIGN KEY (patient_id)   REFERENCES patients(patient_id)     ON DELETE RESTRICT,
    UNIQUE (caregiver_id, patient_id)
);


CREATE TABLE medical_conditions (
    condition_id   INT AUTO_INCREMENT PRIMARY KEY,
    condition_name VARCHAR(150) NOT NULL UNIQUE,
    description    TEXT
);

CREATE TABLE patient_conditions (
    patient_condition_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id     INT NOT NULL,
    condition_id   INT NOT NULL,
    diagnosed_by   INT NULL,                      -- doctor_id
    diagnosed_date DATE,
    notes          TEXT,
    FOREIGN KEY (patient_id)   REFERENCES patients(patient_id)           ON DELETE RESTRICT,
    FOREIGN KEY (condition_id) REFERENCES medical_conditions(condition_id) ON DELETE RESTRICT,
    FOREIGN KEY (diagnosed_by) REFERENCES doctors(doctor_id)             ON DELETE RESTRICT,
    UNIQUE (patient_id, condition_id)
);


CREATE TABLE medication_categories (
    category_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT
);

CREATE TABLE medications (
    medication_id   INT AUTO_INCREMENT PRIMARY KEY,
    category_id     INT NULL,
    medication_name VARCHAR(150) NOT NULL,
    generic_name    VARCHAR(150),
    dosage_form     VARCHAR(50) NOT NULL,        
    strength        VARCHAR(50) NOT NULL,         
    manufacturer    VARCHAR(150),
    description     TEXT,
    FOREIGN KEY (category_id) REFERENCES medication_categories(category_id) ON DELETE SET NULL,
    UNIQUE (medication_name, strength, dosage_form)
);


CREATE TABLE prescriptions (
    prescription_id   INT AUTO_INCREMENT PRIMARY KEY,
    patient_id        INT NOT NULL,
    doctor_id         INT NOT NULL,
    prescription_date DATE NOT NULL,
    diagnosis         VARCHAR(255),
    notes             TEXT,
    status ENUM('ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE RESTRICT,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)   ON DELETE RESTRICT
);

-- Dates + interval live HERE only (no duplicated duration/frequency text)
CREATE TABLE prescription_items (
    prescription_item_id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id INT NOT NULL,
    medication_id   INT NOT NULL,
    dosage_amount   DECIMAL(10,2) NOT NULL,
    dosage_unit     VARCHAR(30)   NOT NULL,      
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    interval_days   TINYINT NOT NULL DEFAULT 1,  
    instructions    TEXT,                         
    status ENUM('ACTIVE','DISCONTINUED','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    discontinued_at DATETIME NULL,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(prescription_id) ON DELETE RESTRICT,
    FOREIGN KEY (medication_id)   REFERENCES medications(medication_id)     ON DELETE RESTRICT,
    CHECK (dosage_amount > 0),
    CHECK (end_date >= start_date),
    CHECK (interval_days >= 1)
);


CREATE TABLE dose_times (
    dose_time_id         INT AUTO_INCREMENT PRIMARY KEY,
    prescription_item_id INT NOT NULL,
    dose_time            TIME NOT NULL,
    FOREIGN KEY (prescription_item_id)
        REFERENCES prescription_items(prescription_item_id) ON DELETE CASCADE,
    UNIQUE (prescription_item_id, dose_time)
);


CREATE TABLE dose_records (
    dose_id              INT AUTO_INCREMENT PRIMARY KEY,
    prescription_item_id INT NOT NULL,
    scheduled_at         DATETIME NOT NULL,      
    status ENUM('PENDING','TAKEN','SKIPPED','MISSED','CANCELLED')
           NOT NULL DEFAULT 'PENDING',
    taken_at     DATETIME NULL,
    skip_reason  VARCHAR(255),
    recorded_by  INT NULL,                        
    recorded_at  DATETIME NULL,
    notes        TEXT,
    FOREIGN KEY (prescription_item_id)
        REFERENCES prescription_items(prescription_item_id) ON DELETE RESTRICT,
    FOREIGN KEY (recorded_by) REFERENCES users(user_id) ON DELETE RESTRICT,
    UNIQUE (prescription_item_id, scheduled_at),
    INDEX idx_dose_status_time (status, scheduled_at),
    CHECK (status <> 'TAKEN' OR taken_at IS NOT NULL)
);


CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id   INT NOT NULL,                       
    type ENUM('DOSE_REMINDER','MISSED_DOSE','APPOINTMENT',
              'LINK_REQUEST','SYSTEM') NOT NULL,
    message   VARCHAR(255) NOT NULL,
    dose_id   INT NULL,
    is_read   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)        ON DELETE RESTRICT,
    FOREIGN KEY (dose_id) REFERENCES dose_records(dose_id) ON DELETE RESTRICT,
    UNIQUE (user_id, type, dose_id)                   
);


CREATE TABLE appointments (
    appointment_id   INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT NOT NULL,
    doctor_id        INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    reason           VARCHAR(255),
    status ENUM('SCHEDULED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    notes            TEXT,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE RESTRICT,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)   ON DELETE RESTRICT,
    INDEX idx_doctor_day (doctor_id, appointment_date)
);


CREATE TABLE audit_log (
    log_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NULL,
    action     VARCHAR(50) NOT NULL,             
    entity     VARCHAR(50) NOT NULL,             
    entity_id  INT,
    details    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT
);




CREATE VIEW v_dose_details AS
SELECT d.dose_id,
       d.scheduled_at,
       d.status,
       d.taken_at,
       d.recorded_by,
       pi.prescription_item_id,
       pi.dosage_amount,
       pi.dosage_unit,
       pi.instructions,
       m.medication_name,
       m.strength,
       p.prescription_id,
       p.patient_id,
       p.doctor_id
FROM dose_records d
JOIN prescription_items pi ON pi.prescription_item_id = d.prescription_item_id
JOIN prescriptions p       ON p.prescription_id       = pi.prescription_id
JOIN medications m         ON m.medication_id         = pi.medication_id;


CREATE VIEW v_adherence_by_item AS
SELECT patient_id,
       prescription_item_id,
       medication_name,
       SUM(status = 'TAKEN')   AS taken,
       SUM(status = 'MISSED')  AS missed,
       SUM(status = 'SKIPPED') AS skipped,
       ROUND(100 * SUM(status = 'TAKEN')
             / NULLIF(SUM(status IN ('TAKEN','MISSED','SKIPPED')), 0), 2)
             AS adherence_pct
FROM v_dose_details
GROUP BY patient_id, prescription_item_id, medication_name;


-- Default admin login: admin / Admin@123
-- CHANGE THIS PASSWORD after first login (Change Password button).
INSERT INTO users (username, password_hash, full_name, role, status)
VALUES ('admin', '$2a$12$x/pCOW95FQpx4jrg7sKMkOCzhZZjRURG9/bi8lcrbMhLxwMPjKKyi',
        'System Administrator', 'ADMIN', 'ACTIVE');
