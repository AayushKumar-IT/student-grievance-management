# Database — AI-Based Student Grievance Management System

This folder contains all database-related files for the project. The application uses **MySQL 8** with Hibernate auto-managing the schema at runtime. These files give you full control to set up, reset, or inspect the database manually.

---

## Folder Contents

| File | Purpose |
|------|---------|
| `schema.sql` | Full DDL — creates the database and all 14 tables with FK constraints and indexes |
| `seed_data.sql` | Initial data — 17 colleges and all their departments |
| `super_admin_setup.sql` | Creates the first Super Admin account (required before anyone can log in) |
| `README.md` | This file |

---

## Setup Order

Run the files in this exact sequence:

```bash
# Step 1 — Create database and all tables
mysql -u root -p < databases/schema.sql

# Step 2 — Insert colleges and departments
mysql -u root -p < databases/seed_data.sql

# Step 3 — Create the Super Admin account
mysql -u root -p < databases/super_admin_setup.sql
```

> All three scripts are **idempotent** — safe to run multiple times. Existing rows are not duplicated.

---

## Default Super Admin Credentials

Inserted by `super_admin_setup.sql`:

| Field | Value |
|-------|-------|
| Email | `superadmin@grievance.com` |
| Password | `Admin@1234` |
| Role | `SUPER_ADMIN` |

> ⚠️ Change the password immediately after first login.

---

## Table Overview

The database has **14 tables** grouped into four functional layers:

### Layer 1 — Identity & Auth
| Table | Description |
|-------|-------------|
| `users` | Central auth table. Every person has one row here. Spring Security authenticates against `email` + `password` (BCrypt). |
| `students` | Profile extension for `role = STUDENT`. One-to-one with `users`. |
| `faculty` | Profile extension for `role = FACULTY`. Includes `is_grievance_resolver` flag. |
| `college_admins` | Profile extension for `role = COLLEGE_ADMIN`. |
| `super_admins` | Profile extension for `role = SUPER_ADMIN`. System-wide authority. |
| `registration_tokens` | Single-use, role-scoped tokens required for registration. Expire after 7 days. |

### Layer 2 — Institution
| Table | Description |
|-------|-------------|
| `colleges` | Registered academic institutions. |
| `departments` | Departments within a college. `(college_id, code)` is unique. |

### Layer 3 — Grievance Lifecycle
| Table | Description |
|-------|-------------|
| `grievances` | Core table. Every student complaint. Tracks status, category, priority, assignment. |
| `evidences` | Files uploaded as supporting evidence. Many-to-one with `grievances`. |
| `incidents` | Structured incident reports attached to a grievance. |
| `incident_confirmations` | Student confirmations/disputes on an incident. |

### Layer 4 — AI Analysis
| Table | Description |
|-------|-------------|
| `ai_analyses` | AI microservice output per grievance — category, priority, fake/duplicate/anomaly flags and confidence scores. One-to-one with `grievances`. |
| `risk_assessments` | Risk score and level computed from AI analysis. Drives `requiresImmediateAction`. One-to-one with `grievances`. |

---

## Entity Relationship Diagram

```
users (1) ──────────────────── (1) students
  │                                    │
  │                                    │ many
  ├─── (1) faculty                     │
  │          │                         ▼
  │          │                    grievances (1) ──── (1) ai_analyses
  ├─── (1) college_admins          │    │                      │
  │          │                     │    └────────── (1) risk_assessments
  └─── (1) super_admins            │
                                   │ ─── (many) evidences
colleges (1) ──── (many) departments
   │                    │           ─── (many) incidents (1) ──── (many) incident_confirmations
   │                    │
   ├── (many) students ─┘
   ├── (many) faculty ──┘
   ├── (many) college_admins
   └── (many) registration_tokens
```

### Key Foreign Key Relationships

| Child Table | FK Column | References | On Delete |
|-------------|-----------|------------|-----------|
| `departments` | `college_id` | `colleges.id` | CASCADE |
| `students` | `user_id` | `users.id` | CASCADE |
| `students` | `college_id` | `colleges.id` | RESTRICT |
| `students` | `department_id` | `departments.id` | RESTRICT |
| `faculty` | `user_id` | `users.id` | CASCADE |
| `faculty` | `college_id` | `colleges.id` | RESTRICT |
| `faculty` | `department_id` | `departments.id` | RESTRICT |
| `college_admins` | `user_id` | `users.id` | CASCADE |
| `college_admins` | `college_id` | `colleges.id` | RESTRICT |
| `super_admins` | `user_id` | `users.id` | CASCADE |
| `registration_tokens` | `college_id` | `colleges.id` | SET NULL |
| `grievances` | `student_id` | `students.id` | RESTRICT |
| `grievances` | `college_id` | `colleges.id` | RESTRICT |
| `grievances` | `department_id` | `departments.id` | SET NULL |
| `grievances` | `assigned_faculty_id` | `faculty.id` | SET NULL |
| `ai_analyses` | `grievance_id` | `grievances.id` | CASCADE |
| `risk_assessments` | `grievance_id` | `grievances.id` | CASCADE |
| `evidences` | `grievance_id` | `grievances.id` | CASCADE |
| `incidents` | `grievance_id` | `grievances.id` | CASCADE |
| `incident_confirmations` | `incident_id` | `incidents.id` | CASCADE |
| `incident_confirmations` | `student_id` | `students.id` | RESTRICT |

---

## Connection Between Files and Spring Boot

The schema here mirrors the JPA entities exactly. Hibernate manages the schema at runtime via:

```properties
spring.jpa.hibernate.ddl-auto=update
```

This means:
- On first run → Hibernate creates all tables automatically
- On subsequent runs → Hibernate adds any new columns but never drops existing ones
- These SQL files → give you a clean baseline, a way to reset, and a source of truth for the schema

### When to use these files vs letting Hibernate manage

| Scenario | Use |
|----------|-----|
| Fresh install on a new machine | `schema.sql` → `seed_data.sql` → `super_admin_setup.sql` |
| Schema is drifted / column mismatch errors | Drop `grievance_db`, re-run `schema.sql` |
| Need to reset all data but keep structure | Truncate tables manually or re-run seed files |
| Production deployment | Use `schema.sql` as the base migration, switch `ddl-auto` to `validate` |

---

## Connection to the Backend

The backend connects to this database via `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://${MYSQLHOST:localhost}:${MYSQLPORT:3306}/${MYSQLDATABASE:grievance_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${MYSQLUSER:root}
spring.datasource.password=${MYSQLPASSWORD:your_password}
```

All environment variables have local fallbacks so the app works without any export commands. For production deployment (Railway, AWS RDS, etc.) set the actual environment variables and the fallbacks are ignored.

---

## Resetting the Database

To wipe everything and start fresh:

```sql
DROP DATABASE IF EXISTS grievance_db;
```

Then re-run all three scripts in order.

---

## Useful Queries

```sql
-- Count grievances by status
SELECT status, COUNT(*) AS total
FROM   grievances
GROUP BY status;

-- List all unresolved HIGH/CRITICAL grievances
SELECT g.id, g.title, g.category, g.priority, g.status,
       CONCAT(u.first_name, ' ', u.last_name) AS student,
       c.name AS college
FROM   grievances g
JOIN   students   s ON s.id = g.student_id
JOIN   users      u ON u.id = s.user_id
JOIN   colleges   c ON c.id = g.college_id
WHERE  g.priority IN ('HIGH', 'CRITICAL')
  AND  g.status   NOT IN ('RESOLVED', 'CLOSED', 'REJECTED')
ORDER  BY g.submitted_at ASC;

-- Grievances flagged by AI as fake or anomalous
SELECT g.id, g.title, g.category,
       a.is_fake_complaint, a.fake_confidence,
       a.is_anomaly, a.anomaly_score,
       r.risk_level
FROM   grievances    g
JOIN   ai_analyses   a ON a.grievance_id = g.id
JOIN   risk_assessments r ON r.grievance_id = g.id
WHERE  a.is_fake_complaint = 1 OR a.is_anomaly = 1
ORDER  BY r.risk_score DESC;

-- Active registration tokens (not used, not expired)
SELECT t.token, t.target_role, c.name AS college, t.expires_at
FROM   registration_tokens t
LEFT   JOIN colleges c ON c.id = t.college_id
WHERE  t.used = 0
  AND  t.expires_at > NOW();

-- Faculty who are grievance resolvers per college
SELECT c.name AS college,
       CONCAT(u.first_name, ' ', u.last_name) AS faculty_name,
       f.designation, f.employee_id
FROM   faculty   f
JOIN   users     u ON u.id = f.user_id
JOIN   colleges  c ON c.id = f.college_id
WHERE  f.is_grievance_resolver = 1
ORDER  BY c.name, u.last_name;
```
