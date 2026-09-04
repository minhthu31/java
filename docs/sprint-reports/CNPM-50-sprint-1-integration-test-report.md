# CNPM-50 - Báo cáo tích hợp và kiểm tra kết quả Sprint 1

## 1. Mục tiêu

Xác nhận các phần việc Sprint 1 có thể hoạt động cùng nhau trên cùng nhánh `main`, bao gồm Spring Boot, MySQL/Flyway, xác thực - phân quyền và giao diện React.

## 2. Phạm vi kiểm tra

| Nhóm chức năng | Nội dung |
|---|---|
| Project foundation | Java 21, Spring Boot, Maven Wrapper, cấu trúc package |
| Database foundation | Kết nối MySQL, Flyway migration, role và tài khoản kiểm thử |
| Authentication | BCrypt, AuthenticationService, Login API, JWT |
| Authorization | Bảo vệ endpoint và điều hướng theo 4 vai trò |
| Frontend | Form đăng nhập do HM thiết kế và trang đích theo vai trò |
| Documentation | Architecture, ERD, Data Dictionary và hướng dẫn phát triển |

## 3. Môi trường nghiệm thu

- Hệ điều hành: Windows.
- Backend: Java 21, Spring Boot 4.1.0, Maven.
- Frontend: React, Node.js và npm.
- Database: MySQL Community Server 8.4, schema `cnpm_project_support`.
- Backend URL: `http://localhost:8080`.
- Frontend URL: `http://localhost:3000`.

Không ghi mật khẩu database, JWT secret hoặc integration encryption key vào tài liệu và Git.

## 4. Kết quả kiểm tra

| ID | Trường hợp kiểm tra | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| INT-01 | Biên dịch và chạy backend | Spring Boot khởi động, không lỗi migration | Đạt |
| INT-02 | `GET /actuator/health` | HTTP 200, `status=UP` | Đạt |
| INT-03 | Flyway trên database mới | Tạo đủ schema và dữ liệu nền | Đạt |
| INT-04 | Đăng nhập bằng username/email hợp lệ | Trả thông tin người dùng, role và access token | Đạt |
| INT-05 | Sai mật khẩu hoặc user không tồn tại | Từ chối, không lộ password/stack trace | Đạt |
| INT-06 | Tài khoản inactive | Bị từ chối đăng nhập | Đạt theo unit test |
| INT-07 | Truy cập endpoint bảo vệ khi chưa đăng nhập | HTTP 401 | Đạt |
| INT-08 | Truy cập sai vai trò | HTTP 403 | Đạt |
| INT-09 | Đăng nhập từ React | Gọi đúng `POST /api/v1/auth/login` | Đạt |
| INT-10 | Điều hướng theo role | Admin/Lecturer/Leader/Member vào đúng trang | Đạt |
| INT-11 | Giao diện đăng nhập | Giữ nguyên thiết kế của HM sau tích hợp | Đạt |
| INT-12 | Frontend production build | `npm run build` biên dịch thành công | Đạt ngày 14/08/2026 |

Backend đã có 12 automated tests cho application context, authentication, JWT, database contract và Spring Security. Lần kiểm tra tích hợp trước ghi nhận `12 tests`, `0 failures`, `0 errors`, `0 skipped`.

Trong lần rà soát tài liệu ngày 14/08/2026, backend vẫn đang chạy trong IntelliJ nên Windows khóa thư mục `target` và một số thư viện đang được tiến trình Java sử dụng. Vì vậy lệnh `clean verify` ngoài IntelliJ không được dùng làm bằng chứng mới; bằng chứng runtime, test trước đó và frontend build được giữ tách biệt, không ghi nhận sai thành build failure của source code.

## 5. Đối chiếu các task Sprint 1

| Task | Sản phẩm | Trạng thái nghiệm thu |
|---|---|---|
| CNPM-18 | Architecture | Sẵn sàng review trong PR tài liệu riêng |
| CNPM-19 | ERD | Sẵn sàng review trong PR tài liệu riêng |
| CNPM-20 | Data Dictionary | Sẵn sàng review trong PR tài liệu riêng |
| CNPM-21 | Spring Boot skeleton | Đạt |
| CNPM-35 | Spring Boot - MySQL connection | Đạt |
| CNPM-36 | Initial Flyway migration | Đạt |
| CNPM-37 | User/Role entity | Đạt |
| CNPM-38 | User/Role repository | Đạt |
| CNPM-39 | Test accounts | Đạt |
| CNPM-40 | BCrypt configuration | Đạt |
| CNPM-41 | Authentication service | Đạt |
| CNPM-42 | Spring Security | Đạt |
| CNPM-43 | Login API contract/DTO | Đạt sau tích hợp |
| CNPM-44 | Login controller | Đạt sau tích hợp |
| CNPM-45 | Validation/exception | Đạt |
| CNPM-46 | Role-based authorization | Đạt |
| CNPM-47 | Login UI | Đạt, giữ thiết kế HM |
| CNPM-48 | Role landing pages | Đạt |
| CNPM-49 | Authentication/authorization tests | Đạt |

## 6. Vấn đề đã xử lý

- Đồng nhất endpoint đăng nhập thành `POST /api/v1/auth/login`.
- Sửa CORS cho frontend local.
- Sửa giao dịch khi xác thực và truy cập dữ liệu lazy-loaded.
- Đồng nhất BCrypt hash của tài khoản kiểm thử.
- Khôi phục giao diện đăng nhập của HM sau quá trình tích hợp.
- Hoàn thiện tài liệu Architecture, ERD và Data Dictionary theo schema hiện tại.

## 7. Kết luận

Sprint 1 đạt mục tiêu foundation và authentication. CNPM-50 đủ điều kiện chuyển `Done` sau khi:

1. PR tài liệu CNPM-18/19/20 được merge vào `main`.
2. PR báo cáo CNPM-50/51 được review và merge.
3. Không còn work item Sprint 1 ở trạng thái `To Do` hoặc `In Progress`; task chưa đạt phải chuyển rõ sang Sprint 2.

