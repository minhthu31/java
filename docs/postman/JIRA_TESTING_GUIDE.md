# Hướng dẫn Kiểm thử Jira Integration - Sprint 3 (CNPM-86)

Tài liệu này cung cấp hướng dẫn thiết lập môi trường và chạy tự động Postman Collection để kiểm thử toàn bộ luồng tích hợp API Jira trong Sprint 3.

## 1. Thiết lập Môi trường (Environment)
Để đáp ứng tiêu chí bảo mật, tuyệt đối **không lưu hard-code** `apiToken` hay thông tin nhạy cảm vào file Collection. Hãy sử dụng tính năng Environment của Postman và cấu hình các biến sau:

| Tên biến | Kiểu dữ liệu | Ý nghĩa & Dữ liệu mẫu |
| :--- | :--- | :--- |
| `baseUrl` | `string` | Đường dẫn server local (VD: `http://localhost:8080`) |
| `projectId` | `number` | ID của Project đang thực hiện tích hợp (VD: `1`) |
| `token_admin` | `string` | JWT Token của user có quyền `ADMIN` |
| `token_leader` | `string` | JWT Token của user có quyền `TEAM_LEADER` |
| `jira_site_url` | `string` | URL gốc của Jira (VD: `https://your-domain.atlassian.net`) |
| `jira_project_key` | `string` | Mã Key của project trên Jira (VD: `CNPM`) |
| `jira_email` | `string` | Email tài khoản quản trị Jira (VD: `admin@example.com`) |
| `jira_api_token` | `string` | API Token được tạo từ hệ thống Atlassian |

## 2. Kịch bản Kiểm thử (Test Scenarios)
Bộ Collection được thiết kế để chạy nối tiếp nhau theo đúng thứ tự logic của hệ thống:

1. **Lưu/Cập nhật cấu hình Jira (PUT `/config`):** Đẩy thông tin site, email và token lên hệ thống. Yêu cầu quyền ADMIN.
2. **Test kết nối (POST `/test-connection`):** Backend gọi sang Jira để xác thực độ chính xác của cấu hình và Token.
3. **Tạo Task (POST `/tasks`):** Khởi tạo Task mới, đi kèm header `Idempotency-Key` để trigger cơ chế tạo chống trùng lặp và đồng bộ sang Jira.
4. **Cập nhật Status/Assignee (PATCH):** Chuyển trạng thái Task hoặc thay đổi người phụ trách, kích hoạt đồng bộ 2 chiều.
5. **Retry đồng bộ Task (POST `/retry`):** Gọi lệnh thử lại (retry) đối với các Task bị lỗi trong quá trình đẩy lên Jira trước đó.
6. **Đồng bộ Issue/Backlog (POST `/sync-issues`):** Chủ động kéo các danh sách Issue, Backlog và Sprint hiện có từ Jira về hệ thống nội bộ.

## 3. Chạy Kiểm thử Tự động & Kết quả Mẫu
Mọi request trong bộ Collection này đều đã được tích hợp sẵn kịch bản kiểm tra (Test Scripts) để tự động đối chiếu:
*   Đảm bảo HTTP Status Code trả về đúng chuẩn (200 OK, 201 Created).
*   Đảm bảo cấu trúc Response bắt buộc chứa `success = true` (đối với các API tích hợp).

**Cách thực thi:**
1. Nhấp chuột phải vào tên Collection `CNPM Sprint 3 - Jira Integration` ở cột trái.
2. Chọn **Run collection**.
3. Đảm bảo đã chọn đúng Environment chứa các biến token.
4. Bấm **Run** và theo dõi các test case báo xanh (Pass) trên màn hình kết quả.