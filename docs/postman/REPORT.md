# KẾT QUẢ BÀN GIAO VÀ KIỂM TRẢ TIÊU CHÍ (ACCEPTANCE CRITERIA)

## 1. Issue ID và Issue Key của Issue thử nghiệm
* **Project Key:** `CNPM`
* **Issue ID:** `10045`
* **Issue Key:** `CNPM-30`
* **HTTP Status Code:** `201 Created`

- [x] **Nhận được Issue ID và Issue Key.**

---

## 2. JSON Response kết quả request
```json
{
  "id": "10045",
  "key": "CNPM-30",
  "self": "[https://ut-team-cg81y9ob.atlassian.net/rest/api/3/issue/10045](https://ut-team-cg81y9ob.atlassian.net/rest/api/3/issue/10045)"
}
# BÁO CÁO VÀ HƯỚNG DẪN KIỂM THỬ API SPRINT 2 (CNPM-70)

Tài liệu này cung cấp hướng dẫn thiết lập dữ liệu đầu vào và thực thi kiểm thử tự động cho toàn bộ API CRUD của Requirement, Task cùng các kịch bản bảo mật và xác thực dữ liệu.

---

## 1. Hướng dẫn Import Postman Collection
1. Mở phần mềm **Postman**.
2. Nhấn nút **Import** (hoặc tổ hợp phím `Ctrl + O`).
3. Chọn file `docs/postman/sprint2-api-collection.json` từ dự án để nạp vào Postman.

---

## 2. Thiết lập Biến môi trường (Environment Variables)
Để tuân thủ tiêu chí bảo mật, **không lưu trữ mật khẩu hay Token trực tiếp trong mã nguồn**. Hãy cấu hình các biến trong Postman (Tab *Variables* của Collection hoặc tạo mới một *Environment*):

| Tên biến (Variable) | Ý nghĩa | Giá trị mẫu (Local) |
| :--- | :--- | :--- |
| `baseUrl` | Đường dẫn máy chủ Backend | `http://localhost:8080` |
| `projectId` | ID của Project cần test | `1` |
| `token_leader` | JWT Token của tài khoản có role `TEAM_LEADER` | *(Lấy từ API Login)* |
| `token_member` | JWT Token của tài khoản có role `TEAM_MEMBER` | *(Lấy từ API Login)* |

---

## 3. Danh mục các kịch bản kiểm thử (Test Scenarios)

### A. Requirement CRUD
* **Create Requirement (POST `/api/v1/projects/{projectId}/requirements`):** Tạo mới Requirement với tiêu đề, actor và độ ưu tiên.
* **Get Requirements (GET `/api/v1/projects/{projectId}/requirements`):** Lấy danh sách Requirement có phân trang và lọc.

### B. Task CRUD
* **Create Task (POST `/api/v1/projects/{projectId}/tasks`):** Tạo mới Task (Yêu cầu quyền `TEAM_LEADER`).
* **Get Tasks (GET `/api/v1/projects/{projectId}/tasks`):** Lấy danh sách Task theo Project.
* **Update Task Status (PATCH `/api/v1/projects/{projectId}/tasks/{taskId}/status`):** Chuyển trạng thái Task sang `IN_PROGRESS` hoặc `DONE`.
* **Delete Task (DELETE `/api/v1/projects/{projectId}/tasks/{taskId}`):** Xóa Task theo ID (Yêu cầu quyền `TEAM_LEADER`).

### C. Kịch bản Bảo mật & Validation (Security & Exception Tests)
* **401 Unauthorized:** Gửi request mà không truyền Bearer Token.
* **403 Forbidden:** Dùng `token_member` (Role `TEAM_MEMBER`) để gọi API Xóa Task vốn chỉ dành cho `TEAM_LEADER`.
* **400 Bad Request (Validation):** Cố tình để trống trường `title` khi tạo Task/Requirement để kiểm tra việc bắt lỗi `@NotBlank`.

---

## 4. Hướng dẫn chạy kiểm thử tự động
1. Nhấp chuột phải vào Collection **CNPM Sprint 2 API Tests** trong Postman.
2. Chọn **Run collection**.
3. Đảm bảo các biến môi trường đã có giá trị Token hợp lệ.
4. Bấm **Run CNPM Sprint 2 API Tests** để kiểm tra kết quả toàn bộ API.