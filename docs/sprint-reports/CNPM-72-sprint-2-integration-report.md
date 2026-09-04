# CNPM-72 - Báo cáo tích hợp backend và frontend Sprint 2

## 1. Mục tiêu

Xác nhận lát cắt Sprint 2 hoạt động xuyên suốt trên cùng một nhánh: đăng nhập, quản lý Requirement, tạo và giao Task, Member xem công việc được giao và Leader/Lecturer xem trước SRS.

Mốc nền dùng để tích hợp: `origin/main` tại commit `5721b9e` ngày 23/08/2026.

## 2. Phần đã tích hợp

- Ghép giao diện Task từ CNPM-65 vào Dashboard hiện tại, giữ nguyên phần Requirement của CNPM-63/64.
- Nối nút **Tạo Requirement** và **Sửa** với `RequirementForm`; không còn thông báo giả “sẽ tích hợp”.
- Đồng nhất base URL Requirement và Task về biến `REACT_APP_API_URL`.
- Bổ sung `id` và `projectId` vào kết quả đăng nhập để frontend tự nhận đúng user/project context.
- Bổ sung API `GET /api/v1/projects/{projectId}/members` để Leader chọn Member ACTIVE khi giao Task.
- Bổ sung màn hình **SRS Preview**, lấy trực tiếp dữ liệu Requirement và cho phép in/lưu PDF từ trình duyệt.
- Bổ sung migration V4 với group, project, Lecturer, Leader, Member, Sprint, Feature và Task mẫu hợp lệ.
- Bổ sung automated test cho project context và toàn bộ luồng demo backend.

## 3. Đối chiếu Acceptance Criteria

| Tiêu chí | Bằng chứng | Kết quả |
|---|---|---|
| Leader đăng nhập | Login API trả JWT, `id`, `role`, `projectId` | Đạt |
| Leader tạo Requirement | Dashboard mở `RequirementForm`, gọi POST Requirement | Đạt |
| Leader tạo Task và giao Member | Task form tải Member ACTIVE từ API và gửi `assigneeUserId` | Đạt |
| Leader xem Task list | Dashboard mở Task list từ API theo project | Đạt |
| Member đăng nhập và xem Task được giao | Backend bắt buộc filter theo current user; integration test kiểm tra Task vừa giao | Đạt |
| Leader mở SRS Preview | Tab SRS Preview tổng hợp tối đa 100 Requirement và kiểm tra quyền | Đạt |
| Backend build/test thành công | Maven test toàn dự án, không có failure/error/skipped | Đạt |
| Frontend test/build thành công | React test và production build thành công | Đạt |
| Migration chạy trên database mới | Flyway chạy tuần tự V1 → V4 trên H2 MySQL mode | Đạt |
| Không chứa secret | Chỉ có placeholder `.env.example`; không commit `.env` hoặc token | Đạt |
| Không có lỗi console nghiêm trọng | Frontend tests không còn `console.error`; production build thành công | Đạt |

## 4. Luồng demo đề xuất

### Chuẩn bị

1. Tạo `.env` cục bộ từ `.env.example` và thay toàn bộ placeholder bằng giá trị local. Không commit file này.
2. Chạy MySQL bằng `docker compose --env-file .env up -d mysql`.
3. Chạy backend bằng `mvnw.cmd spring-boot:run`.
4. Trong `frontend`, giữ `REACT_APP_API_URL=http://localhost:8080/api/v1`, sau đó chạy `npm start`.

Các tài khoản demo do V3 tạo gồm `leader.test`, `member.test`, `lecturer.test` và `admin.test`. Mật khẩu local-only được ghi trong migration V3; không dùng các tài khoản này ngoài môi trường demo.

### Kịch bản

1. Đăng nhập bằng `leader.test`.
2. Ở tab **Yêu cầu dự án**, chọn **Tạo Requirement**, nhập Title, Actor, Priority, Description và Main Flow rồi lưu.
3. Chuyển sang **Công việc được giao**, chọn **Tạo Task mới**, chọn `member.test` trong danh sách người thực hiện rồi lưu.
4. Kiểm tra Task vừa tạo xuất hiện trong danh sách của Leader.
5. Đăng xuất, đăng nhập bằng `member.test`, mở **Công việc được giao** và xác nhận Task vừa giao xuất hiện.
6. Đăng xuất, đăng nhập lại bằng `leader.test`, mở **Xem trước SRS** và xác nhận Requirement vừa tạo được tổng hợp.

## 5. Kiểm thử tự động

- `Sprint2DemoFlowIntegrationTest`: đăng nhập hai vai trò, kiểm tra project context, danh sách Member, tạo Requirement, tạo/giao Task, danh sách Task của Leader/Member và dữ liệu nguồn cho SRS.
- `ProjectRepositoryTests`: Leader, Lecturer và Member nhận cùng project; Admin không nhận student project.
- Backend: toàn bộ **111/111 test đạt**, không có failure, error hoặc skipped.
- Frontend: toàn bộ **57/57 test đạt** trên 6 test suite; production build thành công.
- Phạm vi frontend được kiểm tra gồm Requirement form integration, Task list/create/detail/status, member selector, SRS Preview và Dashboard navigation.

## 6. Checklist lỗi/giới hạn còn lại

- CNPM-66 mới tồn tại dưới dạng UI mock trên nhánh riêng; chưa ghép bộ lọc Task vào API thật.
- CNPM-70 có collection Postman trên nhánh riêng nhưng chưa merge và chưa có run result tin cậy.
- SRS Preview hiện là bản xem trực tiếp/in từ trình duyệt; lưu version SRS phía server là phạm vi sau.
- Tab Tiến độ nhóm và Hoạt động GitHub vẫn là placeholder.
- Chưa đồng bộ Jira thật; đây là mục tiêu chính của Sprint 3.

## 7. Kết luận

CNPM-72 đủ điều kiện đưa vào review sau khi PR có CI/test xanh. Chỉ chuyển `Done` sau khi PR được reviewer chấp thuận và merge vào `main`.
