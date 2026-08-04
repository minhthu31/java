# Data Dictionary - Thiết kế Cơ sở dữ liệu

Tài liệu này mô tả chi tiết cấu trúc các bảng trong sơ đồ ERD của hệ thống Quản lý dự án & Tích hợp Jira/GitHub.

## 1. Bảng: `roles`
**Mô tả:** Lưu trữ danh sách phân quyền (vai trò) trong hệ thống.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính của bảng |
| `role_name` | VARCHAR | 50 | - | - | No | Yes | - | Tên vai trò (Ví dụ: Admin, Student...) |

## 2. Bảng: `users`
**Mô tả:** Lưu trữ thông tin tài khoản người dùng.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `role_id` | INT | - | FK | `roles(id)` | No | No | - | Khóa ngoại tham chiếu đến bảng roles |
| `username` | VARCHAR | 50 | - | - | No | Yes | - | Tên đăng nhập duy nhất |
| `password` | VARCHAR | 255 | - | - | No | No | - | Mật khẩu (đã mã hóa) |

## 3. Bảng: `student_groups`
**Mô tả:** Lưu trữ thông tin các nhóm sinh viên.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính định danh nhóm |
| `group_name` | VARCHAR | 100 | - | - | No | Yes | - | Tên nhóm |

## 4. Bảng: `group_members` (Bảng trung gian)
**Mô tả:** Thể hiện quan hệ n-n giữa người dùng và nhóm.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `group_id` | INT | - | PK, FK | `student_groups(id)`| No | No | - | Khóa ngoại tham chiếu đến nhóm |
| `user_id` | INT | - | PK, FK | `users(id)` | No | No | - | Khóa ngoại tham chiếu đến thành viên |

## 5. Bảng: `projects`
**Mô tả:** Lưu trữ thông tin dự án của các nhóm.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính dự án |
| `group_id` | INT | - | FK | `student_groups(id)`| No | No | - | Nhóm thực hiện dự án này |
| `name` | VARCHAR | 255 | - | - | No | No | - | Tên dự án |

## 6. Bảng: `requirements`
**Mô tả:** Lưu trữ các yêu cầu chức năng của dự án.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `project_id`| INT | - | FK | `projects(id)` | No | No | - | Thuộc dự án nào |
| `content` | TEXT | - | - | - | No | No | - | Nội dung chi tiết của yêu cầu |

## 7. Bảng: `tasks`
**Mô tả:** Lưu trữ danh sách các công việc trong dự án.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính công việc |
| `project_id`| INT | - | FK | `projects(id)` | No | No | - | Thuộc dự án nào |
| `title` | VARCHAR | 255 | - | - | No | No | - | Tiêu đề công việc |
| `status` | VARCHAR | 50 | - | - | Yes | No | 'To Do' | Trạng thái (To Do, In Progress, Done) |

## 8. Bảng: `jira_issues`
**Mô tả:** Lưu trữ thông tin liên kết với Jira.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `task_id` | INT | - | FK | `tasks(id)` | No | No | - | Liên kết với task nội bộ hệ thống |
| `issue_key` | VARCHAR | 50 | - | - | No | Yes | - | Mã Issue trên Jira (VD: CNPM-20) |

## 9. Bảng: `github_repositories`
**Mô tả:** Lưu trữ thông tin kho lưu trữ GitHub của dự án.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `project_id`| INT | - | FK | `projects(id)` | No | No | - | Thuộc dự án nào |
| `repo_url` | VARCHAR | 255 | - | - | No | No | - | Đường dẫn URL của Repository |

## 10. Bảng: `github_commits`
**Mô tả:** Lưu trữ lịch sử commit từ GitHub.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `commit_hash`| VARCHAR | 40 | PK | - | No | Yes | - | Mã băm duy nhất của commit |
| `repo_id` | INT | - | FK | `github_repositories(id)`| No | No | - | Thuộc repo nào |
| `message` | TEXT | - | - | - | No | No | - | Nội dung tin nhắn commit |

## 11. Bảng: `task_commit_links` (Bảng trung gian)
**Mô tả:** Thể hiện quan hệ giữa Task và Commit (1 task có nhiều commit, 1 commit giải quyết nhiều task).
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `task_id` | INT | - | PK, FK | `tasks(id)` | No | No | - | Khóa ngoại trỏ đến task |
| `commit_hash`| VARCHAR | 40 | PK, FK | `github_commits(commit_hash)`| No | No | - | Khóa ngoại trỏ đến commit |

## 12. Bảng: `sync_logs`
**Mô tả:** Lưu lịch sử đồng bộ dữ liệu với hệ thống ngoài.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `project_id`| INT | - | FK | `projects(id)` | No | No | - | Dự án được đồng bộ |
| `status` | VARCHAR | 50 | - | - | No | No | - | Trạng thái (Thành công/Thất bại) |

## 13. Bảng: `activity_logs`
**Mô tả:** Lưu lịch sử hoạt động của người dùng trong hệ thống.
| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | INT | - | PK | - | No | Yes | Tự tăng | Khóa chính |
| `user_id` | INT | - | FK | `users(id)` | No | No | - | Người dùng thực hiện hành động |
| `action` | VARCHAR | 255 | - | - | No | No | - | Mô tả hành động (Ví dụ: Đăng nhập, Tạo task) |