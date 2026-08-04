# Data Dictionary - Thiết kế Cơ sở dữ liệu

Tài liệu này mô tả chi tiết cấu trúc các bảng trong sơ đồ ERD của hệ thống.

## 1. Bảng: `users`
**Mô tả:** Lưu trữ thông tin tài khoản của người dùng trong hệ thống.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | - | PK | - | No | Yes | Tự tăng | Khóa chính, định danh duy nhất cho người dùng |
| `username` | VARCHAR | 50 | - | - | No | Yes | - | Tên đăng nhập của người dùng |
| `password` | VARCHAR | 255 | - | - | No | No | - | Mật khẩu đã được mã hóa (hashed) |
| `email` | VARCHAR | 100 | - | - | No | Yes | - | Địa chỉ email để liên hệ và khôi phục mật khẩu |
| `created_at` | TIMESTAMP | - | - | - | No | No | CURRENT_TIMESTAMP | Thời gian tài khoản được tạo |

## 2. Bảng: `products`
**Mô tả:** Lưu trữ thông tin các mặt hàng, sản phẩm đang được bán.

| Column name | Data type | Length | Key | Reference | Nullable | Unique | Default value | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `id` | BIGINT | - | PK | - | No | Yes | Tự tăng | Khóa chính của sản phẩm |
| `product_name`| VARCHAR | 255 | - | - | No | No | - | Tên hiển thị của sản phẩm |
| `price` | DECIMAL | 10,2 | - | - | No | No | 0.00 | Giá bán của sản phẩm |
| `stock` | INT | - | - | - | No | No | 0 | Số lượng hàng còn trong kho |
| `seller_id` | BIGINT | - | FK | `users(id)` | No | No | - | Khóa ngoại trỏ tới ID của người bán |