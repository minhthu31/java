# Hướng dẫn Kiểm thử Jira Integration - Sprint 3 (CNPM-86)

Tài liệu này cung cấp hướng dẫn thiết lập môi trường và chạy tự động Postman Collection để kiểm thử toàn bộ luồng tích hợp API Jira trong Sprint 3.

## 1. Thiết lập Môi trường (Environment)
Để đáp ứng tiêu chí bảo mật, tuyệt đối **không lưu hard-code** thông tin nhạy cảm vào file Collection. Hãy sử dụng tính năng Environment của Postman và cấu hình các biến sau:

| Tên biến | Ý nghĩa | Dữ liệu mẫu |
| :--- | :--- | :--- |
| `baseUrl` | Đường dẫn server local | `http://localhost:8080` |
| `projectId` | ID của Project đang thực hiện tích hợp | `1` |
| `token_admin` | JWT Token của user có quyền `ADMIN` | `eyJhbG...` |
| `token_leader`| JWT Token của user có quyền `TEAM_LEADER` | `eyJhbG...` |
| `jira_site_url`| URL gốc của Jira | `https://your-domain.atlassian.net` |
| `jira_project_key`| Mã Key của project trên Jira | `CNPM` |
| `jira_email` | Email tài khoản quản trị Jira | `admin@example.com` |
| `jira_api_token`| API Token được tạo từ hệ thống Atlassian | `ATATT3xFfGF0...` |
| `taskId` | ID của Task (Tự động lấy khi chạy request Tạo Task) | *(Tự động điền)* |

## 2. Kịch bản Kiểm thử (Test Scenarios)
Bộ Collection chạy nối tiếp nhau theo đúng thứ tự logic hệ thống:

1. **Lưu/Cập nhật cấu hình Jira (PUT `/config`):** Đẩy thông tin site, email và token lên hệ thống.
2. **Test kết nối (POST `/test-connection`):** Xác thực độ chính xác của cấu hình và Token.
3. **Tạo Task Local (POST `/tasks`):** Khởi tạo Task mới lưu vào database nội bộ. *(Lưu ý: Tạo Task local chưa đồng nghĩa với việc đã đẩy sang Jira)*. ID của task sẽ tự động được lưu vào biến `{{taskId}}`.
4. **Cập nhật Status / Assignee (PATCH):** Thay đổi trạng thái hoặc gán người phụ trách cho Task local.
5. **Đồng bộ 1 Task lên Jira (POST `/tasks/{{taskId}}/sync`):** Đẩy chủ động một Task cụ thể sang Jira.
6. **Retry đồng bộ Task (POST `/tasks/{{taskId}}/retry`):** Gọi lệnh thử lại kèm `Idempotency-Key` đối với Task bị lỗi đồng bộ.
7. **Đồng bộ toàn bộ dữ liệu (POST `/sync`):** Kéo toàn bộ Issue, Backlog, Sprint từ Jira về hệ thống.

## 3. Chạy Kiểm thử Tự động
Các request đã tích hợp Test Scripts tự động kiểm tra HTTP Status Code (200, 201) và tự động gán biến.
* Nhấp chuột phải vào tên Collection -> Chọn **Run collection** -> Bấm **Run**.