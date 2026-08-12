-- 1. Thêm 4 Vai trò (Roles)
INSERT INTO roles (name, description) VALUES 
('ADMIN', 'Quản trị hệ thống'),
('LECTURER', 'Giảng viên hướng dẫn'),
('STUDENT', 'Sinh viên thực hiện'),
('STAFF', 'Cán bộ phòng đào tạo');

-- 2. Thêm 4 Tài khoản mẫu (Users)
-- Mật khẩu cho tất cả là '123456' (Hash BCrypt tương ứng: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG)

INSERT INTO users (role_id, username, email, password_hash, status) VALUES
-- Admin (role_id 1)
(1, 'admin_test', 'admin@demo.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ACTIVE'),
-- Lecturer (role_id 2)
(2, 'gv_test', 'lecturer@demo.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ACTIVE'),
-- Student (role_id 3)
(3, 'sv_test', 'student@demo.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ACTIVE'),
-- Staff (role_id 4)
(4, 'staff_test', 'staff@demo.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ACTIVE');