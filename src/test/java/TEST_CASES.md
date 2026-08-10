# Tài Liệu Kiểm Thử Chức Năng Đăng Nhập & Phân Quyền (Auth & RBAC Test Suite)

## 1. Danh Sách Test Cases & Expected Results

| Mã TC | Tên Test Case | Đầu Vào (Input) | Kết Quả Mong Đợi (Expected Result) | Trạng Thái Mong Đợi |
| :--- | :--- | :--- | :--- | :---: |
| **TC01** | Đăng nhập thành công | Username & Password hợp lệ | Trả về thông báo thành công và kèm JWT Token | `200 OK` |
| **TC02** | Sai mật khẩu | Mật khẩu không đúng | Từ chối đăng nhập, báo lỗi xác thực | `401 Unauthorized` |
| **TC03** | Tài khoản không tồn tại | Username chưa đăng ký hệ thống | Từ chối đăng nhập | `401 Unauthorized` |
| **TC04** | Tài khoản inactive | Tài khoản bị khóa / chưa kích hoạt | Từ chối đăng nhập do tài khoản không hoạt động | `403 Forbidden` |
| **TC05** | Thiếu username/email | Bỏ trống trường username | Báo lỗi thiếu dữ liệu đầu vào | `400 Bad Request` |
| **TC06** | Thiếu password | Bỏ trống trường password | Báo lỗi thiếu dữ liệu đầu vào | `400 Bad Request` |
| **TC07** | Truy cập khi chưa đăng nhập | Không đính kèm Bearer Token | Từ chối truy cập endpoint bảo vệ | `401 Unauthorized` |
| **TC08** | Đúng role truy cập thành công | Token có Role `ADMIN` truy cập `/api/admin/dashboard` | Truy cập tài nguyên thành công | `200 OK` |
| **TC09** | Sai role nhận lỗi 403 | Token có Role `STUDENT` truy cập `/api/admin/dashboard` | Từ chối truy cập do không đủ thẩm quyền | `403 Forbidden` |

---

## 2. Kiểm Trả Tiêu Chí Chấp Nhận (Acceptance Criteria Check)

- [x] Có automated test (Dùng JUnit 5 + MockMvc Spring Boot).
- [x] Có mô tả Expected Result rõ ràng cho từng test case.
- [x] Không chứa tài khoản, mật khẩu hoặc token thật (chỉ sử dụng mock data).
