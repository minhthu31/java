# Actor Permission Matrix
# Actor
- Admin
- Lecturer
- Team Leader
- Team Member


# Phân tích các ACTOR

- Admin
    - Nhiệm vụ
        + Quản lý tài khoản người dùng.
        + Quản lý danh sách giảng viên.
        + Quản lý nhóm sinh viên.
        + Phân công giảng viên cho từng nhóm.
        + Thiết lập và quản lý kết nối Jira.
        + Thiết lập và quản lý kết nối GitHub.
        + Quản lý phân quyền của các actor.
        + Theo dõi báo cáo tiến độ của tất cả các nhóm.
    - Quyền hạn
        + Toàn quyền quản trị hệ thống.
        + Có thể xem, thêm, sửa, xóa dữ liệu quản trị.
        + Có quyền truy cập mọi thông tin của hệ thống.
- Lecturer
    + Nhiệm vụ
        + Quản lý sinh viên trong các nhóm
        + Xem danh sách Requirement của nhóm
        + Xem danh sách Task của nhóm
        + Theo dõi tiến độ thực hiện dự án
        + Xem báo cáo bà thống kê tiến bộ dự án qua commit GitHub
    + Quyền hạn
        + Chỉ được xem dữ liệu của các nhóm phụ trách.
        + Không được tạo hoặc chỉnh sửa cấu hình hệ thống.
        + Không được phân công công việc cho thành viên quản trị người dùng.
- Team leader
    + Nhiệm vụ
        + Quản lý Requirements của nhóm.
        + Tạo, chỉnh sửa và xóa Task.
        + Phân công Task cho từng thành viên.
        + Theo dõi tiến độ thực hiện Task.
        + Cập nhật trạng thái công việc khi cần.
        + Theo dõi báo cáo tiến độ của nhóm.
    + Quyền hạn
        + Chỉ được quản lý các nhóm được phân công
        + Chỉ được thao tác trên dữ liệu trên dữ liệu của nhóm 
        + Được phép quản lý Requirement, Task của nhóm
- Team Member
    + Nhiệm vụ
        + Xem danh sách Task được giao.
        + Thực hiện công việc theo Task.
        + Cập nhật trạng thái Task.
        + Commit mã nguồn lên GitHub.
        + Theo dõi tiến độ công việc cá nhân.
    + Quyền hạn
        + Chỉ được thao tác trên các Task được phân công.
        + Không được tạo hoặc phân công Task.
        + Không được quản lý Requirements.
        + Không được thay đổi cấu hình hệ thống hoặc phân quyền người dùng.


# Actor – Permission Matrix
| Chức năng                     | Admin | Lecturer | Team Leader | Team Member |
| ----------------------------- | ----- | -------- | ----------- | ----------- |
| Đăng nhập hệ thống            | Y     | Y        | Y           | Y           |
| Quản lý tài khoản             | Y     | N        | N           | N           |
| Quản lý giảng viên            | Y     | N        | N           | N           |
| Quản lý nhóm sinh viên        | Y     | N        | N           | N           |
| Gán giảng viên cho nhóm       | Y     | N        | N           | N           |
| Cấu hình Jira                 | Y     | N        | N           | N           |
| Cấu hình GitHub               | Y     | N        | N           | N           |
| Xem Requirement               | N     | Y        | Y           | N           |
| Quản lý Requirement           | N     | N        | Y           | N           |
| Xem Task                      | N     | Y        | Y           | Y           |
| Tạo Task                      | N     | N        | Y           | N           |
| Sửa Task                      | N     | N        | Y           | N           |
| Xóa Task                      | N     | N        | Y           | N           |
| Phân công Task                | N     | N        | Y           | N           |
| Cập nhật trạng thái Task      | N     | N        | Y           | Y           |
| Theo dõi tiến độ dự án        | Y     | Y        | Y           | Y           |
| Xem báo cáo tiến độ           | Y     | Y        | Y           | N           |
| Xem thống kê commit GitHub    | Y     | Y        | Y           | Y           |
| Đồng bộ Jira                  | N     | N        | Y           | N           |
| Đồng bộ GitHub                | N     | N        | Y           | N           |
| Commit source code lên GitHub | N     | N        | Y           | Y           |



# Chức năng của mỗi Actor
1. Admin
    + Đăng nhập.
    + Quản lý tài khoản giảng viên.
    + Quản lý nhóm sinh viên.
    + Gán giảng viên cho nhóm.
    + Cấu hình tích hợp GitHub.
    + Cấu hình tích hợp Jira.
    + Xem báo cáo tổng hợp.
    + Xem thống kê commit GitHub.
2. Lecturer
    + Đăng nhập.
    + Xem nhóm được phân công.
    + Xem Requirement.
    + Xem Task.
    + Theo dõi tiến độ dự án
    + Xem báo cáo dự án.
    + Xem thống kê commit GitHub.
3. Team leader
    + Đăng nhập.
    + Quản lý Requirement (thêm, sửa, xóa).
    + Tạo Task.
    + Sửa Task.
    + Xóa Task.
    + Phân công Task.
    + Cập nhật trạng thái Task.
    + Theo dõi tiến độ nhóm.

4. Team Member
    + Đăng nhập.
    + Xem Task được giao.
    + Cập nhật trạng thái Task.
    + Commit source code.
    + Xem thống kê commit cá nhân.