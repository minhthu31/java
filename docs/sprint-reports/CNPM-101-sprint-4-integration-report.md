# CNPM-101 - Báo cáo tích hợp và tổng kết Sprint 4

## 1. Mục tiêu

Hoàn thiện luồng GitHub Integration trên nền `main`: Admin cấu hình và đồng bộ GitHub, hệ thống nhập commit và Pull Request, tự liên kết hoạt động có Jira Issue Key với Task, sau đó hiển thị hoạt động liên quan trong chi tiết Task.

## 2. Phạm vi tích hợp

| Nhóm | Task | Kết quả |
| --- | --- | --- |
| Contract, persistence và REST client | CNPM-88 đến CNPM-90 | Đã có trên `main` |
| API và giao diện cấu hình | CNPM-91, CNPM-92 | Đã có trên `main` |
| Repository, commit và Pull Request | CNPM-93 đến CNPM-95 | Commit đã có; phần Pull Request được hoàn thiện an toàn trong CNPM-101 |
| Liên kết tài khoản, Task và API hoạt động | CNPM-96 đến CNPM-99 | Đã có trên `main` |
| RBAC và integration test | CNPM-100 | Đã có trên `main` |
| Tích hợp, demo và tổng kết | CNPM-101 | Nhánh hiện tại |

Base tích hợp là commit `ece82dd` của `origin/main`.

## 3. Phần hoàn thiện trong CNPM-101

- Công bố endpoint Admin `POST /api/v1/projects/{projectId}/integrations/github/sync` đúng contract CNPM-88.
- Đồng bộ commit và Pull Request có pagination, upsert, SyncLog, correlation ID và xử lý lỗi từng bản ghi.
- Dùng GitHub client và cấu hình mã hóa hiện có; không truyền token qua controller hoặc frontend.
- Tự nhận diện Jira Issue Key trong branch, commit message, tiêu đề và nội dung Pull Request để liên kết với Task.
- Bổ sung nút đồng bộ ở màn hình cấu hình GitHub và hiển thị kết quả commit/PR.
- Bổ sung bảng hoạt động GitHub trong chi tiết Task.
- Bổ sung kiểm thử REST client, service, RBAC, giao diện cấu hình, service hoạt động và panel chi tiết Task.

## 4. Kịch bản demo

1. Admin lưu cấu hình GitHub và chạy Test Connection.
2. Team Leader tạo Task đã liên kết Jira Issue Key.
3. Thành viên tạo branch, commit hoặc Pull Request có mã `CNPM-xx`.
4. Admin chọn **Đồng bộ GitHub**.
5. Mở chi tiết Task và xác nhận commit/Pull Request liên quan được hiển thị.
6. Lecturer hoặc Team Leader mở dữ liệu hoạt động thành viên theo quyền được cấp.
7. Chạy đồng bộ lần nữa và xác nhận không tạo dữ liệu hoặc liên kết trùng.

## 5. Checklist nghiệm thu

| Hạng mục | Người chịu trách nhiệm | Trạng thái |
| --- | --- | --- |
| Backend full test và Flyway migration | Backend owner | 344 test pass; 11 migration hợp lệ |
| Frontend test và production build | Frontend owner | 95 test pass; build thành công |
| Quét secret trước PR | Team Leader | Không phát hiện GitHub/Jira token hoặc JWT |
| Demo Jira Cloud và GitHub thật | Team Leader/Admin | Chờ token và project thật của nhóm |
| Kiểm tra quyền Lecturer/Leader bằng tài khoản demo | QA/Team Leader | Thực hiện trong buổi nghiệm thu |

## 6. Kết luận

Mã nguồn, kiểm thử và hướng dẫn đã sẵn sàng để tạo Pull Request. CNPM-101 chỉ chuyển sang Done sau khi CI của PR xanh và nhóm chạy thành công kịch bản live bằng tài khoản Jira/GitHub thật; không đưa token hoặc secret vào repository.
