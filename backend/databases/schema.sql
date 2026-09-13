-- =============================================================================
-- AI-Based Student Grievance Management System
-- Full Database Schema — grievance_db
--
-- Engine  : MySQL 8.0+
-- Charset : utf8mb4
-- Collate : utf8mb4_0900_ai_ci
--
-- Usage:
--   mysql -u root -p < databases/schema.sql
--
-- This script:
--   1. Creates the database (if it does not exist)
--   2. Drops all existing tables in dependency-safe order
--   3. Re-creates all 14 tables with constraints and indexes
-- =============================================================================

CREATE DATABASE IF NOT EXISTS grievance_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE grievance_db;

-- -----------------------------------------------------------------------------
-- Disable FK checks while dropping / recreating tables
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS incident_confirmations;
DROP TABLE IF EXISTS incidents;
DROP TABLE IF EXISTS ai_analyses;
DROP TABLE IF EXISTS risk_assessments;
DROP TABLE IF EXISTS evidences;
DROP TABLE IF EXISTS grievances;
DROP TABLE IF EXISTS registration_tokens;
DROP TABLE IF EXISTS college_admins;
DROP TABLE IF EXISTS super_admins;
DROP TABLE IF EXISTS faculty;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS colleges;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- TABLE 1 — users
-- Central auth table. Every person in the system (student, faculty, admin,
-- super-admin) has exactly one row here. Spring Security authenticates
-- against email + password (BCrypt).
-- =============================================================================
CREATE TABLE users (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255)  NOT NULL,
    password   VARCHAR(255)  NOT NULL,               -- BCrypt hash
    first_name VARCHAR(255)  NOT NULL,
    last_name  VARCHAR(255)  NOT NULL,
    role       ENUM(
                   'STUDENT',
                   'FACULTY',
                   'COLLEGE_ADMIN',
                   'SUPER_ADMIN'
               )             NOT NULL,
    enabled    BIT(1)        NOT NULL DEFAULT 1,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 2 — colleges
-- Every college registered in the system.
-- =============================================================================
CREATE TABLE colleges (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255)  NOT NULL,
    code         VARCHAR(255)  NOT NULL,              -- short identifier, e.g. "ABES"
    address      VARCHAR(255)  NOT NULL,
    city         VARCHAR(255)  NOT NULL,
    state        VARCHAR(255)  NOT NULL,
    email        VARCHAR(255)  NOT NULL,
    phone_number VARCHAR(255)  NOT NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_colleges_name (name),
    UNIQUE KEY uq_colleges_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 3 — departments
-- A department belongs to one college. (college_id, code) is unique so the
-- same dept code can exist in different colleges.
-- =============================================================================
CREATE TABLE departments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255)  NOT NULL,
    code       VARCHAR(255)  NOT NULL,
    college_id BIGINT        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_dept_college_code (college_id, code),
    CONSTRAINT fk_dept_college
        FOREIGN KEY (college_id) REFERENCES colleges (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 4 — students
-- Profile extension of users where role = 'STUDENT'.
-- One-to-one with users; many-to-one with colleges and departments.
-- =============================================================================
CREATE TABLE students (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    enrollment_number VARCHAR(255)  NOT NULL,
    phone_number      VARCHAR(255)  NOT NULL,
    college_id        BIGINT        NOT NULL,
    department_id     BIGINT        NOT NULL,
    year              VARCHAR(255)  NOT NULL,         -- "1st", "2nd", "3rd", "4th"
    section           VARCHAR(255)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_students_user   (user_id),
    UNIQUE KEY uq_students_enroll (enrollment_number),
    CONSTRAINT fk_student_user
        FOREIGN KEY (user_id)       REFERENCES users       (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_college
        FOREIGN KEY (college_id)    REFERENCES colleges    (id),
    CONSTRAINT fk_student_dept
        FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 5 — faculty
-- Profile extension of users where role = 'FACULTY'.
-- is_grievance_resolver = 1 means the faculty member can be assigned grievances.
-- =============================================================================
CREATE TABLE faculty (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    user_id               BIGINT        NOT NULL,
    employee_id           VARCHAR(255)  NOT NULL,
    designation           VARCHAR(255)  NOT NULL,
    phone_number          VARCHAR(255)  NOT NULL,
    college_id            BIGINT        NOT NULL,
    department_id         BIGINT        NOT NULL,
    is_grievance_resolver BIT(1)        NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_faculty_user     (user_id),
    UNIQUE KEY uq_faculty_emp_id   (employee_id),
    CONSTRAINT fk_faculty_user
        FOREIGN KEY (user_id)       REFERENCES users       (id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_college
        FOREIGN KEY (college_id)    REFERENCES colleges    (id),
    CONSTRAINT fk_faculty_dept
        FOREIGN KEY (department_id) REFERENCES departments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 6 — college_admins
-- Profile extension of users where role = 'COLLEGE_ADMIN'.
-- =============================================================================
CREATE TABLE college_admins (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    phone_number VARCHAR(255)  NOT NULL,
    college_id   BIGINT        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_college_admin_user (user_id),
    CONSTRAINT fk_college_admin_user
        FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE,
    CONSTRAINT fk_college_admin_college
        FOREIGN KEY (college_id) REFERENCES colleges (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 7 — super_admins
-- Profile extension of users where role = 'SUPER_ADMIN'.
-- No college association — system-wide authority.
-- =============================================================================
CREATE TABLE super_admins (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    phone_number VARCHAR(255),

    PRIMARY KEY (id),
    UNIQUE KEY uq_super_admin_user (user_id),
    CONSTRAINT fk_super_admin_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 8 — registration_tokens
-- Single-use, role-scoped tokens generated by the Super Admin.
-- Required for any new user registration. Expires after 7 days.
-- college_id is NULL for SUPER_ADMIN tokens.
-- =============================================================================
CREATE TABLE registration_tokens (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    token       VARCHAR(255)  NOT NULL,
    target_role ENUM(
                    'STUDENT',
                    'FACULTY',
                    'COLLEGE_ADMIN',
                    'SUPER_ADMIN'
                )             NOT NULL,
    college_id  BIGINT,                               -- NULL for SUPER_ADMIN
    used        BIT(1)        NOT NULL DEFAULT 0,
    expires_at  DATETIME(6)   NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    used_at     DATETIME(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_reg_token (token),
    CONSTRAINT fk_reg_token_college
        FOREIGN KEY (college_id) REFERENCES colleges (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 9 — grievances
-- Core table. Every grievance submitted by a student.
-- Linked to college, optionally to department and assigned faculty.
-- is_duplicate / duplicate_of_id are set by the AI microservice.
-- =============================================================================
CREATE TABLE grievances (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    title              VARCHAR(255)  NOT NULL,
    description        TEXT          NOT NULL,
    category           ENUM(
                           'ACADEMIC',
                           'INFRASTRUCTURE',
                           'HARASSMENT',
                           'FINANCIAL',
                           'ADMINISTRATIVE',
                           'HOSTEL',
                           'TRANSPORTATION',
                           'LIBRARY',
                           'LABORATORY',
                           'OTHER'
                       )             NOT NULL,
    type               ENUM(
                           'INDIVIDUAL',
                           'GROUP',
                           'ANONYMOUS'
                       )             NOT NULL,
    priority           ENUM(
                           'LOW',
                           'MEDIUM',
                           'HIGH',
                           'CRITICAL'
                       )             NOT NULL DEFAULT 'MEDIUM',
    status             ENUM(
                           'SUBMITTED',
                           'UNDER_REVIEW',
                           'ASSIGNED',
                           'IN_PROGRESS',
                           'RESOLVED',
                           'REJECTED',
                           'CLOSED',
                           'ESCALATED'
                       )             NOT NULL DEFAULT 'SUBMITTED',
    student_id         BIGINT        NOT NULL,
    college_id         BIGINT        NOT NULL,
    department_id      BIGINT,
    assigned_faculty_id BIGINT,
    is_duplicate       BIT(1)        NOT NULL DEFAULT 0,
    duplicate_of_id    BIGINT,
    resolution_note    VARCHAR(255),
    submitted_at       DATETIME(6)   NOT NULL,
    resolved_at        DATETIME(6),
    updated_at         DATETIME(6),

    PRIMARY KEY (id),
    KEY idx_grievance_student  (student_id),
    KEY idx_grievance_college  (college_id),
    KEY idx_grievance_status   (status),
    KEY idx_grievance_category (category),
    KEY idx_grievance_faculty  (assigned_faculty_id),
    CONSTRAINT fk_grievance_student
        FOREIGN KEY (student_id)          REFERENCES students (id),
    CONSTRAINT fk_grievance_college
        FOREIGN KEY (college_id)          REFERENCES colleges (id),
    CONSTRAINT fk_grievance_dept
        FOREIGN KEY (department_id)       REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_grievance_faculty
        FOREIGN KEY (assigned_faculty_id) REFERENCES faculty (id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 10 — ai_analyses
-- One-to-one with grievances. Populated asynchronously by the AI microservice
-- after a grievance is submitted. NULL until analysis completes.
-- =============================================================================
CREATE TABLE ai_analyses (
    id                       BIGINT  NOT NULL AUTO_INCREMENT,
    grievance_id             BIGINT  NOT NULL,
    predicted_category       ENUM(
                                 'ACADEMIC','INFRASTRUCTURE','HARASSMENT',
                                 'FINANCIAL','ADMINISTRATIVE','HOSTEL',
                                 'TRANSPORTATION','LIBRARY','LABORATORY','OTHER'
                             ),
    category_confidence      DOUBLE,
    predicted_priority       ENUM('LOW','MEDIUM','HIGH','CRITICAL'),
    priority_confidence      DOUBLE,
    is_fake_complaint        BIT(1),
    fake_confidence          DOUBLE,
    is_duplicate             BIT(1),
    duplicate_of_id          BIGINT,
    duplicate_similarity_score DOUBLE,
    is_anomaly               BIT(1),
    anomaly_score            DOUBLE,
    analysis_notes           TEXT,
    analyzed_at              DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_ai_grievance (grievance_id),
    CONSTRAINT fk_ai_grievance
        FOREIGN KEY (grievance_id) REFERENCES grievances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 11 — risk_assessments
-- One-to-one with grievances. Created by RiskAssessmentService immediately
-- after AI analysis. Drives the "requiresImmediateAction" flag.
-- =============================================================================
CREATE TABLE risk_assessments (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    grievance_id            BIGINT NOT NULL,
    risk_level              ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    risk_score              DOUBLE,
    risk_factors            TEXT,
    recommendations         TEXT,
    requires_immediate_action BIT(1),
    assessed_at             DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_risk_grievance (grievance_id),
    CONSTRAINT fk_risk_grievance
        FOREIGN KEY (grievance_id) REFERENCES grievances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 12 — evidences
-- Files uploaded as supporting evidence for a grievance.
-- Many-to-one with grievances (a grievance can have multiple files).
-- Files are stored at: uploads/grievances/{grievanceId}/{uuid_filename}
-- =============================================================================
CREATE TABLE evidences (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    grievance_id BIGINT        NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,              -- original filename
    file_path    VARCHAR(255)  NOT NULL,              -- server-side path
    file_type    VARCHAR(255)  NOT NULL,              -- MIME type
    file_size    BIGINT,                              -- bytes
    uploaded_at  DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    KEY idx_evidence_grievance (grievance_id),
    CONSTRAINT fk_evidence_grievance
        FOREIGN KEY (grievance_id) REFERENCES grievances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 13 — incidents
-- An incident is a structured report of what happened, attached to a grievance.
-- Faculty or admins create incidents. Students can confirm them.
-- =============================================================================
CREATE TABLE incidents (
    id                   BIGINT  NOT NULL AUTO_INCREMENT,
    grievance_id         BIGINT  NOT NULL,
    incident_description TEXT    NOT NULL,
    incident_date        DATETIME(6) NOT NULL,
    verification_status  ENUM(
                             'PENDING',
                             'VERIFIED',
                             'DISPUTED',
                             'UNVERIFIED'
                         )       NOT NULL DEFAULT 'PENDING',
    confirmation_count   INT              DEFAULT 0,
    reported_at          DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_incident_grievance (grievance_id),
    CONSTRAINT fk_incident_grievance
        FOREIGN KEY (grievance_id) REFERENCES grievances (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- TABLE 14 — incident_confirmations
-- A student confirms (or disputes) an incident report.
-- Many-to-one with both incidents and students.
-- =============================================================================
CREATE TABLE incident_confirmations (
    id           BIGINT  NOT NULL AUTO_INCREMENT,
    incident_id  BIGINT  NOT NULL,
    student_id   BIGINT  NOT NULL,
    confirmed    BIT(1),
    comment      TEXT,
    confirmed_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    KEY idx_ic_incident (incident_id),
    KEY idx_ic_student  (student_id),
    CONSTRAINT fk_ic_incident
        FOREIGN KEY (incident_id) REFERENCES incidents (id) ON DELETE CASCADE,
    CONSTRAINT fk_ic_student
        FOREIGN KEY (student_id)  REFERENCES students  (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =============================================================================
-- End of schema.sql
-- Run seed_data.sql next to populate initial colleges, departments,
-- and the Super Admin account.
-- =============================================================================
