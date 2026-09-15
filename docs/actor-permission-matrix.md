# Ma trận phân quyền và kết quả rà soát RBAC (CNPM-109)

Tài liệu này là nguồn chuẩn cho quyền truy cập của bốn vai trò. Quyền được kiểm tra ở backend theo vai trò, project, nhóm và Task; việc ẩn nút trên frontend chỉ hỗ trợ giao diện và không phải lớp bảo mật.

## Nguyên tắc phạm vi

- **Admin:** quản trị tài khoản, nhóm, phân công giảng viên và cấu hình tích hợp; được xem báo cáo toàn hệ thống nhưng không sửa Requirement hoặc Task.
- **Lecturer:** chỉ đọc Requirement, Task, báo cáo và thống kê GitHub của các nhóm được phân công.
- **Team Leader:** chỉ quản lý Requirement, Task, thành viên và thao tác đồng bộ của project thuộc nhóm mình làm leader.
- **Team Member:** chỉ xem danh sách Task trong project mình tham gia, xem/cập nhật Task được giao, xem hoạt động GitHub và báo cáo đóng góp của chính mình.

## Actor – Permission Matrix

| Chức năng | Admin | Lecturer | Team Leader | Team Member |
| --- | --- | --- | --- | --- |
| Quản trị tài khoản, nhóm và phân công giảng viên | Toàn hệ thống | Không | Không | Không |
| Cấu hình Jira/GitHub | Sửa và kiểm tra | Không | Chỉ xem cấu hình project mình | Không |
| Xem Requirement | Không | Nhóm được phân công | Project của nhóm mình | Không |
| Tạo, sửa, xóa Requirement | Không | Không | Project của nhóm mình | Không |
| Xem danh sách Task | Không | Nhóm được phân công | Project của nhóm mình | Project đang tham gia |
| Xem chi tiết Task | Không | Task thuộc nhóm được phân công | Task thuộc project mình | Chỉ Task được giao |
| Tạo, sửa, xóa, phân công Task | Không | Không | Project của nhóm mình | Không |
| Cập nhật trạng thái Task | Không | Không | Project của nhóm mình | Chỉ Task được giao |
| Xem báo cáo tiến độ | Toàn hệ thống | Nhóm được phân công | Project của nhóm mình | Chỉ dữ liệu cá nhân trong project |
| Xem danh sách commit/PR toàn repository | Toàn hệ thống | Nhóm được phân công | Project của nhóm mình | Không |
| Xem hoạt động GitHub | Toàn hệ thống | Nhóm được phân công | Project của nhóm mình | Chỉ hoạt động cá nhân/Task được giao |
| Chạy đồng bộ GitHub/Jira | Theo API quản trị | Không | Các luồng được giao cho project mình | Không |

## Điểm kiểm soát backend

- Requirement dùng `canViewRequirements(projectId)` và `canManageRequirements(projectId)` tại cả controller và service.
- Task dùng `canViewTasks(projectId)`, `canViewTask(projectId, taskId)`, `canManageTasks(projectId)` và `canUpdateTask(projectId, taskId)`; thành viên phải là assignee mới xem chi tiết hoặc cập nhật trạng thái.
- Báo cáo dùng `canViewReports(projectId)`; bộ lọc `memberId` của Team Member bị cố định về chính tài khoản đang đăng nhập.
- Hoạt động GitHub theo project tự ép `actorUserId` về Team Member hiện tại; truy cập activity theo Task dùng `canViewTask`.
- Danh sách commit, Pull Request và check-run cấp repository chỉ dành cho Admin, Lecturer được phân công hoặc Team Leader của project.
- Cấu hình Jira/GitHub chỉ Admin được thay đổi; Team Leader chỉ xem cấu hình của project mình.

## Lỗi phát hiện và trạng thái xử lý

| Mã | Vấn đề | Mức độ | Trạng thái |
| --- | --- | --- | --- |
| RBAC-01 | Controller Requirement chỉ kiểm tra role nên request sang project khác vẫn đi vào service | Cao | Đã sửa: kiểm tra trực tiếp phạm vi project tại controller |
| RBAC-02 | Controller Task chỉ kiểm tra role, chưa kiểm tra project và assignee ngay tại biên API | Cao | Đã sửa: áp dụng kiểm tra project/task/assignee tại controller |
| RBAC-03 | API commit và Pull Request cấp repository cho Team Member xem dữ liệu toàn nhóm | Cao | Đã sửa: Team Member chỉ còn API activity cá nhân hoặc Task được giao |
| RBAC-04 | API check-run cấp repository dùng quyền xem Task nên Team Member đọc được dữ liệu toàn project | Trung bình | Đã sửa: giới hạn Admin, Lecturer được phân công và Team Leader của project |
| RBAC-05 | Danh sách thành viên project chỉ kiểm tra role Team Leader | Trung bình | Đã sửa: kiểm tra Team Leader đúng project tại controller và service |
| RBAC-06 | Báo cáo tiến độ thiếu kiểm tra cùng chính sách với báo cáo tổng hợp | Cao | Đã sửa: cả hai endpoint dùng `canViewReports` |

## Bằng chứng kiểm thử

`ReportRbacIntegrationTest` xác minh các trường hợp: Admin chỉ quản trị và xem báo cáo; Lecturer chỉ đọc project được phân công; Team Leader chỉ quản lý project của nhóm mình; Team Member chỉ xem Task được giao và báo cáo cá nhân; truy cập chéo project, thành viên hoặc dữ liệu GitHub cấp repository trả về `403`; request chưa đăng nhập trả về `401`.
