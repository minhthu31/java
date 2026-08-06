# Data Dictionary - Thiết kế Cơ sở dữ liệu

Tài liệu này mô tả chi tiết cấu trúc dữ liệu của hệ thống, dựa trên thiết kế Entity Relationship Diagram (ERD).

## 1. Bảng `roles`
Lưu trữ thông tin các nhóm quyền trong hệ thống (VD: Sinh viên, Giảng viên).

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh duy nhất của quyền |
| `name` | VARCHAR | 50 | - | - | No | Yes | - | Tên quyền (VD: Student, Teacher) |

## 2. Bảng `users`
Lưu trữ thông tin tài khoản người dùng đăng nhập hệ thống.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh người dùng |
| `role_id` | INT | - | FK | `roles(id)` | No | No | - | Phân quyền của người dùng |
| `username` | VARCHAR | 50 | - | - | No | Yes | - | Tên đăng nhập |
| `email` | VARCHAR | 100 | - | - | No | Yes | - | Địa chỉ email người dùng |
| `password_hash` | VARCHAR | 255 | - | - | No | No | - | Mật khẩu đã được mã hóa (hash) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian tạo tài khoản |

## 3. Bảng `student_groups`
Lưu trữ thông tin các nhóm sinh viên làm bài tập/đồ án.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh của nhóm |
| `name` | VARCHAR | 100 | - | - | No | No | - | Tên nhóm |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian lập nhóm |

## 4. Bảng `group_members`
Bảng trung gian thể hiện quan hệ nhiều-nhiều giữa sinh viên và nhóm.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `group_id` | INT | - | PK, FK | `student_groups(id)`| No | No | - | Mã nhóm |
| `user_id` | INT | - | PK, FK | `users(id)` | No | No | - | Mã người dùng (sinh viên) |
| `joined_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian sinh viên tham gia nhóm |

## 5. Bảng `projects`
Lưu trữ thông tin các dự án/đồ án do các nhóm thực hiện.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh dự án |
| `group_id` | INT | - | FK | `student_groups(id)`| No | No | - | Nhóm thực hiện dự án này |
| `name` | VARCHAR | 255 | - | - | No | No | - | Tên dự án |
| `description` | TEXT | - | - | - | Yes | No | NULL | Mô tả chi tiết dự án |

## 6. Bảng `requirements`
Lưu trữ các yêu cầu (Requirement) thuộc về một dự án.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh yêu cầu |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Dự án chứa yêu cầu này |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tiêu đề yêu cầu |
| `description` | TEXT | - | - | - | Yes | No | NULL | Mô tả chi tiết yêu cầu |

## 7. Bảng `tasks`
Lưu trữ các công việc chi tiết cần làm để đáp ứng yêu cầu.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh công việc |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Dự án chứa công việc này |
| `requirement_id`| INT | - | FK | `requirements(id)`| Yes| No | NULL | Yêu cầu liên kết với công việc (nếu có) |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tên công việc |
| `status` | VARCHAR | 50 | - | - | No | No | 'TODO' | Trạng thái công việc (TODO, IN_PROGRESS, DONE) |

## 8. Bảng `jira_issues`
Lưu trữ thông tin liên kết giữa Task trong hệ thống và Issue trên Jira Cloud.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh bản ghi |
| `task_id` | INT | - | FK | `tasks(id)` | No | Yes | - | Task liên kết (Quan hệ 1-1) |
| `jira_issue_key`| VARCHAR | 50 | - | - | No | Yes | - | Mã Issue trên Jira (VD: CNPM-1) |
| `url` | VARCHAR | 255 | - | - | Yes | No | NULL | Đường dẫn trực tiếp đến Issue trên Jira |

## 9. Bảng `github_repositories`
Lưu trữ thông tin kho mã nguồn (Repository) của dự án.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh bản ghi |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Dự án sở hữu kho code này |
| `repo_url` | VARCHAR | 255 | - | - | No | No | - | Đường dẫn đến Repository trên GitHub |

## 10. Bảng `github_commits`
Lưu trữ lịch sử đẩy code (commits) từ GitHub.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh bản ghi |
| `repository_id` | INT | - | FK | `github_repositories(id)`| No | No | - | Kho code chứa commit này |
| `commit_hash` | VARCHAR | 100 | - | - | No | Yes | - | Mã hash định danh duy nhất của commit |
| `message` | TEXT | - | - | - | Yes | No | NULL | Nội dung (message) của lần commit |

## 11. Bảng `task_commit_links`
Bảng trung gian liên kết nhiều-nhiều giữa Tasks và GitHub Commits.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `task_id` | INT | - | PK, FK | `tasks(id)` | No | No | - | Mã công việc |
| `commit_id` | INT | - | PK, FK | `github_commits(id)` | No | No | - | Mã commit tương ứng |

## 12. Bảng `sync_logs`
Lưu nhật ký đồng bộ dữ liệu giữa hệ thống và Jira/GitHub.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh log |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Dự án được đồng bộ |
| `sync_type` | VARCHAR | 50 | - | - | No | No | - | Loại đồng bộ (JIRA hoặc GITHUB) |
| `status` | VARCHAR | 50 | - | - | No | No | - | Trạng thái đồng bộ (SUCCESS, FAILED) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian thực hiện đồng bộ |

## 13. Bảng `activity_logs`
Lưu nhật ký các thao tác của người dùng trên hệ thống.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | Mã định danh log |
| `user_id` | INT | - | FK | `users(id)` | No | No | - | Người thực hiện thao tác |
| `action` | VARCHAR | 255 | - | - | No | No | - | Hành động đã thực hiện (VD: Tạo task mới) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian thực hiện thao tác |