-- V3__seed_users.sql
-- Dữ liệu mẫu phục vụ kiểm thử đăng nhập và phân quyền (Task 39)
-- Mật khẩu mặc định cho tất cả tài khoản mẫu: 123456
-- Hash BCrypt tương ứng: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

INSERT INTO users (role_id, username, email, password_hash, full_name, status) VALUES
(
    (SELECT id FROM roles WHERE code = 'ADMIN'),
    'admin_test',
    'admin@demo.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Admin',
    'ACTIVE'
),
(
    (SELECT id FROM roles WHERE code = 'LECTURER'),
    'gv_test',
    'lecturer@demo.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Giảng viên',
    'ACTIVE'
),
(
    (SELECT id FROM roles WHERE code = 'TEAM_LEADER'),
    'leader_test',
    'leader@demo.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Trưởng nhóm',
    'ACTIVE'
),
(
    (SELECT id FROM roles WHERE code = 'TEAM_MEMBER'),
    'member_test',
    'member@demo.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'Thành viên',
    'ACTIVE'
);