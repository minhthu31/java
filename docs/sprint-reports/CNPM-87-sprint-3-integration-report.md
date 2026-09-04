# CNPM-87 - Báo cáo tích hợp và tổng kết Sprint 3

## 1. Mục tiêu

Xác nhận các chức năng Jira Integration của Sprint 3 hoạt động cùng nhau trên nền `main`, thống nhất contract backend/frontend, chuẩn bị luồng demo và bàn giao backlog GitHub Integration cho Sprint 4.

## 2. Phạm vi tích hợp

| Nhóm | Task | Kết quả |
| --- | --- | --- |
| Contract và persistence | CNPM-74, CNPM-75 | Đã có trên `main` |
| Secure client, DTO, config | CNPM-76 đến CNPM-79 | Đã có trên `main` |
| Task/Jira sync | CNPM-80 đến CNPM-82 | Đã có trên `main` |
| Retry, SyncLog, correlation ID | CNPM-83 | Đã có trên `main` |
| Giao diện trạng thái đồng bộ | CNPM-84 | Đã có trên `main` |
| RBAC, lỗi Jira, retry, idempotency tests | CNPM-85 | Đã có trên `main` |
| Postman và hướng dẫn | CNPM-86 | Được tích hợp và sửa contract trong CNPM-87 |

Base tích hợp được kiểm tra là commit `429a532` của `origin/main` ngày 02/09/2026.

## 3. Lỗi tích hợp đã xử lý

- Bổ sung endpoint backend `GET /api/v1/projects/{projectId}/integrations/jira/issues/{jiraIssueKey}` vốn đã được frontend sử dụng nhưng controller chưa công bố.
- Bảo vệ endpoint đọc Jira Issue cho Admin hoặc Team Leader đúng project.
- Sửa Postman dùng đúng `/integrations/jira/tasks/{taskId}/sync`, `/retry` và `/integrations/jira/sync`.
- Loại bỏ Task ID ghi cứng; collection tự lưu `data.id` và `data.jiraIssueKey`.
- Sửa test script theo response envelope thật `{ data, timestamp }`, không kiểm tra trường `success` không tồn tại.
- Bổ sung request cập nhật status, assignee, đọc Jira Issue và đọc SyncLog lỗi gần nhất.
- Thêm frontend test/build vào GitHub Actions để tránh backend xanh nhưng frontend hỏng.

## 4. Kết quả kiểm tra tự động

| Hạng mục | Kết quả |
| --- | --- |
| Backend full test | 247 tests, 0 failures, 0 errors, 0 skipped |
| Jira controller integration test | 8 tests pass, gồm endpoint đọc Jira Issue mới |
| Frontend test | 8 suites, 75 tests pass |
| Frontend production build | Thành công |
| Postman collection/environment JSON | Parse thành công |
| Git diff whitespace check | Thành công |
| Quét mẫu JWT, GitHub token và Atlassian token | Không phát hiện secret |

## 5. Kịch bản demo Sprint 3

1. Admin lưu cấu hình Jira và chạy Test Connection.
2. Team Leader tạo Task local.
3. Đẩy Task lên Jira với `Idempotency-Key` và nhận Jira Issue Key.
4. Đọc lại Task cùng Jira Issue đã liên kết.
5. Cập nhật status và assignee.
6. Tạo lỗi có kiểm soát, khôi phục cấu hình rồi chạy retry.
7. Chạy project sync để nhập Project, Issue, Backlog và Sprint.
8. Khi có lỗi, dùng correlation ID và SyncLog để truy vết.

Bộ request và hướng dẫn nằm trong `docs/postman`. Lượt demo Jira Cloud thật phải được người vận hành chạy bằng environment riêng có token hợp lệ; token không được đưa vào repository.

## 6. Technical debt và rủi ro còn lại

- `Retry-After` lớn hiện có thể giữ request thread chờ lâu; Sprint sau nên chuyển retry dài sang cơ chế bất đồng bộ.
- Test dùng H2 2.4 mới hơn phiên bản Flyway đã xác minh và sinh cảnh báo, chưa làm test thất bại.
- Một số dependency frontend cũ phát cảnh báo deprecated; xử lý sau khi hoàn thành luồng nghiệp vụ chính.
- Demo live phụ thuộc tài khoản Atlassian, Jira project, quyền tạo/chuyển trạng thái Issue và mapping assignee hợp lệ.

## 7. Sprint 4 backlog

Backlog GitHub Integration đã được tạo từ CNPM-88 đến CNPM-101. CNPM-88 là nguồn contract chung và phải hoàn thành trước các task persistence, REST client, API, giao diện và đồng bộ GitHub.

## 8. Kết luận

Source code, test, build, collection và tài liệu của Sprint 3 đã sẵn sàng review. CNPM-87 chỉ chuyển `Done` sau khi PR được merge và nhóm chạy thành công kịch bản live bằng Jira Cloud thật; nếu live demo phát hiện lỗi, ghi rõ lỗi và người xử lý thay vì bỏ qua bước nghiệm thu.
