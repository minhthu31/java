# Hướng dẫn cấu hình kết nối MySQL (Local)

Để chạy được project, bạn cần tạo Database local và thiết lập biến môi trường. **Tuyệt đối không hardcode mật khẩu vào file application.properties**.

## 1. Tạo Database
Mở công cụ quản lý MySQL (Workbench/DBeaver) hoặc Terminal và chạy lệnh SQL sau:
`CREATE DATABASE cnpm_project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`

## 2. Thiết lập Biến môi trường (Environment Variables)
Khi chạy project trong IntelliJ IDEA hoặc Eclipse, bạn cần thêm 2 biến môi trường vào cấu hình chạy (Run Configuration):
- `DB_USERNAME`: Tên đăng nhập MySQL của máy bạn (thường là root)
- `DB_PASSWORD`: Mật khẩu MySQL của máy bạn

Nếu chạy bằng Terminal/CMD:
`set DB_USERNAME=root`
`set DB_PASSWORD=mat_khau_cua_ban`
`mvn spring-boot:run` 

# Hướng dẫn thiết lập Local Database & Môi trường chạy dự án (CNPM-35)

Tài liệu này hướng dẫn các thành viên cách tạo cơ sở dữ liệu local và cấu hình dự án Spring Boot để chạy trên máy cá nhân. 

⚠️ **QUY TẮC BẢO MẬT:** Để tuân thủ quy tắc bảo mật của hệ thống, tuyệt đối **KHÔNG** gõ trực tiếp tên đăng nhập và mật khẩu MySQL vào file `application.properties`. Chúng ta sẽ sử dụng Biến môi trường (Environment Variables).

---

## Bước 1: Khởi tạo Database Local

Bạn cần có một database trống để Spring Boot (Hibernate) tự động tạo các bảng dữ liệu.

1. Mở hệ quản trị cơ sở dữ liệu của bạn (MySQL Workbench, DBeaver, Navicat, hoặc phpMyAdmin trong XAMPP).
2. Mở một query tab mới và chạy câu lệnh SQL sau:

```sql
CREATE DATABASE cnpm_project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;