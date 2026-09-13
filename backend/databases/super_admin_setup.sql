-- =============================================================================
-- AI-Based Student Grievance Management System
-- Super Admin Account Setup
--
-- Usage (run AFTER schema.sql and seed_data.sql):
--   mysql -u root -p grievance_db < databases/super_admin_setup.sql
--
-- Default credentials inserted by this script:
--   Email    : superadmin@grievance.com
--   Password : Admin@1234
--
-- ⚠️  IMPORTANT: Change the password immediately after first login.
--     Use the profile settings page or update it directly via ALTER USER.
--
-- The BCrypt hash below corresponds to: Admin@1234
-- Generated with 10 rounds. You can verify at: https://bcrypt-generator.com
-- =============================================================================

USE grievance_db;

-- -----------------------------------------------------------------------------
-- Step 1 — Insert into users
-- Using INSERT IGNORE so re-running this script is safe (no duplicate error).
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO users
    (email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES
    (
        'superadmin@grievance.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhuG',  -- Admin@1234
        'Super',
        'Admin',
        'SUPER_ADMIN',
        1,
        NOW(),
        NOW()
    );


-- -----------------------------------------------------------------------------
-- Step 2 — Insert into super_admins (links to the user row above)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO super_admins (user_id, phone_number)
SELECT id, NULL
FROM   users
WHERE  email = 'superadmin@grievance.com'
  AND  id NOT IN (SELECT user_id FROM super_admins);


-- -----------------------------------------------------------------------------
-- Confirmation output
-- -----------------------------------------------------------------------------
SELECT
    u.id          AS user_id,
    u.email,
    u.first_name,
    u.last_name,
    u.role,
    u.enabled,
    sa.id         AS super_admin_profile_id
FROM  users        u
JOIN  super_admins sa ON sa.user_id = u.id
WHERE u.email = 'superadmin@grievance.com';


-- =============================================================================
-- How to change the Super Admin password:
--
-- 1. Generate a new BCrypt hash for your chosen password:
--    Java:       new BCryptPasswordEncoder().encode("YourNewPassword")
--    Online:     https://bcrypt-generator.com  (10 rounds)
--
-- 2. Run the following UPDATE (replace the hash):
--    UPDATE users
--    SET    password = '$2a$10$YOUR_NEW_BCRYPT_HASH'
--    WHERE  email    = 'superadmin@grievance.com';
-- =============================================================================
