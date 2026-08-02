# CNPM Project Management Tool

## Giới thiệu

Công cụ hỗ trợ quản lý yêu cầu và tiến độ dự án phần mềm
thông qua việc tích hợp Jira và GitHub.

## Chức năng chính

- Quản lý nhóm sinh viên và Lecturer.
- Quản lý requirement và hỗ trợ tạo SRS.
- Tạo và giao task trên website.
- Đồng bộ task và tiến độ với Jira.
- Lấy commit và Pull Request từ GitHub.
- Liên kết Jira task với GitHub commit.
- Tổng hợp báo cáo tiến độ và mức độ đóng góp.

## Công nghệ dự kiến

- Java Spring Boot
- MySQL
- Jira Cloud REST API
- GitHub REST API

## Quy tắc làm việc

Mọi branch, commit và Pull Request phải chứa Jira Issue Key.

Ví dụ:

- Branch: `feature/CNPM-30-login-api`
- Commit: `CNPM-30 Implement login API`
- Pull Request: `CNPM-30 Complete login feature`

Chi tiết quy trình xem tại:

- `docs/development-workflow.md`
