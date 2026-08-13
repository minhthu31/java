# Sprint 1 database foundation (CNPM-35, CNPM-36, CNPM-39)

## CNPM-35 — Spring Boot connection to MySQL

The application uses the root `application.yml`; no second Spring Boot project is required.

Required environment variables:

```text
DB_NAME=cnpm_project_support
DB_USERNAME=cnpm_user
DB_PASSWORD=<local password>
INTEGRATION_ENCRYPTION_KEY=<at least 32 random characters>
```

Developers can either use an installed MySQL 8.4 service or start the provided Docker Compose service. Secrets must remain in local environment variables or an untracked `.env` file.

## CNPM-36 — Initial Flyway migration

Flyway is the schema source of truth. Hibernate uses `ddl-auto=validate` and must not update the schema automatically.

Existing migrations:

- `V1__create_core_schema.sql`: creates the initial schema, including `roles` and `users`.
- `V2__seed_roles.sql`: inserts `ADMIN`, `LECTURER`, `TEAM_LEADER`, and `TEAM_MEMBER`.
- `V3__seed_test_users.sql`: inserts one active local test account for every supported role.

Never edit an applied migration. Every future database change must use a new migration version.

## CNPM-39 — Local test accounts

The four accounts below are development/test data only:

| Role | Username | Email |
|---|---|---|
| ADMIN | `admin.test` | `admin.test@example.com` |
| LECTURER | `lecturer.test` | `lecturer.test@example.com` |
| TEAM_LEADER | `leader.test` | `leader.test@example.com` |
| TEAM_MEMBER | `member.test` | `member.test@example.com` |

Temporary local password: `password`.

Only its BCrypt hash is stored in the migration. These accounts must not be enabled in a production deployment; replace the seed strategy before production use.

## Verification

After starting the application against an empty MySQL database, run:

```sql
USE cnpm_project_support;

SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT code, name
FROM roles
ORDER BY id;

SELECT u.username, u.email, u.full_name, u.status, r.code AS role
FROM users u
JOIN roles r ON r.id = u.role_id
ORDER BY u.id;
```

Expected result: migrations V1–V3 succeed, four supported roles exist, and each role has one active test user.
