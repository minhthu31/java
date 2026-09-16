# CNPM-118 — Kịch bản diễn tập demo cuối kỳ và chốt Sprint 5

## 1. Mục tiêu

Tài liệu này là kịch bản duy nhất dùng trong buổi diễn tập và buổi demo chính thức. Mỗi phần
có người chịu trách nhiệm, dữ liệu cần chuẩn bị, kết quả mong đợi và phương án dự phòng.

## 2. Phân công trình bày

| Phần | Người phụ trách | Nội dung |
| --- | --- | --- |
| Mở đầu, đăng nhập theo vai trò, điều phối và kết luận | MT | Giới thiệu phạm vi, chuyển người trình bày, chốt kết quả |
| Database, migration và dữ liệu demo | N | Chứng minh schema mới migrate được, giới thiệu dữ liệu Requirement/Sprint/Task |
| Jira/GitHub API, cấu hình an toàn và kịch bản mất kết nối | NT | Giới thiệu cấu hình, che token, giải thích retry/fallback |
| Luồng giao diện | HM | Điều hướng Requirement, Task, Sprint, Integration và Reports |
| RBAC, test và báo cáo tiến độ/đóng góp | H | So sánh quyền, trình bày kết quả test và số liệu báo cáo |
| Jira/GitHub client và tài liệu tích hợp | Người phụ trách CNPM-108 | Demo sync hoặc dùng dữ liệu dự phòng khi provider lỗi |

Nếu một người vắng mặt, MT tiếp quản phần đó và dùng ảnh/Postman response đã chuẩn bị.

## 3. Chuẩn bị trước buổi demo

- Backend dùng JDK 21 và MySQL đang chạy.
- Frontend đã `npm ci`, test và build thành công.
- Tạo schema MySQL demo mới và để Flyway migrate; không dùng dữ liệu cá nhân.
- Bật demo seed theo hướng dẫn trong `docs/demo-database.md` nếu cần dữ liệu ổn định.
- Chuẩn bị bốn tài khoản: Admin, Lecturer, Team Leader và Team Member.
- Token Jira/GitHub chỉ đặt trong environment; không chiếu hoặc ghi token vào slide/log.
- Mở sẵn Jira project, GitHub repository, trang đăng nhập và Postman collection.
- Ghi lại commit SHA của release candidate; không pull/merge thêm trong lúc diễn tập.

## 4. Luồng trình bày chính

### Bước 1 — Đăng nhập và RBAC

1. Đăng nhập Team Leader, mở project và các màn hình quản lý.
2. Đăng nhập Team Member, chứng minh chỉ xem/cập nhật task được giao và chỉ xem đóng góp cá nhân.
3. Đăng nhập Lecturer, chứng minh chỉ xem nhóm được phân công.
4. Nếu thời gian cho phép, Admin trình bày cấu hình/quản trị.

Kết quả đạt: mỗi vai trò chỉ nhìn thấy chức năng và dữ liệu đúng phạm vi.

### Bước 2 — Requirement, Sprint và Task

1. Team Leader tạo hoặc mở Requirement mẫu.
2. Mở danh sách Sprint bằng `GET /api/v1/projects/{projectId}/sprints` thông qua giao diện tiến độ.
3. Tạo Task, gán Requirement, Sprint và Team Member.
4. Team Member cập nhật trạng thái Task; quay lại Team Leader để kiểm tra tiến độ.

Kết quả đạt: Sprint tải được, Task thuộc đúng project/Sprint và RBAC không bị vượt quyền.

### Bước 3 — Đồng bộ Jira

1. Cấu hình Jira bằng biến môi trường/token đã che.
2. Chạy đồng bộ project hoặc đồng bộ Task.
3. Chứng minh Task lưu đúng `jiraIssueKey` và trạng thái đồng bộ.
4. Chạy lại để chứng minh không tạo bản ghi trùng.

### Bước 4 — Đồng bộ GitHub và liên kết Task

1. Đồng bộ repository, commit và Pull Request.
2. Chọn commit/PR có Jira key hoặc Task key và mở liên kết trong hệ thống.
3. Chứng minh trạng thái Open/Closed/Merged, tác giả và thời gian remote được lưu đúng.

### Bước 5 — Báo cáo

1. Mở báo cáo tiến độ project, chọn Sprint và khoảng thời gian.
2. Đối chiếu tổng Task, Task hoàn thành, quá hạn và phần trăm tiến độ.
3. Mở báo cáo đóng góp thành viên, đối chiếu commit, PR và Task liên kết.
4. Đổi sang Team Member để chứng minh chỉ thấy đóng góp của chính mình.

## 5. Kịch bản dự phòng

### Jira không truy cập được

- Không nhập lại hoặc chiếu token.
- Hiển thị trạng thái lỗi/retry trong giao diện hoặc Postman.
- Dùng Jira snapshot và SyncLog đã lưu để tiếp tục báo cáo.
- Trình bày test `JiraSyncReliabilityContractTest` và partial-failure trong
  `JiraGitHubReportIntegrationTest` làm bằng chứng dữ liệu đã đồng bộ không bị mất.

### GitHub không truy cập được

- Dùng repository, commit, PR và task links đã đồng bộ trong database demo.
- Trình bày thông báo provider unavailable/retryable, không đổi dữ liệu đã lưu.
- Không tạo token mới trực tiếp trên màn hình chiếu.

### Máy demo hoặc frontend lỗi

- Chạy backend API bằng Postman collection trong `docs/postman`.
- Dùng production build đã tạo và ảnh response có sẵn trong `docs/integrations/assets`.
- Nếu database hỏng, chuyển sang schema demo dự phòng đã migrate trước buổi trình bày.

## 6. Done/Not Done của Sprint 5

### Done về mặt mã nguồn và kiểm thử

- [x] Đăng nhập và phân quyền theo vai trò có automated test.
- [x] Requirement và Task có API/giao diện và automated test.
- [x] Sprint có API danh sách theo project, RBAC và integration test.
- [x] Jira sync có happy path, retry, idempotency và partial-failure test.
- [x] GitHub repository/commit/PR sync và liên kết Task có test.
- [x] Báo cáo tiến độ và đóng góp thành viên có test.
- [x] Backend verify (`417/417`), frontend test (`113/113`) và production build chạy thành công trên release candidate.
- [x] Backend khởi động trên MySQL 8.4.11, health `UP`, đăng nhập và tải Sprint thành công.
- [x] Có kịch bản dự phòng khi Jira/GitHub không truy cập được.
- [x] Đã phân người trình bày từng phần.

### Cần xác nhận thủ công trong Jira trước khi đóng Sprint

- [ ] Cả nhóm chạy một lượt diễn tập theo đúng thứ tự ở mục 4.
- [ ] N xác nhận migration trên schema MySQL mới của máy demo.
- [ ] MT điền commit SHA release candidate và xác nhận không còn blocker/critical.
- [ ] Các task chưa đạt được chuyển sang backlog, không đánh dấu Done giả.
- [ ] MT chuyển CNPM-115 và CNPM-118 sang Done rồi đóng Sprint 5.

## 7. Biên bản diễn tập

Điền ngay sau khi diễn tập:

- Thời gian:
- Release candidate SHA:
- Người tham gia:
- Luồng đã chạy thành công:
- Lỗi phát hiện:
- Người nhận sửa:
- Quyết định Go/No-Go của MT:
