# CNPM-75 - Persistence cho Jira Integration

## 1. Phạm vi

Hiện thực tầng lưu trữ cho ba bảng Jira Integration đã được thiết kế trong
Flyway V1:

- `integration_configs` → `IntegrationConfig`
- `jira_issues` → `JiraIssue`
- `sync_logs` → `SyncLog`

Entity chỉ ánh xạ schema và cung cấp repository query. Jira HTTP client, mã hóa
secret, API controller và nghiệp vụ retry thuộc các task Sprint 3 khác.

## 2. Kết quả kiểm tra schema

| Yêu cầu | Flyway hiện tại | Quyết định |
|---|---|---|
| Một config trên mỗi `project + provider` | `uk_project_provider_config` | Dùng lại, không tạo migration |
| Một Task chỉ map một Jira Issue | `jira_issues.task_id UNIQUE` | Dùng lại, không tạo migration |
| Jira Issue ID không trùng | `jira_issue_id UNIQUE` | Dùng lại, không tạo migration |
| Jira Issue Key không trùng | `jira_issue_key UNIQUE` | Dùng lại, không tạo migration |
| Lịch sử theo project và status | `idx_sync_project_status` | Dùng lại, không tạo migration |
| Lịch sử theo correlation ID | Có cột `correlation_id`; query theo project scope | Chưa cần index riêng với quy mô Sprint 3 |

Không sửa `V1__create_core_schema.sql` vì đây là migration đã dùng chung. Không
có cột hoặc constraint bắt buộc nào bị thiếu, vì vậy CNPM-75 **không tạo migration
mới**. ERD và Data Dictionary vẫn khớp schema nên cũng không cần sửa.

## 3. Entity

### IntegrationConfig

- Dùng `BaseEntity` cho `id`, `createdAt`, `updatedAt`.
- Provider là `JIRA` hoặc `GITHUB`.
- Trạng thái cấu hình: `NOT_CHECKED`, `CONNECTED`, `CONNECTION_FAILED`.
- `encryptedSecret` là ciphertext nội bộ; không thêm `toString` và không dùng
  entity làm API response. Getter được đánh dấu `@JsonIgnore` để tránh lộ secret
  nếu entity vô tình đi qua JSON serializer.

### JiraIssue

- Lưu mapping duy nhất giữa `taskId`, `jiraIssueId` và `jiraIssueKey`.
- Lưu URL, thời điểm Jira cập nhật, lần sync thành công cuối và snapshot hash.
- `rawSnapshot` dùng Hibernate JSON mapping vào `Map<String, Object>`.

### SyncLog

- Provider: `JIRA` hoặc `GITHUB`.
- Direction: `IMPORT` hoặc `EXPORT`.
- Trạng thái execution: `RUNNING`, `SUCCESS`, `FAILED`.
- Lưu retry count, lỗi đã sanitize, correlation ID và thời gian chạy.
- `SyncLogStatus` là trạng thái của một lần chạy, không dùng thay
  `Task.syncStatus`.

## 4. Repository contract

### IntegrationConfigRepository

```text
findByProjectIdAndProvider(projectId, provider)
existsByProjectIdAndProvider(projectId, provider)
```

### JiraIssueRepository

```text
findByTaskId(taskId)
findByJiraIssueId(jiraIssueId)
findByJiraIssueKey(jiraIssueKey)
```

### SyncLogRepository

```text
findByProjectIdOrderByStartedAtDesc(projectId, pageable)
findByProjectIdAndStatusOrderByStartedAtDesc(projectId, status, pageable)
findByProjectIdAndCorrelationIdOrderByStartedAtDesc(projectId, correlationId)
findByProjectIdAndStatusAndCorrelationIdOrderByStartedAtDesc(
    projectId, status, correlationId)
```

Mọi truy vấn lịch sử đều có `projectId` để tránh đọc chéo project.

## 5. Bằng chứng kiểm thử

`JiraPersistenceRepositoryTests` chạy bằng Spring Data JPA cùng profile `test` và
schema do Flyway tạo. Bộ test kiểm tra:

- lưu và tìm config theo project/provider;
- constraint không cho hai config cùng project/provider;
- lưu JSON snapshot và tìm Jira Issue theo Task ID, Issue ID hoặc Issue Key;
- ba constraint chống trùng Task mapping, Issue ID và Issue Key;
- lọc/sắp xếp SyncLog theo project, status và correlation ID;
- dữ liệu project khác không lọt vào kết quả.

Kết quả chạy toàn bộ backend test: **119/119 test đạt**, không có failure hoặc
error. Riêng `JiraPersistenceRepositoryTests` có **8/8 test đạt**.

## 6. Acceptance checklist

- [x] Có entity và repository cho `IntegrationConfig`, `JiraIssue`, `SyncLog`.
- [x] Cấu hình Jira duy nhất theo project/provider.
- [x] Jira Issue ID, Issue Key và Task mapping không bị trùng.
- [x] Có truy vấn lấy cấu hình theo project/provider.
- [x] Có truy vấn tìm Jira Issue theo Task ID hoặc Issue Key.
- [x] Có truy vấn lịch sử đồng bộ theo project, status và correlation ID.
- [x] Không sửa Flyway V1; schema hiện tại không cần migration mới.
- [x] Repository test chạy trên schema Flyway.
- [x] ERD/Data Dictionary không đổi vì schema không thay đổi.
