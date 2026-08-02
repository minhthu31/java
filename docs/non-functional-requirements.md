**# Non-functional Requirements

**## Giới thiệu

Tài liệu này mô tả các yêu cầu phi chức năng (Non-functional Requirements) của hệ thống quản lý dự án tích hợp Jira và GitHub. Các yêu cầu này quy định về chất lượng, hiệu năng, bảo mật, khả năng mở rộng và các tiêu chuẩn mà hệ thống phải đáp ứng trong quá trình vận hành.

---

**Mã yêu cầu** NFR-01

**Tên yêu cầu:** Security

**Nhóm:** Security

**Mô tả**

Hệ thống phải đảm bảo tính bảo mật cho toàn bộ dữ liệu và tài khoản người dùng. Mỗi người dùng phải được xác thực trước khi sử dụng hệ thống và chỉ được phép truy cập các chức năng đúng với vai trò của mình.

Các quyền phải được kiểm soát theo Actor Permission Matrix:
- Admin có toàn quyền quản trị hệ thống.
- Lecturer chỉ được xem thông tin của các nhóm phụ trách.
- Team Leader được quản lý Requirement, Task và thành viên trong nhóm.
- Team Member chỉ được thao tác trên các Task được giao.

Mật khẩu không được lưu dưới dạng văn bản thuần (Plain Text).
Phiên đăng nhập phải hết hạn sau một khoảng thời gian không hoạt động.
Các API tích hợp với GitHub và Jira phải sử dụng Access Token hoặc OAuth.

**Tiêu chí kiểm tra**
- Người dùng chưa đăng nhập không thể truy cập hệ thống.
- Người dùng không thể truy cập chức năng vượt quyền.
- Token GitHub/Jira được lưu an toàn.
- Mật khẩu được mã hóa trong cơ sở dữ liệu.

**Mức độ ưu tiên** High

---

# NFR-02: Performance

**Mã yêu cầu:** NFR-02

**Tên yêu cầu:** Performance

**Nhóm:** Performance

**Mô tả**

Hệ thống phải phản hồi nhanh khi người dùng thực hiện các chức năng:
- Đăng nhập.
- Xem Requirement.
- Xem Task.
- Cập nhật trạng thái Task.
- Xem Dashboard.
- Đồng bộ GitHub.
- Đồng bộ Jira.
Hệ thống phải hỗ trợ nhiều người dùng truy cập đồng thời mà vẫn đảm bảo hiệu suất.
**Tiêu chí kiểm tra**
- Thời gian đăng nhập ≤ 2 giây.
- Thời gian mở danh sách Requirement ≤ 3 giây.
- Thời gian mở danh sách Task ≤ 3 giây.
- Dashboard hiển thị ≤ 5 giây.
- Hệ thống hỗ trợ tối thiểu 100 người dùng đồng thời.

**Mức độ ưu tiên** High

---

# NFR-03: Reliability

**Mã yêu cầu:** NFR-03

**Tên yêu cầu:** Reliability

**Nhóm:** Reliability

**Mô tả**
Hệ thống phải hoạt động ổn định trong thời gian dài.
Khi xảy ra lỗi mạng hoặc GitHub/Jira không phản hồi, hệ thống không được làm mất dữ liệu đã lưu.
Các thao tác:
- Tạo Requirement
- Tạo Task
- Cập nhật Task
- Phân công Task
phải đảm bảo lưu thành công hoặc hoàn toàn không lưu (không lưu dở).

**Tiêu chí kiểm tra**

- Không mất dữ liệu khi hệ thống gặp lỗi.
- Sau khi khởi động lại vẫn còn dữ liệu.
- Tỷ lệ hoạt động đạt tối thiểu 99%.

**Mức độ ưu tiên** High

---

# NFR-04: Usability

**Mã yêu cầu:** NFR-04

**Tên yêu cầu:** Usability

**Nhóm:** Usability

**Mô tả**
Giao diện phải đơn giản, trực quan và dễ sử dụng đối với:
- Admin
- Lecturer
- Team Leader
- Team Member
Các chức năng chính phải dễ tìm kiếm.
Các nút chức năng phải được đặt tên rõ ràng.
Thông báo thành công hoặc thất bại phải dễ hiểu.

**Tiêu chí kiểm tra**
- Người dùng mới có thể sử dụng các chức năng cơ bản sau khoảng 10-15 phút làm quen.
- Không quá 3 thao tác để truy cập chức năng chính.

**Mức độ ưu tiên** Medium

---

# NFR-05: Maintainability

**Mã yêu cầu:** NFR-05

**Tên yêu cầu:** Maintainability

**Nhóm:** Maintainability

**Mô tả**
Mã nguồn phải được tổ chức theo mô hình rõ ràng (ví dụ MVC hoặc Layered Architecture).
Các chức năng phải được tách thành các module:
- Authentication
- User Management
- Requirement Management
- Task Management
- GitHub Integration
- Jira Integration
- Reporting
Việc cập nhật hoặc bổ sung chức năng mới không được ảnh hưởng đến các module khác.

**Tiêu chí kiểm tra**

- Có thể thêm module mới mà không sửa nhiều module hiện có.
- Code dễ đọc, dễ bảo trì.

**Mức độ ưu tiên** Medium

---

# NFR-06: Compatibility

**Mã yêu cầu:** NFR-06

**Tên yêu cầu:** Compatibility

**Nhóm:** Compatibility

**Mô tả**

Hệ thống phải hoạt động trên:
- Google Chrome
- Microsoft Edge
- Mozilla Firefox
Hệ thống phải tương thích với:
- GitHub REST API
- Jira REST API
API phải hỗ trợ HTTPS.

**Tiêu chí kiểm tra**

- Chạy ổn định trên các trình duyệt phổ biến.
- Kết nối GitHub thành công.
- Kết nối Jira thành công.

**Mức độ ưu tiên** High

---

# NFR-07: Error Handling

**Mã yêu cầu:** NFR-07

**Tên yêu cầu:** Error Handling

**Nhóm:** Error Handling

**Mô tả**

Khi xảy ra lỗi:
- Sai tài khoản
- Sai mật khẩu
- API GitHub lỗi
- API Jira lỗi
- Token hết hạn
- Mất kết nối Internet
- Thiếu dữ liệu nhập
hệ thống phải hiển thị thông báo cụ thể.
Không hiển thị lỗi kỹ thuật (Stack Trace) cho người dùng.
Các lỗi phải được ghi vào Log để quản trị viên kiểm tra.

**Tiêu chí kiểm tra**

- Hiển thị thông báo rõ ràng.
- Không làm treo hệ thống.
- Có log ghi nhận lỗi.

**Mức độ ưu tiên** High

---

# NFR-08: API Rate Limit

**Mã yêu cầu:** NFR-08

**Tên yêu cầu:** API Rate Limit

**Nhóm:** API Rate Limit

**Mô tả**

Do hệ thống tích hợp GitHub REST API và Jira REST API nên phải kiểm soát số lượng Request gửi tới API.
Hệ thống phải:

- Theo dõi số lượng Request.
- Hạn chế gọi API liên tục.
- Cache dữ liệu phù hợp.
- Retry khi API tạm thời lỗi.
- Thông báo cho người dùng nếu vượt Rate Limit.

**Tiêu chí kiểm tra**

- Không vượt giới hạn Request của GitHub.
- Không vượt giới hạn Request của Jira.
- Khi vượt giới hạn phải thông báo rõ nguyên nhân.

**Mức độ ưu tiên** Medium

---

# Tổng kết
| Mã | Nhóm | Ưu tiên |
|-----|-----------------|---------|
| NFR-01 | Security | High |
| NFR-02 | Performance | High |
| NFR-03 | Reliability | High |
| NFR-04 | Usability | Medium |
| NFR-05 | Maintainability | Medium |
| NFR-06 | Compatibility | High |
| NFR-07 | Error Handling | High |
| NFR-08 | API Rate Limit | Medium |
