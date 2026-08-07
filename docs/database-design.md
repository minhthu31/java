# DATA DICTIONARY (TỪ ĐIỂN DỮ LIỆU HỆ THỐNG)

> **Phiên bản:** 1.0 (Baseline 2.0)  
> **Tệp tin:** `docs/database-design.md`  
> **Mô tả:** Tài liệu mô tả chi tiết 21 bảng dữ liệu, các thuộc tính, kiểu dữ liệu, khóa và các ràng buộc dữ liệu của hệ thống.

---

## 1. PHÂN KHU HỆ THỐNG & NGƯỜI DÙNG

### 1.1 Bảng `roles` (Vai trò)
Mô tả: Lưu danh sách vai trò người dùng trong hệ thống (ADMIN, LECTURER, STUDENT...).

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID vai trò |
| `name` | VARCHAR | 50 | - | - | No | Yes | - | Tên vai trò |
| `description` | VARCHAR | 255 | - | - | Yes | No | NULL | Mô tả vai trò |

---

### 1.2 Bảng `users` (Người dùng)
Mô tả: Lưu thông tin tài khoản đăng nhập hệ thống.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | - | PK | - | No | Yes | Auto Increment | ID tài khoản |
| `role_id` | INT | - | FK | `roles(id)` | No | No | - | Khóa ngoại vai trò |
| `username` | VARCHAR | 50 | - | - | No | Yes | - | Tên đăng nhập |
| `email` | VARCHAR | 255 | - | - | No | Yes | - | Email cá nhân |
| `password_hash` | VARCHAR | 255 | - | - | No | No | - | Mật khẩu mã hóa (BCrypt) |
| `status` | VARCHAR | 20 | - | - | No | No | 'ACTIVE' | Trạng thái (ACTIVE/LOCKED) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | DATETIME | - | - | - | Yes | No | NULL | Ngày cập nhật |

---

### 1.3 Bảng `user_external_accounts` (Tài khoản tích hợp)
Mô tả: Liên kết tài khoản hệ thống với tài khoản Jira Cloud và GitHub.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | - | PK | - | No | Yes | Auto Increment | ID liên kết |
| `user_id` | BIGINT | - | FK | `users(id)` | No | No | - | Khóa ngoại người dùng |
| `jira_account_id` | VARCHAR | 128 | - | - | Yes | No | NULL | ID tài khoản Jira Cloud |
| `github_user_id` | VARCHAR | 128 | - | - | Yes | No | NULL | ID/Username tài khoản GitHub |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày liên kết |

---

## 2. PHÂN KHU NHÓM & DỰ ÁN

### 2.1 Bảng `student_groups` (Nhóm sinh viên)
Mô tả: Lưu thông tin các nhóm đồ án sinh viên.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID nhóm |
| `code` | VARCHAR | 30 | - | - | No | Yes | - | Mã nhóm |
| `name` | VARCHAR | 100 | - | - | No | No | - | Tên nhóm |
| `leader_user_id` | BIGINT | - | FK | `users(id)` | Yes | No | NULL | Trưởng nhóm |
| `start_date` | DATE | - | - | - | Yes | No | NULL | Ngày bắt đầu |
| `end_date` | DATE | - | - | - | Yes | No | NULL | Ngày kết thúc |
| `status` | VARCHAR | 20 | - | - | No | No | 'ACTIVE' | Trạng thái nhóm |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |

---

### 2.2 Bảng `group_members` (Thành viên nhóm)
Mô tả: Bảng trung gian quản lý danh sách sinh viên thuộc từng nhóm.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `group_id` | INT | - | PK, FK | `student_groups(id)` | No | No | - | Khóa ngoại nhóm |
| `user_id` | BIGINT | - | PK, FK | `users(id)` | No | No | - | Khóa ngoại sinh viên |
| `member_role` | VARCHAR | 30 | - | - | No | No | 'MEMBER' | Vai trò trong nhóm |
| `joined_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tham gia |

---

### 2.3 Bảng `group_lecturers` (Giảng viên phụ trách)
Mô tả: Bảng trung gian phân công giảng viên hướng dẫn cho nhóm.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `group_id` | INT | - | PK, FK | `student_groups(id)` | No | No | - | Khóa ngoại nhóm |
| `lecturer_user_id` | BIGINT | - | PK, FK | `users(id)` | No | No | - | Khóa ngoại giảng viên |
| `assigned_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày phân công |

---

### 2.4 Bảng `projects` (Dự án Jira)
Mô tả: Thông tin Dự án liên kết với Jira.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID dự án |
| `group_id` | INT | - | FK | `student_groups(id)` | No | No | - | Khóa ngoại nhóm |
| `name` | VARCHAR | 150 | - | - | No | No | - | Tên dự án |
| `jira_project_id` | VARCHAR | 50 | - | - | Yes | No | NULL | ID dự án trên Jira Cloud |
| `jira_project_key` | VARCHAR | 20 | - | - | Yes | No | NULL | Key dự án Jira (VD: CNPM) |
| `status` | VARCHAR | 20 | - | - | No | No | 'IN_PROGRESS' | Trạng thái dự án |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |

---

## 3. PHÂN KHU YÊU CẦU, TASK & JIRA

### 3.1 Bảng `requirements` (Yêu cầu SRS)
Mô tả: Danh sách các Yêu cầu nghiệp vụ (Requirements).

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID Yêu cầu |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `jira_issue_key` | VARCHAR | 50 | - | - | Yes | No | NULL | Jira Key tương ứng |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tiêu đề Yêu cầu |
| `actor` | VARCHAR | 100 | - | - | Yes | No | NULL | Tác nhân thực hiện |
| `priority` | VARCHAR | 20 | - | - | No | No | 'MEDIUM' | Độ ưu tiên |
| `status` | VARCHAR | 20 | - | - | No | No | 'DRAFT' | Trạng thái Yêu cầu |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |

---

### 3.2 Bảng `features` (Tính năng lớn / Epic)
Mô tả: Gom nhóm các Task theo tính năng.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID Feature |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `jira_epic_key` | VARCHAR | 50 | - | - | Yes | No | NULL | Epic Key trên Jira |
| `name` | VARCHAR | 150 | - | - | No | No | - | Tên Feature |
| `parent_feature_id` | INT | - | FK | `features(id)` | Yes | No | NULL | Feature cấp cha (nếu có) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |

---

### 3.3 Bảng `sprints` (Chu kỳ phát triển)
Mô tả: Quản lý các Sprint của dự án.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID Sprint |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `jira_sprint_id` | VARCHAR | 50 | - | - | Yes | No | NULL | ID Sprint trên Jira |
| `name` | VARCHAR | 100 | - | - | No | No | - | Tên Sprint |
| `state` | VARCHAR | 20 | - | - | No | No | 'FUTURE' | Trạng thái (ACTIVE/CLOSED...) |
| `start_date` | DATETIME | - | - | - | Yes | No | NULL | Bắt đầu Sprint |
| `end_date` | DATETIME | - | - | - | Yes | No | NULL | Kết thúc Sprint |

---

### 3.4 Bảng `tasks` (Công việc)
Mô tả: Bảng trung tâm lưu vết công việc cần làm.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID công việc nội bộ |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `requirement_id` | INT | - | FK | `requirements(id)` | Yes | No | NULL | Khóa ngoại yêu cầu |
| `feature_id` | INT | - | FK | `features(id)` | Yes | No | NULL | Khóa ngoại feature |
| `sprint_id` | INT | - | FK | `sprints(id)` | Yes | No | NULL | Khóa ngoại sprint |
| `assignee_user_id` | BIGINT | - | FK | `users(id)` | Yes | No | NULL | Sinh viên được giao |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tiêu đề công việc |
| `issue_type` | VARCHAR | 30 | - | - | No | No | 'Task' | Loại (Task/Bug/Subtask...) |
| `priority` | VARCHAR | 20 | - | - | No | No | 'Medium' | Độ ưu tiên |
| `deadline` | DATE | - | - | - | Yes | No | NULL | Hạn chót hoàn thành |
| `status` | VARCHAR | 30 | - | - | No | No | 'To Do' | Trạng thái công việc |
| `sync_status` | VARCHAR | 20 | - | - | No | No | 'NOT_SYNCED' | Trạng thái đồng bộ |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | DATETIME | - | - | - | Yes | No | NULL | Ngày cập nhật |

---

### 3.5 Bảng `jira_issues` (Map Jira Issue)
Mô tả: Ánh xạ 1-1 giữa Task nội bộ và Issue trên Jira.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID bảng ánh xạ |
| `task_id` | INT | - | FK | `tasks(id)` | No | Yes | - | Khóa ngoại Task (Unique 1-1) |
| `jira_issue_id` | VARCHAR | 64 | - | - | No | No | - | ID Issue trên Jira Cloud |
| `jira_issue_key` | VARCHAR | 50 | - | - | No | Yes | - | Issue Key (VD: CNPM-19) |
| `last_synced_at` | DATETIME | - | - | - | Yes | No | NULL | Thời gian đồng bộ gần nhất |

---

## 4. PHÂN KHU GITHUB & AUTO-TEST

### 4.1 Bảng `github_repositories` (Repo GitHub)
Mô tả: Quản lý thông tin Repository GitHub của nhóm.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID Repo nội bộ |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `full_name` | VARCHAR | 150 | - | - | No | Yes | - | Tên repo (VD: org/repo) |
| `default_branch` | VARCHAR | 50 | - | - | No | No | 'main' | Nhánh mặc định |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày thêm repo |

---

### 4.2 Bảng `github_commits` (Lịch sử Commit)
Mô tả: Lưu trữ danh sách commit lấy từ GitHub.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID commit |
| `repository_id` | INT | - | FK | `github_repositories(id)` | No | No | - | Khóa ngoại Repo |
| `sha` | VARCHAR | 40 | - | - | No | Yes | - | Mã SHA commit |
| `author_external_id` | VARCHAR | 128 | - | - | Yes | No | NULL | ID/Username người commit |
| `message` | TEXT | - | - | - | Yes | No | NULL | Nội dung commit |
| `additions` | INT | - | - | - | No | No | 0 | Số dòng thêm |
| `deletions` | INT | - | - | - | No | No | 0 | Số dòng xóa |
| `is_reverted` | BOOLEAN | - | - | - | No | No | FALSE | Đánh dấu commit bị revert |
| `committed_at` | DATETIME | - | - | - | Yes | No | NULL | Thời gian commit |

---

### 4.3 Bảng `github_pull_requests` (Pull Requests)
Mô tả: Lưu trữ danh sách PR từ GitHub.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID PR nội bộ |
| `repository_id` | INT | - | FK | `github_repositories(id)` | No | No | - | Khóa ngoại Repo |
| `number` | INT | - | - | - | No | No | - | Số thứ tự PR trên GitHub |
| `head_ref` | VARCHAR | 100 | - | - | Yes | No | NULL | Tên nhánh làm việc |
| `state` | VARCHAR | 20 | - | - | No | No | 'open' | Trạng thái PR (open/closed) |
| `html_url` | VARCHAR | 255 | - | - | Yes | No | NULL | Đường dẫn xem PR |
| `created_at` | DATETIME | - | - | - | Yes | No | NULL | Ngày tạo PR |

---

### 4.4 Bảng `workflow_runs` (Kết quả Auto-Test)
Mô tả: Lưu vết kết quả chạy GitHub Actions / CI/CD pipeline.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID lượt chạy |
| `repository_id` | INT | - | FK | `github_repositories(id)` | No | No | - | Khóa ngoại Repo |
| `commit_id` | INT | - | FK | `github_commits(id)` | Yes | No | NULL | Khóa ngoại Commit |
| `external_run_id` | VARCHAR | 64 | - | - | No | Yes | - | Run ID trên GitHub Actions |
| `status` | VARCHAR | 30 | - | - | Yes | No | NULL | Trạng thái (completed...) |
| `conclusion` | VARCHAR | 30 | - | - | Yes | No | NULL | Kết quả (success/failure) |
| `run_at` | DATETIME | - | - | - | Yes | No | NULL | Thời gian chạy |

---

### 4.5 Bảng `task_commit_links` (Liên kết Task - Commit)
Mô tả: Bảng trung gian nối Task với Commit (Quan hệ N-N).

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `task_id` | INT | - | PK, FK | `tasks(id)` | No | No | - | Khóa ngoại Task |
| `commit_id` | INT | - | PK, FK | `github_commits(id)` | No | No | - | Khóa ngoại Commit |
| `link_source` | VARCHAR | 20 | - | - | No | No | 'AUTO' | Nguồn tạo (AUTO/MANUAL) |
| `linked_by` | BIGINT | - | FK | `users(id)` | Yes | No | NULL | Người gán link (nếu thủ công) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày liên kết |

---

### 4.6 Bảng `task_pr_links` (Liên kết Task - Pull Request)
Mô tả: Bảng trung gian nối Task với Pull Request (Quan hệ N-N).

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `task_id` | INT | - | PK, FK | `tasks(id)` | No | No | - | Khóa ngoại Task |
| `pr_id` | INT | - | PK, FK | `github_pull_requests(id)` | No | No | - | Khóa ngoại PR |
| `link_source` | VARCHAR | 20 | - | - | No | No | 'AUTO' | Nguồn tạo (AUTO/MANUAL) |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày liên kết |

---

## 5. PHÂN KHU LOGS & XUẤT BÁO CÁO (SRS)

### 5.1 Bảng `sync_logs` (Nhật ký Đồng bộ)
Mô tả: Lưu vết lỗi hoặc trạng thái các lượt đồng bộ dữ liệu với Jira/GitHub.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID nhật ký đồng bộ |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `provider` | VARCHAR | 30 | - | - | No | No | - | Nguồn đồng bộ (JIRA/GITHUB) |
| `entity_type` | VARCHAR | 30 | - | - | No | No | - | Loại thực thể (TASK/COMMIT) |
| `status` | VARCHAR | 20 | - | - | No | No | - | Kết quả (SUCCESS/FAILED) |
| `error_message` | TEXT | - | - | - | Yes | No | NULL | Chi tiết lỗi nếu có |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Ngày ghi log |

---

### 5.2 Bảng `activity_logs` (Lịch sử Hoạt động)
Mô tả: Audit log ghi lại mọi thao tác của người dùng trên hệ thống.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID nhật ký |
| `actor_user_id` | BIGINT | - | FK | `users(id)` | Yes | No | NULL | Người thực hiện |
| `group_id` | INT | - | FK | `student_groups(id)` | Yes | No | NULL | Nhóm liên quan |
| `action` | VARCHAR | 100 | - | - | No | No | - | Hành động thực hiện |
| `result` | VARCHAR | 20 | - | - | No | No | 'SUCCESS' | Kết quả hành động |
| `created_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời điểm thực hiện |

---

### 5.3 Bảng `srs_versions` (Phiên bản Tài liệu SRS)
Mô tả: Lưu trữ các phiên bản SRS được kết xuất ra file.

| Column Name | Data Type | Length | Key | Reference | Nullable | Unique | Default Value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Auto Increment | ID bản xuất SRS |
| `project_id` | INT | - | FK | `projects(id)` | No | No | - | Khóa ngoại dự án |
| `version` | VARCHAR | 20 | - | - | No | No | - | Tên phiên bản (VD: v1.0) |
| `file_path` | VARCHAR | 500 | - | - | No | No | - | Đường dẫn file lưu trữ |
| `generated_at` | DATETIME | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian tạo file |