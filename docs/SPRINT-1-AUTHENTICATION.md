# Sprint 1 Authentication Integration

## Scope

This integration completes CNPM-40, CNPM-41, CNPM-42, CNPM-45, CNPM-46, CNPM-47, CNPM-48 and CNPM-49. CNPM-43, CNPM-44, CNPM-50 and CNPM-51 are intentionally unchanged.

## API contract

`POST /api/v1/auth/login`

```json
{"usernameOrEmail":"admin","password":"Admin@123"}
```

The successful `ApiResponse.data` contains `accessToken`, `tokenType`, `expiresIn`, profile fields and `role`.

## Role boundaries

- `/api/v1/admin/**`: ADMIN
- `/api/v1/lecturer/**`: LECTURER
- `/api/v1/team-leader/**`: TEAM_LEADER
- `/api/v1/member/**`: TEAM_MEMBER

Anonymous access returns 401; authenticated users with the wrong role receive 403.

## Local setup

Copy `.env.example` to `.env`, replace all placeholder secrets, run MySQL, then start Spring Boot. For the UI, copy `frontend/.env.example` to `frontend/.env`, run `npm install`, then `npm start` inside `frontend`.
