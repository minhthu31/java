-- V4__seed_features_sprints_tasks.sql
-- Seed dữ liệu mẫu cho CNPM-62 (Groups, Projects, Sprints, Features, Tasks)

-- =====================================================================
-- 0.1 TẠO 1 NHÓM SINH VIÊN MẪU (Bắt buộc vì Project yêu cầu group_id)
-- =====================================================================
INSERT INTO student_groups (code, name, leader_user_id) 
VALUES (
    'GROUP_TEST_62', 
    'Nhóm Test CNPM 62',
    (SELECT id FROM users WHERE username = 'leader_test')
);

-- =====================================================================
-- 0.2 TẠO 1 PROJECT MẪU
-- Cột bắt buộc: group_id, name 
-- =====================================================================
INSERT INTO projects (group_id, name) 
VALUES (
    (SELECT id FROM student_groups WHERE code = 'GROUP_TEST_62'),
    'Dự án Thực hành CNPM 62'
);

-- =====================================================================
-- 1. TẠO 1 SPRINT MẪU
-- =====================================================================
INSERT INTO sprints (project_id, name, state, start_date, end_date) 
VALUES (
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    'Sprint 1 - Khởi tạo hệ thống', 
    'ACTIVE', 
    '2026-08-17 00:00:00', 
    '2026-08-31 23:59:59'
);

-- =====================================================================
-- 2. TẠO 2 FEATURE MẪU
-- =====================================================================
INSERT INTO features (project_id, name, description) 
VALUES 
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    'Feature 1: Quản lý người dùng', 
    'Chứa các task về Đăng nhập, Phân quyền'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    'Feature 2: Quản lý công việc', 
    'Chứa các task về Sprint, Feature, Task'
);

-- =====================================================================
-- 3. TẠO 9 TASK THUỘC NHIỀU CLASSIFICATION & GÁN CHO TÀI KHOẢN HỢP LỆ
-- =====================================================================
INSERT INTO tasks (
    project_id, sprint_id, feature_id, assignee_user_id, 
    title, acceptance_criteria, issue_type, classification, priority, status
) VALUES 
-- 3 Task thuộc Feature 1
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 1: Quản lý người dùng'),
    (SELECT id FROM users WHERE username = 'admin_test'),
    'Thiết kế DB Schema cho Users', 'Đủ 21 bảng theo ERD', 'TASK', 'DATABASE', 'HIGH', 'DONE'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 1: Quản lý người dùng'),
    (SELECT id FROM users WHERE username = 'leader_test'),
    'Viết API Đăng nhập Authentication', 'Trả về JWT token hợp lệ', 'STORY', 'BACKEND', 'CRITICAL', 'IN_PROGRESS'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 1: Quản lý người dùng'),
    (SELECT id FROM users WHERE username = 'member_test'),
    'Làm giao diện màn hình Login', 'Giống Figma 100%', 'TASK', 'FRONTEND', 'MEDIUM', 'TO_DO'
),

-- 6 Task thuộc Feature 2
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'gv_test'),
    'Review tài liệu thiết kế Sprint', 'Phê duyệt doc trên Confluence', 'TASK', 'DOCUMENTATION', 'LOW', 'TO_DO'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'member_test'),
    'Thiết kế UI/UX cho Kanban Board', 'Hoàn thành bản Wireframe', 'STORY', 'UI_UX', 'MEDIUM', 'IN_PROGRESS'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'member_test'),
    'Viết Unit Test cho Sprint API', 'Coverage đạt > 80%', 'TASK', 'TESTING', 'HIGH', 'TO_DO'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'leader_test'),
    'Nghiên cứu thư viện kéo thả Task', 'Chọn được 1 lib React dnd', 'RESEARCH', 'RESEARCH', 'LOW', 'DONE'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'admin_test'),
    'Cấu hình Docker cho môi trường test', 'Có file docker-compose.yml', 'TASK', 'DEVOPS', 'HIGH', 'DONE'
),
(
    (SELECT id FROM projects WHERE name = 'Dự án Thực hành CNPM 62'),
    (SELECT id FROM sprints WHERE name = 'Sprint 1 - Khởi tạo hệ thống'),
    (SELECT id FROM features WHERE name = 'Feature 2: Quản lý công việc'),
    (SELECT id FROM users WHERE username = 'leader_test'),
    'Fix lỗi không hiển thị tên Feature', 'Giao diện hiển thị đúng tên', 'BUG', 'BUG_FIX', 'CRITICAL', 'TO_DO'
);