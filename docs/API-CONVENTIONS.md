# API conventions

- Base path: `/api/v1`.
- JSON dùng `camelCase`; database dùng `snake_case`.
- Thời gian trao đổi qua API dùng ISO-8601 UTC.
- Phân trang dùng `page`, `size`, `sort`; giới hạn `size` tối đa tại backend.
- Response thành công dùng `ApiResponse<T>` khi cần envelope thống nhất.
- Lỗi dùng `ApiError`: `code`, `message`, `correlationId`, `fieldErrors`, `timestamp`.
- Không gửi JPA entity hoặc encrypted secret ra response.
- `401`: chưa xác thực/token không hợp lệ; `403`: đã xác thực nhưng vượt quyền.
- Request tạo tài nguyên đồng bộ Jira phải hỗ trợ idempotency key.
- Header `X-Correlation-ID` được nhận hoặc tự sinh và trả lại client.
