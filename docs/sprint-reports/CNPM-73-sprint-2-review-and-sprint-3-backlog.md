# CNPM-73 - Tổng kết Sprint 2 và tạo Sprint 3 Backlog

## 1. Sprint 2 Review

### Sprint Goal

Leader quản lý Requirement và Task trong database nội bộ, phân quyền đúng bốn vai trò và tạo được lát cắt demo frontend/backend trước khi bắt đầu tích hợp Jira.

### Kết quả chính

- Hoàn thành entity/repository, API contract, service và controller cho Requirement và Task.
- Hoàn thành validation, exception, project/group authorization và activity logging.
- Hoàn thành giao diện danh sách/form Requirement và giao diện quản lý Task.
- Bổ sung test Service, API, phân quyền và một bài test end-to-end cho luồng demo.
- Khôi phục `main` sau các lần merge nhầm và loại bỏ `PermissionEvaluatorService` hard-code.
- Hoàn thành migration dữ liệu demo và SRS Preview trong PR closeout.

## 2. Đối chiếu 22 work item

Trạng thái dưới đây dựa trên source/commit của GitHub tại ngày 23/08/2026. Đây là trạng thái nghiệm thu kỹ thuật, không tự động thay đổi status Jira.

| Task | Nội dung rút gọn | Trạng thái nghiệm thu | Bằng chứng/Ghi chú |
|---|---|---|---|
| CNPM-52 | Kickoff và API contract | Đạt trên `main` | `38f375e`, tài liệu API contract |
| CNPM-53 | Migration Sprint 2 | Đạt trong PR closeout | Flyway V4 chạy trên database mới |
| CNPM-54 | Project/Requirement repository | Đạt trên `main` | `099196b`, `f9b9660` |
| CNPM-55 | Requirement DTO/contract | Đạt trên `main` | PR #87, `69211f3` |
| CNPM-56 | Requirement Service | Đạt trên `main` | `1ebc4f2`; được Mạnh tiếp quản/chốt |
| CNPM-57 | Requirement Controller | Đạt trên `main` | PR #121, `17b429a`; được Mạnh chốt |
| CNPM-58 | Feature/Sprint/Task entity/repository | Đạt trên `main` | `24b2986` |
| CNPM-59 | Requirement validation/exception | Đạt trên `main` | `d1bf9ae` |
| CNPM-60 | Task Service | Đạt trên `main` | `8c186c3`; được Mạnh tiếp quản/chốt |
| CNPM-61 | Task API/controller | Đạt trên `main` | PR #121, `951943d`; được Mạnh chốt |
| CNPM-62 | Dữ liệu mẫu | Đạt sau sửa thay thế trong closeout | Nhánh cũ dùng enum/user sai; V4 mới thay thế bằng dữ liệu hợp lệ |
| CNPM-63 | Requirement list UI | Đạt trên `main` | PR #102 |
| CNPM-64 | Requirement form | Đạt sau khi nối luồng trong closeout | PR #108 có form; CNPM-72 nối create/edit thật |
| CNPM-65 | Task management UI | Đạt trong PR closeout | Code Nguyễn Hương; Mạnh tích hợp với backend/current main |
| CNPM-66 | Task filter và label | **Chưa đạt DoD** | Chỉ có mock UI trên nhánh `feature/CNPM-66-task-filter-and-labels` |
| CNPM-67 | Phân quyền Requirement/Task/SRS | Đạt trên `main` | `06e7130`; được Mạnh tiếp quản/chốt |
| CNPM-68 | Test Requirement/Task Service | Đạt trên `main` | `e4188ed` |
| CNPM-69 | Test API và phân quyền | Đạt trên `main` | `f402bb5` |
| CNPM-70 | Hướng dẫn Postman | **Chưa đạt DoD** | Nhánh chưa merge; report cũ sai cấu trúc và chưa có run evidence |
| CNPM-71 | Review chéo/tiếp quản task rủi ro | Đạt nội dung, cần xác nhận Jira | Các lần tiếp quản 56/57/60/61/67/68/69 được ghi ở bảng này |
| CNPM-72 | Tích hợp backend/frontend | Sẵn sàng review trong PR closeout | Báo cáo CNPM-72 và automated test |
| CNPM-73 | Tổng kết Sprint 2/Sprint 3 backlog | Sẵn sàng review trong PR closeout | Tài liệu hiện tại |

Nếu PR closeout được merge, kết quả dự kiến là **20/22 task đạt hoặc sẵn sàng Done**. CNPM-66 và CNPM-70 phải được chuyển sang Sprint 3, không đổi sang Done chỉ để đóng Sprint.

## 3. Task trễ và task được tiếp quản

- CNPM-56, CNPM-60 và CNPM-67 được Mạnh Phạm Tuấn tiếp quản, chuẩn hóa trên shared schema rồi merge bằng PR tổng hợp.
- CNPM-57 và CNPM-61 từng bị merge lẫn/khôi phục; Mạnh tạo lại hai commit độc lập và merge qua PR #121.
- CNPM-68 và CNPM-69 có nhánh cũ thiếu/sai test; Mạnh hoàn thiện lại thành hai commit `e4188ed` và `f402bb5`.
- CNPM-62 có migration nhánh cũ tham chiếu username và enum không tồn tại; migration V4 trong closeout là bản thay thế có kiểm thử.
- CNPM-65 do Nguyễn Hương phát triển; phần tích hợp với `main`, project context, Member API và luồng demo được thực hiện trong CNPM-72.
- Commit `3452288` thêm phân quyền hard-code trực tiếp vào `main`; thay đổi này đã được loại bỏ trong CNPM-69 để giữ `ProjectAuthorizationService` làm nguồn chuẩn.

## 4. Sprint 3 Backlog đề xuất

Tên Sprint: **Sprint 3 - Jira Integration & Synchronization**

Thời gian đề xuất: **24/08/2026 - 31/08/2026**

Sprint Goal: Kết nối Jira Cloud bằng cấu hình an toàn, đồng bộ Requirement/Task có idempotency, pagination, retry và log đủ để demo.

Jira sẽ cấp key thật khi tạo work item; mã S3 bên dưới chỉ thể hiện thứ tự và phụ thuộc.

| Thứ tự | Work item | Epic | Ngày | Phụ thuộc | Owner theo chuyên môn |
|---|---|---|---|---|---|
| S3-01 | Hoàn thiện Task filter và classification label (carryover CNPM-66) | Requirements Analysis | 24/08 | Task API hiện tại | Frontend Task |
| S3-02 | Sửa Postman collection và lưu run evidence (carryover CNPM-70) | Integration Research | 24/08 | Main sau closeout | API/Test |
| S3-03 | Xây dựng API cấu hình Jira an toàn | Backend Foundation | 24-25/08 | Migration/config hiện tại | Backend/Security |
| S3-04 | Xây dựng Jira client và kiểm tra kết nối | Integration Research | 25/08 | S3-03 | Jira research |
| S3-05 | Mapping Project, Sprint, Requirement và Task với Jira | System Design | 26/08 | S3-04 | Backend/Database |
| S3-06 | Import backlog/sprint/issue từ Jira có pagination | Integration Research | 27/08 | S3-05 | Jira integration |
| S3-07 | Đẩy Requirement/Task lên Jira có idempotency | Backend Foundation | 28/08 | S3-05 | Backend Task/Requirement |
| S3-08 | Đồng bộ trạng thái, retry và sync log | Backend Foundation | 29/08 | S3-06, S3-07 | Backend/Monitoring |
| S3-09 | Giao diện cấu hình và trạng thái đồng bộ Jira | System Design | 29-30/08 | S3-03, S3-08 | Frontend |
| S3-10 | Test Jira adapter/API/phân quyền bằng mock | Authentication and Authorization | 30/08 | S3-06, S3-08 | Test/Security |
| S3-11 | Tích hợp, demo Jira và tổng kết Sprint 3 | Project Setup | 31/08 | S3-01 đến S3-10 | Team Leader |

## 5. Thứ tự đóng Sprint 2 trên Jira

1. Tạo PR cho nhánh closeout và đính kèm kết quả backend test, frontend test/build và migration.
2. Có ít nhất một reviewer khác người thực hiện.
3. Merge PR vào `main` và xác nhận CI xanh.
4. Chuyển các task đạt ở bảng trên sang Done nếu Jira đã có PR/test evidence.
5. Chuyển CNPM-66 và CNPM-70 sang Sprint 3, giữ nguyên lịch sử và ghi lý do carryover.
6. Xác nhận CNPM-71, CNPM-72 và CNPM-73 có đủ bằng chứng rồi mới chuyển Done.
7. Nhấn **Complete sprint** và kiểm tra Jira chuyển hai carryover sang Sprint 3.

## 6. Kết luận

Sprint 2 tạo được lát cắt nghiệp vụ local hoàn chỉnh để bắt đầu Jira integration. Điểm cần giữ khi vấn đáp là nhóm không coi branch/commit đơn lẻ là hoàn thành: chỉ `main` có PR, test và Acceptance Criteria mới được tính Done.
