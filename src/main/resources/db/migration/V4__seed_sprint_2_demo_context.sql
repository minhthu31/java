-- CNPM-72: minimal local demo context for the Sprint 2 end-to-end flow.
-- Test account passwords remain defined by V3 and are intended for local demo only.

INSERT INTO student_groups (code, name, leader_user_id, status)
SELECT 'CNPM_DEMO',
       'Nhóm demo Sprint 2',
       u.id,
       'ACTIVE'
  FROM users u
 WHERE u.username = 'leader.test';

INSERT INTO group_members (group_id, user_id, member_role, status)
SELECT g.id, u.id, 'TEAM_MEMBER', 'ACTIVE'
  FROM student_groups g
  JOIN users u ON u.username = 'member.test'
 WHERE g.code = 'CNPM_DEMO';

INSERT INTO group_lecturers (group_id, lecturer_user_id)
SELECT g.id, u.id
  FROM student_groups g
  JOIN users u ON u.username = 'lecturer.test'
 WHERE g.code = 'CNPM_DEMO';

INSERT INTO projects (group_id, name, status)
SELECT g.id, 'CNPM Project Management Tool', 'ACTIVE'
  FROM student_groups g
 WHERE g.code = 'CNPM_DEMO';

INSERT INTO sprints (project_id, name, state, start_date, end_date)
SELECT p.id,
       'Sprint 2 - Requirements and Local',
       'ACTIVE',
       '2026-08-14 00:00:00',
       '2026-08-22 23:59:59'
  FROM projects p
 WHERE p.name = 'CNPM Project Management Tool';

INSERT INTO features (project_id, name, description)
SELECT p.id,
       'Requirements and Task Management',
       'Requirement, Task, assignment, authorization and SRS preview for Sprint 2.'
  FROM projects p
 WHERE p.name = 'CNPM Project Management Tool';

INSERT INTO tasks (
    project_id,
    feature_id,
    sprint_id,
    assignee_user_id,
    title,
    description,
    acceptance_criteria,
    issue_type,
    classification,
    priority,
    status,
    sync_status
)
SELECT p.id,
       f.id,
       s.id,
       u.id,
       'Task mẫu được giao cho member.test',
       'Dữ liệu mẫu để kiểm tra màn hình Task ngay sau khi khởi tạo database.',
       'Member đăng nhập và nhìn thấy Task được giao.',
       'TASK',
       'FEATURE_RELATED',
       'MEDIUM',
       'TO_DO',
       'NOT_SYNCED'
  FROM projects p
  JOIN features f
    ON f.project_id = p.id
   AND f.name = 'Requirements and Task Management'
  JOIN sprints s
    ON s.project_id = p.id
   AND s.name = 'Sprint 2 - Requirements and Local'
  JOIN users u ON u.username = 'member.test'
 WHERE p.name = 'CNPM Project Management Tool';
