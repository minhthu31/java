# Hướng dẫn kiểm thử Jira Integration - Sprint 3

Tài liệu này mô tả cách chạy bộ Postman `jira-sprint3-collection.json` để kiểm tra luồng cấu hình Jira, tạo Task local, đẩy Task lên Jira, đọc lại Issue Key, cập nhật Task và đồng bộ dữ liệu từ Jira.

## 1. Chuẩn bị

Import hai file sau vào Postman:

- `jira-sprint3-collection.json`
- `jira-sprint3-environment.example.json`

Tạo một bản sao environment và điền các biến:

| Biến | Ý nghĩa |
| --- | --- |
| `baseUrl` | Backend local, mặc định `http://localhost:8080` |
| `projectId` | ID project local |
| `token_admin` | JWT của Admin |
| `token_leader` | JWT của Team Leader thuộc project |
| `jira_site_url` | Jira Cloud origin dạng `https://tenant.atlassian.net` |
| `jira_project_key` | Project key Jira |
| `jira_email` | Email tài khoản Atlassian |
| `jira_api_token` | API token Atlassian; lưu ở Current value, không commit |
| `member_user_id` | ID thành viên dùng để thử cập nhật assignee |

Không ghi token thật vào collection, environment mẫu, ảnh chụp hoặc log. Backend chỉ trả trạng thái cấu hình và không trả lại API token.

## 2. Thứ tự chạy

Chạy collection theo thứ tự đã sắp xếp:

1. Admin lưu cấu hình Jira.
2. Admin đọc lại cấu hình đã che secret và kiểm tra kết nối.
3. Team Leader tạo Task local. Test script tự lưu `data.id` vào `taskId`.
4. Team Leader gọi API sync Task. Test script lưu `data.jiraIssueKey`.
5. Đọc lại Task và Jira Issue đã liên kết để xác nhận `jiraIssueKey`, URL và `syncStatus`.
6. Cập nhật status và assignee; backend đẩy thay đổi sang Jira nếu Task đã được liên kết.
7. Chạy retry khi Task đang ở `SYNC_FAILED`.
8. Kéo Project, Issue, Backlog và Sprint từ Jira bằng API `/integrations/jira/sync`.
9. Khi cần điều tra lỗi, đọc SyncLog thất bại gần nhất.

Request retry là kịch bản phục hồi lỗi. Muốn demo request này, hãy tạo một lỗi có kiểm soát (ví dụ cấu hình sai token), thực hiện sync để Task chuyển sang `SYNC_FAILED`, khôi phục cấu hình hợp lệ rồi chạy retry với một `Idempotency-Key` mới.

## 3. Kết quả mong đợi

- Response thành công có cấu trúc `{ "data": ..., "timestamp": ... }`.
- Sync Task trả `data.syncStatus = "SYNCED"` và `data.jiraIssueKey` khác rỗng.
- Chạy lại cùng một `Idempotency-Key` không tạo Issue Jira trùng.
- Lỗi Jira trả error code an toàn, correlation ID và không chứa token.
- Project sync trả số lượng Issue, Backlog, Sprint và correlation ID.

Collection dùng biến động thay vì ghi cứng Task ID. Nếu một request phụ thuộc dữ liệu chưa có, Postman sẽ báo rõ biến nào còn thiếu.
