# Data Dictionary - Thiết kế Cơ sở dữ liệu

Tài liệu này mô tả chi tiết cấu trúc các bảng trong sơ đồ ERD của hệ thống Quản lý dự án & Tích hợp Jira/GitHub.

## 1. Bảng: `roles`
**Mô tả:** Lưu trữ danh sách phân quyền (vai trò) trong hệ thống.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính của bảng |
| `role_name` | VARCHAR | 50 | - | - | No | Yes | - | Tên vai trò (Ví dụ: Admin, Sinh viên...) |

## 2. Bảng: `users`
**Mô tả:** Lưu trữ thông tin người dùng.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `role_id` | INT | - | FK | `roles(id)` | No | No | - | Khóa ngoại tham chiếu đến bảng roles |
| `username` | VARCHAR | 50 | - | - | No | Yes | - | Tên đăng nhập |
| `password` | VARCHAR | 255 | - | - | No | No | - | Mật khẩu (đã mã hóa) |

## 3. Bảng: `student_groups`
**Mô tả:** Lưu trữ thông tin các nhóm sinh viên.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính định danh nhóm |
| `group_name` | VARCHAR | 100 | - | - | No | Yes | - | Tên nhóm |

## 4. Bảng: `projects`
**Mô tả:** Lưu trữ thông tin dự án của các nhóm.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính dự án |
| `group_id` | INT | - | FK | `student_groups(id)`| No | No | - | Nhóm thực hiện dự án này |
| `name` | VARCHAR | 255 | - | - | No | No | - | Tên dự án |

## 5. Bảng: `tasks`
**Mô tả:** Lưu trữ danh sách các công việc trong dự án.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính công việc |
| `project_id`| INT | - | FK | `projects(id)` | No | No | - | Thuộc dự án nào |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tiêu đề công việc |
| `status` | VARCHAR | 50 | - | - | Yes | No | 'To Do' | Trạng thái (To Do, In Progress, Done) |

## 6. Bảng: `jira_issues`
**Mô tả:** Lưu trữ thông tin liên kết với Jira.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `task_id` | INT | - | FK | `tasks(id)` | No | No | - | Liên kết với task nội bộ |
| `issue_key` | VARCHAR | 50 | - | - | No | Yes | - | Mã Issue trên Jira (VD: CNPM-20) |