-- =============================================================================
-- AI-Based Student Grievance Management System
-- Seed Data — Colleges and Departments
--
-- Usage (run AFTER schema.sql):
--   mysql -u root -p grievance_db < databases/seed_data.sql
--
-- This script uses INSERT IGNORE so it is safe to run multiple times.
-- Existing rows are not modified.
-- =============================================================================

USE grievance_db;

-- =============================================================================
-- COLLEGES
-- =============================================================================
INSERT IGNORE INTO colleges (name, code, address, city, state, email, phone_number, created_at) VALUES
('ABES Engineering College',                        'ABES',    '19th KM Stone, NH-24',                    'Ghaziabad',     'Uttar Pradesh', 'info@abes.ac.in',                          '01207131000',  NOW()),
('KIET Group of Institutions',                      'KIET',    '13-Km Stone, Ghaziabad-Meerut Road',      'Ghaziabad',     'Uttar Pradesh', 'info@kiet.edu',                            '01232275000',  NOW()),
('GL Bajaj Institute of Technology and Management', 'GLBITM',  'Plot No. 2, APJ Abdul Kalam Road',        'Greater Noida', 'Uttar Pradesh', 'info@glbitm.org',                          '01202406000',  NOW()),
('Noida Institute of Engineering and Technology',   'NIET',    '19 KM Stone, Knowledge Park-II',          'Greater Noida', 'Uttar Pradesh', 'info@niet.co.in',                          '01202326100',  NOW()),
('Galgotias University',                            'GU',      'Plot No. 2, Yamuna Expressway',            'Greater Noida', 'Uttar Pradesh', 'admissions@galgotiasuniversity.edu.in',    '01207106000',  NOW()),
('Greater Noida Institute of Technology',           'GNIOT',   'Knowledge Park-II, Greater Noida',        'Greater Noida', 'Uttar Pradesh', 'info@gniot.net',                           '01202395701',  NOW()),
('I.T.S. Engineering College',                      'ITS',     'Knowledge Park-III, Greater Noida',       'Greater Noida', 'Uttar Pradesh', 'info@its.edu.in',                          '01202325167',  NOW()),
('Birla Institute of Management Technology',        'BIMTECH', 'Plot No. 5, Knowledge Park-II',           'Greater Noida', 'Uttar Pradesh', 'info@bimtech.ac.in',                       '01202323001',  NOW()),
('Mangalmay Institute of Engineering and Technology','MIET',   'Greater Noida–Dadri Road',                'Greater Noida', 'Uttar Pradesh', 'info@mangalmay.org',                       '01202323450',  NOW()),
('Accurate Institute of Management and Technology', 'AIMT',    '49 Knowledge Park-III',                   'Greater Noida', 'Uttar Pradesh', 'info@accurate.in',                        '01202320427',  NOW()),
('Lloyd Institute of Engineering and Technology',   'LIET',    'Plot No. 11, Knowledge Park-II',          'Greater Noida', 'Uttar Pradesh', 'info@lloydcollege.in',                    '01202326418',  NOW()),
('United College of Engineering and Research',      'UCER',    'Naini, Prayagraj',                        'Prayagraj',     'Uttar Pradesh', 'info@united.ac.in',                       '05322544567',  NOW()),
('IIMT College of Engineering',                     'IIMT',    'O Pocket, Gamma-I',                       'Greater Noida', 'Uttar Pradesh', 'info@iimtindia.net',                       '01202328600',  NOW()),
('KCC Institute of Technology and Management',      'KCC',     'Greater Noida',                           'Greater Noida', 'Uttar Pradesh', 'info@kccitm.com',                          '01202326418',  NOW()),
('Sharda University',                               'SHARDA',  'Plot No. 32-34, Knowledge Park-III',      'Greater Noida', 'Uttar Pradesh', 'admissions@sharda.ac.in',                 '01206174000',  NOW()),
('JIMS Engineering Management Technical Campus',    'JIMS',    '48/4, Knowledge Park-III',                'Greater Noida', 'Uttar Pradesh', 'info@jimsgn.ac.in',                       '01202323450',  NOW()),
('Harlal Institute of Management and Technology',   'HIMT',    '8 Knowledge Park-I',                      'Greater Noida', 'Uttar Pradesh', 'info@himtcollege.com',                    '01202326100',  NOW());


-- =============================================================================
-- DEPARTMENTS
-- Helper: Get college IDs by code and insert departments
-- =============================================================================

-- ABES Engineering College
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Machine Learning', 'AIML',  id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Data Science',                                 'DS',    id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical and Electronics Engineering',       'EEE',   id FROM colleges WHERE code = 'ABES';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code = 'ABES';

-- KIET Group of Institutions
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Machine Learning', 'AIML',  id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Information Technology',  'CSIT',  id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical and Electronics Engineering',       'EEE',   id FROM colleges WHERE code = 'KIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code = 'KIET';

-- GL Bajaj Institute
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Machine Learning', 'AIML',  id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Data Science',     'AIDS',  id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical Engineering',                       'EE',    id FROM colleges WHERE code = 'GLBITM';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code = 'GLBITM';

-- NIET
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Machine Learning', 'AIML',  id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Data Science',                                 'DS',    id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical Engineering',                       'EE',    id FROM colleges WHERE code = 'NIET';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code = 'NIET';

-- Galgotias University
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence',                      'AI',    id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Data Science',                                 'DS',    id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical Engineering',                       'EE',    id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code = 'GU';
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Civil Engineering',                            'CE',    id FROM colleges WHERE code = 'GU';

-- Remaining colleges — common departments
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Computer Science and Engineering',             'CSE',   id FROM colleges WHERE code IN ('GNIOT','ITS','BIMTECH','MIET','AIMT','LIET','UCER','IIMT','KCC','SHARDA','JIMS','HIMT');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Information Technology',                       'IT',    id FROM colleges WHERE code IN ('GNIOT','ITS','MIET','AIMT','LIET','UCER','IIMT','KCC','SHARDA','JIMS','HIMT');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electronics and Communication Engineering',    'ECE',   id FROM colleges WHERE code IN ('GNIOT','ITS','MIET','AIMT','LIET','UCER','IIMT','KCC','SHARDA','JIMS','HIMT');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Mechanical Engineering',                       'ME',    id FROM colleges WHERE code IN ('GNIOT','ITS','MIET','AIMT','LIET','UCER','IIMT','KCC','SHARDA','JIMS','HIMT');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Electrical Engineering',                       'EE',    id FROM colleges WHERE code IN ('GNIOT','MIET','LIET','UCER','IIMT','SHARDA');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Artificial Intelligence and Machine Learning', 'AIML',  id FROM colleges WHERE code IN ('GNIOT','SHARDA','ITS');
INSERT IGNORE INTO departments (name, code, college_id) SELECT 'Civil Engineering',                            'CE',    id FROM colleges WHERE code IN ('UCER','SHARDA','LIET');

-- =============================================================================
-- End of seed_data.sql
-- Run super_admin_setup.sql next to create the Super Admin account.
-- =============================================================================
