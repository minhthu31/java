# THIẾT LẬP JIRA PROJECT, WORKFLOW VÀ SPRINT 0

## 1. Thông tin project

- Project name: Công cụ quản lý dự án CNPM
- Project key: CNPM
- Project type: Scrum
- Management type: Team-managed
- Main features: Backlog, Board, Sprint và Timeline

## 2. Thành viên và quyền truy cập

Nhóm gồm 7 thành viên:

- Team Leader: Administrator
- Các thành viên còn lại: Member

Administrator chịu trách nhiệm:

- Cấu hình project.
- Quản lý quyền truy cập.
- Tạo Sprint.
- Tạo Epic và Task.
- Phân công công việc.
- Kiểm tra tiến độ nhóm.

Member có trách nhiệm:

- Xem task được giao.
- Cập nhật trạng thái task.
- Bình luận tiến độ.
- Thực hiện công việc trên GitHub.

## 3. Sprint 0

- Sprint name: Sprint 0 – Analysis and Project Setup
- Sprint goal: Hoàn thành phân tích yêu cầu, thiết kế hệ thống
  ban đầu, nghiên cứu Jira/GitHub API và thiết lập môi trường dự án.

Kết quả cần đạt:

- Project Scope.
- Actor và Permission Matrix.
- Functional Requirements.
- Non-functional Requirements.
- Use Case.
- SRS v1.
- Wireframe.
- Architecture.
- ERD.
- Jira API PoC.
- GitHub API PoC.
- Project Skeleton.

## 4. Các Epic

### CNPM-1 – Requirements Analysis

Chứa các task liên quan đến:

- Project Scope.
- Actor.
- Functional Requirements.
- Non-functional Requirements.
- Use Case.
- SRS.

### CNPM-2 – System Design

Chứa:

- Architecture.
- ERD.
- Data Dictionary.
- Sitemap.
- User Flow.
- Wireframe.

### CNPM-3 – Integration Research

Chứa:

- Jira REST API.
- GitHub REST API.
- Jira PoC.
- GitHub PoC.
- Quy ước liên kết task và commit.

### CNPM-4 – Project Setup

Chứa:

- Jira setup.
- GitHub setup.
- Development workflow.
- Spring Boot skeleton.
- Sprint 1 backlog.

## 5. Workflow

Workflow của nhóm:

```text
To Do
  ↓
In Progress
  ↓
In Review
  ↓
Done
```
### 6.Quy tắc task
Mỗi task phải có:
- Summary rõ ràng.
- Description.
- Assignee.
- Parent Epic.
- Priority.
- Due date.
- Acceptance Criteria.
- Sprint.
- Deliverable.
### 7. Quy tắc kết hợp Jira và GitHub
Mỗi task sử dụng Jira Issue Key trong branch, commit và Pull Request.
Jira task:
CNPM-15

Branch:
feature/CNPM-15-login-api

Commit:
CNPM-15 Implement login API

Pull Request:
CNPM-15 Complete login API
Quy trình:
Nhận task trên Jira
- Chuyển In Progress
- Tạo branch GitHub
- Thực hiện và commit
- Tạo Pull Request
- Chuyển Jira sang In Review
- Review và merge
- Chuyển Jira sang Done
### 8. Kết quả thiết lập
- Scrum project đã được tạo.
- Project key CNPM đã được thiết lập.
- Sprint 0 đã được tạo.
- Bốn Epic đã được tạo.
- Thành viên đã được thêm vào project.
- Task Sprint 0 đã được tạo và phân công.
- Workflow đã được thống nhất.
- Quy trình Jira–GitHub đã được xác định.
