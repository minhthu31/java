# Backend Spring Boot - Quản lý công việc (Jira/GitHub Integration)

Đây là mã nguồn Backend (Spring Boot) cung cấp REST API cho hệ thống, tích hợp với Jira Cloud và GitHub.

## 1. Yêu cầu hệ thống (Prerequisites)
- **Java:** JDK 17
- **Database:** MySQL 8.0+
- **Công cụ build:** Maven

## 2. Cấu trúc thư mục (Architecture)
Dự án áp dụng kiến trúc phân tầng chuẩn (`com.java.backend.*`):
- `controller/`: Chứa các REST API Endpoints tiếp nhận Request.
- `service/`: Chứa logic nghiệp vụ (Business Logic).
- `repository/`: Tương tác với cơ sở dữ liệu (Spring Data JPA).
- `entity/`: Các đối tượng ánh xạ với bảng MySQL.
- `dto/`: Đối tượng truyền tải dữ liệu giữa các tầng.
- `integration/`: Xử lý giao tiếp với External APIs (Jira, GitHub).
- `security/`: Xử lý xác thực (Spring Security) và phân quyền.
- `config/`: Chứa các class cấu hình hệ thống.

## 3. Hướng dẫn cấu hình và chạy dự án (Getting Started)

### Bước 3.1: Khởi tạo Cơ sở dữ liệu (Database)
Bạn mở công cụ quản lý MySQL (như MySQL Workbench, XAMPP hoặc Terminal) và chạy lệnh SQL sau để tạo database cho dự án:
```sql
CREATE DATABASE java_db;
```

### Bước 3.2: Cấu hình biến môi trường (Environment Variables)
Để bảo mật thông tin, dự án không lưu trực tiếp mật khẩu database vào code. Trước khi chạy ứng dụng, bạn cần cung cấp các thông tin này thông qua cấu hình biến môi trường trên máy tính hoặc trong phần mềm IDE (IntelliJ/Eclipse/VS Code):

- `DB_URL`: `jdbc:mysql://localhost:3306/java_db?useSSL=false&serverTimezone=UTC` (Giá trị mặc định)
- `DB_USERNAME`: Tên đăng nhập MySQL của bạn (Mặc định thường là: `root`)
- `DB_PASSWORD`: Mật khẩu MySQL của bạn

*Lưu ý: Nếu không cấu hình các biến này, hệ thống sẽ tự động lấy thông tin mặc định là tài khoản `root` và mật khẩu `root`.*

### Bước 3.3: Build và Khởi chạy ứng dụng
Mở Terminal/Command Prompt tại thư mục gốc của dự án và chạy lệnh Maven sau:
```bash
mvn spring-boot:run
```
Nếu Terminal hiển thị biểu tượng Spring Boot và dòng chữ `Started BackendApplication in ... seconds`, hệ thống backend đã khởi chạy thành công tại địa chỉ: `http://localhost:8080`