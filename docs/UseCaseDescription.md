# Use Case ID: UC-01
# Use Case Name: Đăng nhập

# Primary Actor
    + Admin
    + Lecturer
    + Team Leader
    + Team Member

# Description
    Hệ thống cho phép người dùng đăng nhập để truy cập các chức năng tương ứng với quyền hạn của mình.

# Preconditions
    + Người dùng đã có tài khoản.
    + Tài khoản đang ở trạng thái hoạt động.

# Postconditions
    + Người dùng đăng nhập thành công.
    + Hệ thống chuyển hướng đến trang chủ.

# Main Flow
    1. Người dùng truy cập trang đăng nhập.
    2. Hệ thống hiển thị biểu mẫu đăng nhập.
    3. Người dùng nhập tên đăng nhập và mật khẩu.
    4. Người dùng nhấn nút Đăng nhập.
    5. Hệ thống xác thực thông tin.
    6. Hệ thống chuyển người dùng đến giao diện phù hợp với vai trò.

# Alternative Flow
    3A. Người dùng quên mật khẩu.
    4A. Người dùng chọn hiển thị mật khẩu.

# Exception Flow
    5E. Tên đăng nhập hoặc mật khẩu không chính xác.
    6E. Tài khoản bị khóa.


# Use Case ID: UC-02
# Use Case Name: Tạo Task

# Primary Actor
    + Team Leader

# Description
    Team Leader tạo nhiệm vụ mới cho dự án.

# Preconditions
    + Người dùng đã đăng nhập.
    + Requirement đã được tạo.

# Postconditions
    + Task được tạo thành công.

# Main Flow
    1. Team Leader truy cập chức năng quản lý Task.
    2. Hệ thống hiển thị danh sách Task.
    3. Team Leader chọn tạo Task mới.
    4. Người dùng nhập thông tin Task.
    5. Hệ thống kiểm tra dữ liệu.
    6. Hệ thống lưu Task.

# Alternative Flow
    4A. Người dùng lưu bản nháp.

# Exception Flow
    4E. Thiếu thông tin bắt buộc.
    5E. Dữ liệu không hợp lệ.


# Use Case ID: UC-03
# Use Case Name: Cấu hình Jira

# Primary Actor
    + Admin

# Description
    Admin cấu hình kết nối giữa hệ thống và Jira.

# Preconditions
    + Admin đã đăng nhập.
    + Có thông tin kết nối Jira.

# Postconditions
    + Cấu hình Jira được lưu thành công.

# Main Flow
    1. Admin truy cập chức năng Cấu hình Jira.
    2. Hệ thống hiển thị biểu mẫu cấu hình.
    3. Admin nhập thông tin kết nối.
    4. Hệ thống kiểm tra kết nối.
    5. Hệ thống lưu cấu hình.

# Alternative Flow
    3A. Admin cập nhật thông tin cấu hình hiện có.

# Exception Flow
    4E. Không thể kết nối đến Jira.
    5E. Thông tin cấu hình không hợp lệ.


# Use Case ID: UC-04
# Use Case Name: Cấu hình GitHub

# Primary Actor
    + Admin

# Description
    Admin cấu hình kết nối giữa hệ thống và GitHub.

# Preconditions
    + Admin đã đăng nhập.
    + Có thông tin kết nối GitHub.

# Postconditions
    + Cấu hình GitHub được lưu thành công.

# Main Flow
    1. Admin truy cập chức năng Cấu hình GitHub.
    2. Hệ thống hiển thị biểu mẫu cấu hình.
    3. Admin nhập thông tin kết nối.
    4. Hệ thống kiểm tra kết nối.
    5. Hệ thống lưu cấu hình.

# Alternative Flow
    3A. Admin cập nhật thông tin cấu hình.

# Exception Flow
    4E. Không thể kết nối đến GitHub.
    5E. Thông tin cấu hình không hợp lệ.


# Use Case ID: UC-05
# Use Case Name: Xem thống kê commit GitHub

# Primary Actor
    + Admin
    + Lecturer
    + Team Leader
    + Team Member

# Description
    Người dùng xem thống kê commit từ GitHub.

# Preconditions
    + Dữ liệu commit đã được cập nhật.

# Postconditions
    + Thống kê commit được hiển thị.

# Main Flow
    1. Người dùng truy cập chức năng thống kê commit.
    2. Hệ thống lấy dữ liệu commit.
    3. Hệ thống hiển thị thống kê commit.
    4. Người dùng xem kết quả.

# Alternative Flow
    2A. Người dùng lọc dữ liệu theo thời gian.

# Exception Flow
    2E. Không có dữ liệu commit.


# Use Case ID: UC-06
# Use Case Name: Cập nhật trạng thái Task

# Primary Actor
    + Team Leader
    + Team Member

# Description
    Người dùng cập nhật trạng thái của Task.

# Preconditions
    + Người dùng đã đăng nhập.
    + Task đã được tạo và phân công.

# Postconditions
    + Trạng thái Task được cập nhật thành công.

# Main Flow
    1. Người dùng mở danh sách Task.
    2. Người dùng chọn Task cần cập nhật.
    3. Người dùng chọn trạng thái mới.
    4. Hệ thống lưu thay đổi.
    5. Hệ thống cập nhật trạng thái Task.

# Alternative Flow
    3A. Người dùng hủy thao tác cập nhật.

# Exception Flow
    2E. Không tìm thấy Task.
    4E. Không thể cập nhật trạng thái Task.


# Use Case ID: UC-07
# Use Case Name: Xem báo cáo tiến độ

# Primary Actor
    + Admin
    + Lecturer
    + Team Leader

# Description
    Người dùng xem báo cáo tiến độ của dự án.

# Preconditions
    + Dữ liệu Task và Commit đã được cập nhật.

# Postconditions
    + Báo cáo tiến độ được hiển thị.

# Main Flow
    1. Người dùng truy cập chức năng báo cáo.
    2. Hệ thống hiển thị danh sách dự án.
    3. Người dùng lựa chọn dự án.
    4. Hệ thống hiển thị báo cáo tiến độ.
    5. Người dùng xem kết quả.

# Alternative Flow
    3A. Người dùng lọc dữ liệu theo thời gian.

# Exception Flow
    4E. Không có dữ liệu báo cáo.
