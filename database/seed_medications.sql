-- ============================================================
-- DawaCare - medication catalogue seed data
-- Run AFTER schema.sql. Safe to run more than once.
-- ============================================================

USE medical_adherence_db;

INSERT IGNORE INTO medication_categories (category_name, description) VALUES
('Antihypertensives', 'Blood pressure medicines'),
('Antidiabetics', 'Blood sugar medicines'),
('Antibiotics', 'Bacterial infection medicines'),
('Antimalarials', 'Malaria treatment'),
('Antiretrovirals', 'HIV treatment'),
('Analgesics', 'Pain and fever relief'),
('Lipid-lowering', 'Cholesterol medicines');

SET @htn  = (SELECT category_id FROM medication_categories WHERE category_name = 'Antihypertensives');
SET @dm   = (SELECT category_id FROM medication_categories WHERE category_name = 'Antidiabetics');
SET @abx  = (SELECT category_id FROM medication_categories WHERE category_name = 'Antibiotics');
SET @mal  = (SELECT category_id FROM medication_categories WHERE category_name = 'Antimalarials');
SET @arv  = (SELECT category_id FROM medication_categories WHERE category_name = 'Antiretrovirals');
SET @pain = (SELECT category_id FROM medication_categories WHERE category_name = 'Analgesics');
SET @lip  = (SELECT category_id FROM medication_categories WHERE category_name = 'Lipid-lowering');

INSERT IGNORE INTO medications (category_id, medication_name, generic_name, dosage_form, strength) VALUES
(@htn,  'Amlodipine', 'Amlodipine besylate', 'Tablet', '5mg'),
(@htn,  'Losartan', 'Losartan potassium', 'Tablet', '50mg'),
(@htn,  'Hydrochlorothiazide', 'Hydrochlorothiazide', 'Tablet', '25mg'),
(@dm,   'Metformin', 'Metformin hydrochloride', 'Tablet', '500mg'),
(@dm,   'Glibenclamide', 'Glibenclamide', 'Tablet', '5mg'),
(@abx,  'Amoxicillin', 'Amoxicillin', 'Capsule', '500mg'),
(@abx,  'Ciprofloxacin', 'Ciprofloxacin', 'Tablet', '500mg'),
(@abx,  'Cotrimoxazole', 'Sulfamethoxazole/Trimethoprim', 'Tablet', '960mg'),
(@mal,  'Coartem', 'Artemether/Lumefantrine', 'Tablet', '20mg/120mg'),
(@arv,  'TLD', 'Tenofovir/Lamivudine/Dolutegravir', 'Tablet', '300mg/300mg/50mg'),
(@pain, 'Paracetamol', 'Paracetamol', 'Tablet', '500mg'),
(@lip,  'Atorvastatin', 'Atorvastatin calcium', 'Tablet', '20mg');
