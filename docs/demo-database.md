# Demo Database

Tài liệu hướng dẫn khởi tạo và sử dụng database demo cho project **CNPM Project Support Backend**.

## 1. Mục đích

Database demo được chuẩn bị để phục vụ:

- Kiểm thử luồng đăng nhập.
- Demo 4 vai trò: Admin, Lecturer, Team Leader và Team Member.
- Demo Project, Requirement, Feature, Sprint và Task.
- Demo dữ liệu Jira.
- Demo dữ liệu GitHub.
- Kiểm tra API báo cáo và thống kê.

Database demo sử dụng Flyway để chạy migration theo thứ tự từ `V1` đến `V13`.

---

## 2. Các tài khoản demo

| Vai trò | Username | Password |
|---|---|---|
| Admin | `admin.test` | `password` |
| Lecturer | `lecturer.test` | `password` |
| Team Leader | `leader.test` | `password` |
| Team Member | `member.test` | `password` |

Đây là tài khoản dành cho môi trường demo/local. Không sử dụng các thông tin này cho môi trường production.

Password của các tài khoản được lưu trong database dưới dạng BCrypt hash. Không lưu password dạng plaintext trong migration.

---

## 3. Tạo database sạch

Đăng nhập MySQL bằng tài khoản có quyền quản trị, ví dụ `root`.

Nếu muốn khởi tạo lại database demo từ đầu, chạy:

```sql
DROP DATABASE IF EXISTS cnpm_project_support;

CREATE DATABASE cnpm_project_support
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'cnpm_user'@'localhost'
  IDENTIFIED BY 'password';

GRANT ALL PRIVILEGES ON cnpm_project_support.* TO 'cnpm_user'@'localhost';

FLUSH PRIVILEGES;
```

Nếu `cnpm_user` đã tồn tại nhưng password không phải `password`, có thể cập nhật:

```sql
ALTER USER 'cnpm_user'@'localhost'
IDENTIFIED BY 'password';

GRANT ALL PRIVILEGES ON cnpm_project_support.* TO 'cnpm_user'@'localhost';

FLUSH PRIVILEGES;
```

Kiểm tra database:

```sql
SHOW DATABASES;
```

Phải có:

```text
cnpm_project_support
```

---

## 4. Cấu hình kết nối database

File cấu hình:

```text
src/main/resources/application.yml
```

Cấu hình local demo có thể sử dụng:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/${DB_NAME:cnpm_project_support}?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh}
    username: ${DB_USERNAME:cnpm_user}
    password: ${DB_PASSWORD:password}
```

Các biến môi trường có thể được cấu hình riêng:

```text
DB_URL
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
INTEGRATION_ENCRYPTION_KEY
```

---

## 5. Chạy migration

Từ thư mục gốc của project, chạy:

```powershell
.\mvnw.cmd clean spring-boot:run
```

Khi ứng dụng khởi động, Flyway sẽ tự động chạy các migration.

Migration demo cuối cùng là:

```text
src/main/resources/db/migration/V13__seed_final_demo_data.sql
```

---

## 6. Dữ liệu demo được tạo

Sau khi Flyway chạy thành công, database có dữ liệu mẫu cho:

### Người dùng và nhóm

- `admin.test`
- `lecturer.test`
- `leader.test`
- `member.test`
- Nhóm demo `CNPM_DEMO`

### Project

Project demo:

```text
CNPM Project Management Tool
```

Project có thông tin Jira mẫu:

```text
Jira site: https://demo.atlassian.net
Jira project key: CNPM
Jira project id: 10001
```

Các thông tin Jira trên chỉ phục vụ demo, không phải thông tin kết nối Jira thật.

### Requirement

Có các requirement mẫu, trong đó có:

```text
CNPM-201
CNPM-202
```

### Feature

Có feature mẫu:

```text
CNPM-EPIC-2
Authentication and Reporting
```

### Sprint

Có sprint demo:

```text
Sprint 2 - Requirements and Local
```

### Task

Có task mẫu được giao cho:

```text
member.test
```

Task có dữ liệu để trình diễn luồng Requirement → Feature → Sprint → Task.

---

## 7. Dữ liệu Jira demo

Database có dữ liệu Jira giả lập để phục vụ trình diễn:

- Jira issue.
- Jira issue snapshot.
- Jira backlog snapshot.
- Thời gian đồng bộ.
- Sync log thành công.

---

## 8. Dữ liệu GitHub demo

Database có dữ liệu GitHub giả lập:

- GitHub repository demo.
- Commit mẫu.
- Pull Request mẫu.
- Task → Commit link.
- Task → Pull Request link.
- Sync log GitHub.

Repository demo:

```text
demo/cnpm-project-support
```

Có các Pull Request mẫu với trạng thái:

```text
MERGED
OPEN
```

Dữ liệu commit và Pull Request được dùng để kiểm tra thống kê GitHub và báo cáo.

---

## 9. Kiểm tra dữ liệu sau khi chạy

Đăng nhập MySQL:

```sql
USE cnpm_project_support;
```

Kiểm tra người dùng:

```sql
SELECT username, full_name
FROM users;
```

Kiểm tra project:

```sql
SELECT id, name, jira_project_key
FROM projects;
```

Kiểm tra requirement:

```sql
SELECT id, jira_issue_key, title
FROM requirements;
```

Kiểm tra feature:

```sql
SELECT id, jira_issue_key, name
FROM features;
```

Kiểm tra sprint:

```sql
SELECT id, name
FROM sprints;
```

Kiểm tra task:

```sql
SELECT id, title, status
FROM tasks;
```

Kiểm tra GitHub commit:

```sql
SELECT sha, message, committed_at
FROM github_commits;
```

Kiểm tra Pull Request:

```sql
SELECT number, title, state, remote_created_at
FROM github_pull_requests;
```

Kiểm tra sync log:

```sql
SELECT provider, status, started_at, completed_at
FROM sync_logs
ORDER BY started_at DESC;
```

---

## 10. Kiểm tra ứng dụng

Sau khi ứng dụng khởi động thành công, server mặc định chạy tại:

```text
http://localhost:8080
```

Kiểm tra health:

```text
http://localhost:8080/actuator/health
```

Nếu ứng dụng hoạt động bình thường, kết quả sẽ có:

```json
{
  "status": "UP"
}
```

---

## 11. Đăng nhập

Endpoint đăng nhập:

```text
POST /api/v1/auth/login
```

Ví dụ:

```json
{
  "username": "admin.test",
  "password": "password"
}
```

Có thể kiểm tra lần lượt 4 tài khoản:

```text
admin.test
lecturer.test
leader.test
member.test
```

với password:

```text
password
```

---

## 12. Demo báo cáo

Sau khi đăng nhập và lấy JWT token, có thể sử dụng token để gọi các API yêu cầu xác thực.

Ví dụ endpoint báo cáo tổng quan:

```text
GET /api/v1/projects/{projectId}/reports/summary
```

Thay `{projectId}` bằng ID project demo trong database.

Dữ liệu GitHub demo cho phép kiểm tra các thống kê:

- Số commit theo thành viên.
- Pull Request mở.
- Pull Request đóng.
- Pull Request đã merge.
- Lọc theo project.
- Lọc theo khoảng thời gian.

Khoảng thời gian báo cáo sử dụng quy tắc:

```text
[from, to)
```

Tức là:

```text
>= from
< to
```

---

## 13. GitHub account demo

Trong dữ liệu demo, GitHub external account được tạo cho `leader.test`.

`member.test` được giữ ở trạng thái chưa liên kết GitHub để có thể trình diễn trường hợp thành viên chưa liên kết tài khoản GitHub.

Điều này giúp kiểm tra yêu cầu báo cáo phải thể hiện rõ thành viên chưa liên kết GitHub.

---

## 14. Integration config và secret

Migration demo không seed secret Jira/GitHub thật vào bảng `integration_configs`.

Lý do:

- Không đưa token thật vào source code.
- Không đưa API key thật vào migration.
- Secret phải được cấu hình ở môi trường local/development.
- Nếu ứng dụng sử dụng cơ chế mã hóa secret, khóa mã hóa phải được cung cấp qua biến môi trường `INTEGRATION_ENCRYPTION_KEY`.

---

## 15. Reset database demo

Khi cần làm lại database demo từ đầu:

### Bước 1

Dừng ứng dụng bằng:

```text
Ctrl + C
```

### Bước 2

Trong MySQL chạy:

```sql
DROP DATABASE IF EXISTS cnpm_project_support;

CREATE DATABASE cnpm_project_support
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON cnpm_project_support.* TO 'cnpm_user'@'localhost';

FLUSH PRIVILEGES;
```

### Bước 3

Chạy lại project:

```powershell
.\mvnw.cmd clean spring-boot:run
```

---
**Lưu ý:** Các tài khoản và dữ liệu Jira/GitHub trong tài liệu này là dữ liệu demo local. Không sử dụng chúng như thông tin xác thực cho hệ thống thật.
