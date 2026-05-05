-- Seed users
INSERT IGNORE INTO app_users (username, password)
VALUES
    ('admin', 'admin123'),
    ('staff', 'staff123');

-- Seed clinics
INSERT IGNORE INTO clinics (id, name, street, city, state, zip, phone)
VALUES
    ('a1b2c3d4-0000-0000-0000-000000000001', 'Johns Hopkins Community Clinic', '600 N Wolfe St', 'Baltimore', 'MD', '21287', '410-555-0200'),
    ('a1b2c3d4-0000-0000-0000-000000000002', 'Baltimore City Health Center', '1 N Charles St', 'Baltimore', 'MD', '21201', '410-555-0300');

-- Seed patients
INSERT IGNORE INTO patients (id, first_name, last_name, date_of_birth, email, phone, street, city, state, zip)
VALUES
    ('b2c3d4e5-0000-0000-0000-000000000001', 'Jane', 'Doe', '1990-04-15', 'jane.doe@example.com', '410-555-0100', '123 Main St', 'Baltimore', 'MD', '21201'),
    ('b2c3d4e5-0000-0000-0000-000000000002', 'John', 'Smith', '1985-08-22', 'john.smith@example.com', '410-555-0101', '456 Oak Ave', 'Baltimore', 'MD', '21202');

-- Seed vaccinations
INSERT IGNORE INTO vaccinations (id, patient_id, vaccine_type, date_administered, next_due_date, provider, lot_number)
VALUES
    ('c3d4e5f6-0000-0000-0000-000000000001', 'b2c3d4e5-0000-0000-0000-000000000001', 'FLU', '2025-10-01', '2026-10-01', 'Johns Hopkins Community Clinic', 'A1B2C3'),
    ('c3d4e5f6-0000-0000-0000-000000000002', 'b2c3d4e5-0000-0000-0000-000000000001', 'COVID_19', '2025-09-15', '2026-09-15', 'Baltimore City Health Center', 'X9Y8Z7'),
    ('c3d4e5f6-0000-0000-0000-000000000003', 'b2c3d4e5-0000-0000-0000-000000000002', 'FLU', '2025-10-10', '2026-10-10', 'Johns Hopkins Community Clinic', 'D4E5F6');

-- Seed appointments
INSERT IGNORE INTO appointments (id, slot_id, patient_id, clinic_id, date_time, reason, status)
VALUES
    ('d4e5f6a7-0000-0000-0000-000000000001', 'e5f6a7b8-0000-0000-0000-000000000002', 'b2c3d4e5-0000-0000-0000-000000000001', 'a1b2c3d4-0000-0000-0000-000000000001', '2026-05-15T10:00:00', 'Annual flu vaccination', 'SCHEDULED'),
    ('d4e5f6a7-0000-0000-0000-000000000002', 'e5f6a7b8-0000-0000-0000-000000000005', 'b2c3d4e5-0000-0000-0000-000000000002', 'a1b2c3d4-0000-0000-0000-000000000002', '2026-05-20T14:00:00', 'COVID-19 booster', 'SCHEDULED');

-- Seed slots
INSERT IGNORE INTO slots (id, clinic_id, date_time, available)
VALUES
    ('e5f6a7b8-0000-0000-0000-000000000001', 'a1b2c3d4-0000-0000-0000-000000000001', '2026-05-15T09:00:00', true),
    ('e5f6a7b8-0000-0000-0000-000000000002', 'a1b2c3d4-0000-0000-0000-000000000001', '2026-05-15T10:00:00', false),
    ('e5f6a7b8-0000-0000-0000-000000000003', 'a1b2c3d4-0000-0000-0000-000000000001', '2026-05-15T11:00:00', true),
    ('e5f6a7b8-0000-0000-0000-000000000004', 'a1b2c3d4-0000-0000-0000-000000000002', '2026-05-20T13:00:00', true),
    ('e5f6a7b8-0000-0000-0000-000000000005', 'a1b2c3d4-0000-0000-0000-000000000002', '2026-05-20T14:00:00', false);
