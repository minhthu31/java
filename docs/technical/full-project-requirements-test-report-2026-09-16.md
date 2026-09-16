# Báo cáo kiểm thử toàn bộ project theo đề bài

Ngày kiểm thử: 2026-09-16
Nhánh: `feature/CNPM-115-118-final-verification`
Commit: `d31d1f6`

## 1. Phạm vi và phương pháp

- Chạy toàn bộ backend test bằng Maven trên database H2 mới tạo bởi Flyway.
- Chạy toàn bộ frontend test bằng Jest/React Testing Library.
- Build frontend ở chế độ production.
- Chạy backend đã đóng gói với MySQL 8.4 thật và kiểm tra Flyway migration.
- Smoke test API với bốn tài khoản `ADMIN`, `LECTURER`, `TEAM_LEADER`, `TEAM_MEMBER`.
- Rà soát code và API hiện có so với từng yêu cầu trong đề bài.
- Không gọi Jira/GitHub bên ngoài vì database demo chưa có cấu hình hoặc token thật.

## 2. Kết quả tự động

| Hạng mục | Kết quả |
|---|---:|
| Backend | 421/421 test đạt |
| Frontend | 113/113 test đạt |
| Frontend production build | Thành công |
| MySQL/Flyway | 18 migration hợp lệ, schema ở version 15 |
| Smoke test API/RBAC trên MySQL | 24/24 trường hợp đạt |
| Tổng kiểm tra tự động và smoke test | 558/558 đạt |

Frontend test không còn cảnh báo React `act(...)` ở luồng lọc báo cáo đóng góp.

## 3. Ma trận yêu cầu

| Yêu cầu | Trạng thái | Bằng chứng và nhận xét |
|---|---|---|
| Đăng nhập và phân quyền Admin/Lecturer/Team Leader/Team Member | **Đạt** | Bốn tài khoản đăng nhập được; JWT, RBAC và project scope đạt qua backend test và 17/17 smoke test MySQL. |
| Tạo tài liệu SRS có hệ thống từ yêu cầu Jira | **Đạt** | Có Requirement đầy đủ actor, flow, priority, Jira key; backend tạo snapshot SRS có phiên bản, nội dung JSON và checksum SHA-256; đã tạo/đọc lại `v1` trên MySQL thật. |
| Tổng hợp phân công và thực hiện công việc thành viên | **Đạt** | Task liên kết project, sprint, requirement, feature, assignee; có trạng thái, deadline, lọc và báo cáo theo thành viên. |
| Đánh giá năng suất từ commit GitHub | **Đạt** | Báo cáo tổng hợp commit, PR và Task liên kết theo thành viên, sprint, khoảng thời gian; có chống đếm trùng. |
| Đánh giá chất lượng commit | **Đạt** | Có API điểm chất lượng theo thành viên dựa trên commit bị revert và GitHub check run; công thức, mức xếp hạng và phạm vi dữ liệu theo vai trò đã được chốt trong code. |
| Admin quản lý nhóm sinh viên | **Đạt** | Có API và giao diện tạo/cập nhật nhóm, xem thành viên, thêm hoặc loại thành viên; đã smoke test trên MySQL thật. |
| Admin quản lý giảng viên và phân công giảng viên vào nhóm | **Đạt** | Có API/giao diện tạo giảng viên, phân công và hủy phân công; thao tác phân công có tính idempotent. |
| Admin cấu hình Jira và GitHub | **Đạt** | Có API lưu, đọc, kiểm tra kết nối; token được mã hóa và chỉ Admin được sửa cấu hình. |
| Lecturer quản lý sinh viên trong nhóm được phân công | **Đạt** | Lecturer có API/giao diện xem, tạo, gán và loại sinh viên; service kiểm tra quan hệ `group_lecturers`, truy cập ngoài phạm vi bị chặn. |
| Lecturer xem Requirement, Task, tiến độ và thống kê GitHub | **Đạt** | API Requirement, Task và Report trả 200 cho Lecturer đúng nhóm; sai phạm vi bị chặn trong test RBAC. |
| Team Leader quản lý Requirement/Task và giao việc | **Đạt** | Có CRUD, chuyển trạng thái, assignee validation, idempotency và project scope. |
| Team Leader theo dõi tiến độ và đóng góp nhóm | **Đạt** | Có progress/summary report, tiến độ Sprint, Task quá hạn, commit và PR theo thành viên. |
| Team Member xem/cập nhật Task được giao | **Đạt** | Member xem Task theo phạm vi, cập nhật trạng thái Task được giao; truy cập Requirement quản trị bị chặn 403. |
| Team Member xem commit và thống kê cá nhân | **Đạt** | Backend tự giới hạn member report về chính user hiện tại; có liên kết external GitHub account. |
| Jira Cloud REST API: project, issue, backlog, sprint | **Đạt một phần** | Client, DTO, pagination, retry, snapshot, sync log và test đều có. Chưa kiểm chứng end-to-end với Jira Cloud thật vì cấu hình demo chưa được thiết lập. |
| GitHub REST API: repository, commit, PR, user | **Đạt một phần** | Dùng `api.github.com`, có pagination, commit, PR, PR commits, check runs và account link. Chưa kiểm chứng end-to-end với GitHub thật vì cấu hình demo `NOT_CONFIGURED`. |
| Phân loại Task mới/feature/auto-test/auto-log | **Đạt** | Backend tự phân loại theo nội dung và liên kết Requirement/Feature; hỗ trợ `NEW_FEATURE`, `FEATURE_RELATED`, `AUTO_TEST`, `AUTO_LOG`, `OTHER`. |
| Hệ thống tự thực thi auto-test | **Đạt theo kiến trúc CI** | GitHub Actions/Check Runs là bộ thực thi test; backend đồng bộ kết quả, nhận FAILURE/CANCELLED và tự tạo Task khắc phục phân loại `AUTO_TEST`. Không chạy lệnh tùy ý trực tiếp trên server ứng dụng. |
| Hệ thống auto-log Task | **Đạt** | Có `activity_logs`, `sync_logs`, correlation ID và luồng idempotent tự tạo Task từ check run lỗi, kèm commit và URL truy vết. |
| Phân loại Task trên Jira | **Đạt một phần** | Classification được lưu ở Task local và Task có thể đồng bộ Jira; chưa thấy mapping classification thành Jira issue type/label riêng được kiểm chứng end-to-end. |

## 4. Kết quả hoàn thiện bảy khoảng trống

1. Đã có màn hình và API Admin quản lý nhóm.
2. Đã có quản lý giảng viên, phân công giảng viên và Lecturer quản lý sinh viên đúng phạm vi.
3. Đã có SRS versioning phía backend, snapshot nội dung và checksum.
4. Luồng cấu hình/test connection Jira và GitHub đã sẵn sàng; kiểm thử với dịch vụ thật cần người vận hành nhập URL/token hợp lệ của dự án.
5. Auto-test dùng GitHub CI/Check Runs; check thất bại tự sinh Task khắc phục và chống tạo trùng.
6. Đã chốt và triển khai điểm chất lượng commit cùng mức xếp hạng.
7. Đã dọn cảnh báo bất đồng bộ trong frontend test.

## 5. Kết luận

Các phần có thể hoàn thiện độc lập trong mã nguồn và database đã được triển khai, build và kiểm thử thành công. Điểm duy nhất phụ thuộc môi trường bên ngoài là xác nhận end-to-end với Jira Cloud và repository GitHub thật; việc này không thể chứng nhận khi chưa có URL, email/token Jira và repository/token GitHub của dự án.
