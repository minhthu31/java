# Hướng dẫn chia việc backend

## Thứ tự triển khai đề xuất

1. Identity + Group: user, role, membership, Lecturer/Leader assignment.
2. Auth + RBAC: login/logout, password hash, API authorization và data scope theo group.
3. Project + Requirement + Task: CRUD, assignment, classification và workflow.
4. Jira integration: encrypted config, connection test, issue sync và idempotent retry.
5. GitHub integration + Linking: repository, commit, PR, workflow, verified Issue Key.
6. SRS + Reporting + Audit: version/export, progress, contribution và timeline.

## Mẫu cấu trúc một module

```text
task/
├── controller/TaskController.java
├── dto/CreateTaskRequest.java
├── dto/TaskResponse.java
├── domain/Task.java
├── domain/TaskStatus.java
├── repository/TaskRepository.java
├── service/TaskService.java
└── service/TaskPolicy.java
```

## Definition of Done cho code

- Request có validation và response dùng DTO.
- Quyền được kiểm tra ở backend, bao gồm phạm vi nhóm.
- Service có unit test; endpoint quan trọng có integration/security test.
- Thay đổi database đi qua migration Flyway mới.
- Lỗi trả theo cấu trúc `ApiError` và có correlation ID.
- Không chứa secret, stack trace hoặc file không liên quan.
- Branch, commit và PR chứa Jira Issue Key.
