-- CNPM-39: Four development/test accounts for the four supported roles.
-- Raw password for local testing only: password
-- The value stored below is a BCrypt hash (cost 10), never plaintext.

INSERT INTO users (role_id, username, email, password_hash, full_name, status)
SELECT r.id,
       'admin.test',
       'admin.test@example.com',
       '$2a$10$X5SEXqMNs8g55FRoNmLyAuT8ad6CumKvabP.fW5ieA655hYCLNgq2',
       'Test Admin',
       'ACTIVE'
FROM roles r
WHERE r.code = 'ADMIN';

INSERT INTO users (role_id, username, email, password_hash, full_name, status)
SELECT r.id,
       'lecturer.test',
       'lecturer.test@example.com',
       '$2a$10$X5SEXqMNs8g55FRoNmLyAuT8ad6CumKvabP.fW5ieA655hYCLNgq2',
       'Test Lecturer',
       'ACTIVE'
FROM roles r
WHERE r.code = 'LECTURER';

INSERT INTO users (role_id, username, email, password_hash, full_name, status)
SELECT r.id,
       'leader.test',
       'leader.test@example.com',
       '$2a$10$X5SEXqMNs8g55FRoNmLyAuT8ad6CumKvabP.fW5ieA655hYCLNgq2',
       'Test Team Leader',
       'ACTIVE'
FROM roles r
WHERE r.code = 'TEAM_LEADER';

INSERT INTO users (role_id, username, email, password_hash, full_name, status)
SELECT r.id,
       'member.test',
       'member.test@example.com',
       '$2a$10$X5SEXqMNs8g55FRoNmLyAuT8ad6CumKvabP.fW5ieA655hYCLNgq2',
       'Test Team Member',
       'ACTIVE'
FROM roles r
WHERE r.code = 'TEAM_MEMBER';
