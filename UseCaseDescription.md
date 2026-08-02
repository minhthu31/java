# Use Case ID: UC-01
# Tên Use Case: Đăng nhập

# Tác nhân chính
    + Admin
    + Lecturer
    + Team Leader
    + Team Member
# Mô tả
    Hệ thống cho phép người dùng đăng nhập để truy cập các chức năng tương ứng với quyền hạn của mình.
# Tiền điều kiện
    + Người dùng đã có tài khoản.
    + Tài khoản đang ở trạng thái hoạt động.
# Hậu điều kiện
    + Người dùng đăng nhập thành công.
    + Hệ thống chuyển hướng đến trang chủ.
# Luồng sự kiện chính
    1 Người dùng truy cập trang đăng nhập.
    2 Hệ thống hiển thị biểu mẫu đăng nhập.
    3 Người dùng nhập tên đăng nhập và mật khẩu.
    4 Người dùng nhấn nút Đăng nhập.
    5 Hệ thống xác thực thông tin.
    6 Hệ thống chuyển người dùng đến giao diện phù hợp với vai trò của họ.
# Luồng sự kiện phụ
    3A. Người dùng quên mật khẩu.
    4A. Người dùng chọn hiển thị mật khẩu.

# Luồng ngoại lệk
    5E. Tên đăng nhập hoặc mật khẩu không chính xác.
    6E. Tài khoản bị khóa.


# Use Case ID: UC-02
# Tên Use Case: Tạo và giao task

# Tác nhân chính
    + Team Leader

# Mô tả
Team Leader tạo nhiệm vụ mới và phân công cho các thành viên trong nhóm.

# Tiền điều kiện
    + Người dùng đã đăng nhập.
    + Requirement đã được tạo.
    + Hậu điều kiện
    + Task được tạo thành công.
    + Thành viên được nhận thông báo.
# Luồng sự kiện chính
    1. Team Leader truy cập chức năng quản lý task.
    2. Hệ thống hiển thị danh sách task.
    3. Team Leader chọn tạo task mới.
    4. Người dùng nhập thông tin task.
    5. Người dùng phân công task.
    6. Hệ thống lưu dữ liệu.
    7. Hệ thống gửi thông báo.
# Luồng sự kiện phụ
4A. Người dùng lưu bản nháp.
# Luồng ngoại lệ
4E. Thiếu thông tin bắt buộc.
5E. Thành viên không tồn tại.


# Use Case ID: UC-03

#Tên Use Case: Đẩy task lên Jira

# Tác nhân chính
    + Team Leader
# Mô tả
Hệ thống gửi thông tin task từ hệ thống nội bộ lên Jira.

# Tiền điều kiện
    + Người dùng đã đăng nhập.
    + Jira đã được cấu hình.
    + Hậu điều kiện
    + Task xuất hiện trên Jira.
# Luồng sự kiện chính
    1. Người dùng chọn task.
    2. Người dùng nhấn nút đẩy lên Jira.
    3. Hệ thống kết nối với Jira.
    4. Hệ thống gửi dữ liệu.
    5. Hệ thống hiển thị thông báo thành công.
# Luồng sự kiện phụ
2A. Người dùng đẩy nhiều task cùng lúc.
# Luồng ngoại lệ
3E. Không thể kết nối đến Jira.
4E. Dữ liệu không hợp lệ.


# Use Case ID: UC-04
# Tên Use Case: Đồng bộ Jira

# Tác nhân chính
    + Admin
    + Team Leader
# Mô tả
Hệ thống đồng bộ trạng thái giữa hệ thống và Jira.

# Tiền điều kiện
    + Tài khoản Jira đã được cấu hình.
    + Hậu điều kiện
    + Dữ liệu được cập nhật.
# Luồng sự kiện chính
    1. Người dùng chọn chức năng đồng bộ.
    2. Hệ thống kết nối với Jira.
    3. Hệ thống lấy dữ liệu.
    4. Hệ thống cập nhật dữ liệu.
# Luồng sự kiện phụ
3A. Chỉ đồng bộ dữ liệu mới.

# Luồng ngoại lệ
2E. Kết nối thất bại.

# Use Case ID: UC-05
# Tên Use Case: Đồng bộ GitHub

# Tác nhân chính
    + Admin
    + Team Leader
# Mô tả
Hệ thống lấy dữ liệu commit từ GitHub.

# Tiền điều kiện
    + GitHub đã được cấu hình.
    + Hậu điều kiện
    + Thông tin commit được cập nhật.
# Luồng sự kiện chính
    1. Người dùng chọn chức năng đồng bộ GitHub.
    2. Hệ thống kết nối với GitHub.
    3. Hệ thống lấy dữ liệu commit.
    4. Hệ thống lưu dữ liệu.
# Luồng sự kiện phụ
3A. Chỉ đồng bộ commit mới.

# Luồng ngoại lệ
2E. Không thể kết nối GitHub.


# Use Case ID: UC-06
# Tên Use Case: Liên kết task với commit

# Tác nhân chính
    + Team Leader
    + Team Member
# Mô tả
Hệ thống liên kết commit với task tương ứng.

# Tiền điều kiện
    + Task đã được tạo.
    + Thành viên đã thực hiện commit.
# Hậu điều kiện
Commit được liên kết với task.
# Luồng sự kiện chính
    1. Thành viên tạo commit.
    2. Hệ thống nhận mã commit.
    3. Hệ thống đối chiếu mã task.
    4. Hệ thống lưu liên kết.
# Luồng sự kiện phụ
3A. Người dùng liên kết thủ công.

# Luồng ngoại lệ
3E. Không tìm thấy task.


# Use Case ID: UC-07
# Tên Use Case: Xem báo cáo tiến độ

# Tác nhân chính
    + Admin
    + Lecturer
    + Team Leader
# Mô tả
Người dùng xem tiến độ thực hiện của dự án.

# Tiền điều kiện
    Dữ liệu task và commit đã được cập nhật.
# Hậu điều kiện
    Báo cáo được hiển thị.
# Luồng sự kiện chính
    1. Người dùng truy cập chức năng báo cáo.
    2. Hệ thống hiển thị danh sách dự án.
    3. Người dùng lựa chọn dự án.
    4. Hệ thống hiển thị biểu đồ thống kê.
    5. Người dùng xem kết quả.
# Luồng sự kiện phụ
3A. Người dùng lọc dữ liệu theo thời gian.

# Luồng ngoại lệ
4E. Không có dữ liệu.
