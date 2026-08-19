# Sprint 2 Kickoff

## Sprint Goal

Hoàn thành lát cắt nghiệp vụ đầu tiên gồm Requirement, Task và SRS local; các branch sử dụng chung contract, trạng thái và permission trước khi triển khai đồng bộ Jira/GitHub.

## Thời gian

- Bắt đầu: 15/08/2026.
- Thời lượng: 1 tuần.
- Checkpoint bắt buộc: giữa Sprint.

## Work streams

| Work stream | Đầu ra | Contract phụ thuộc |
|---|---|---|
| Requirement backend | CRUD, filter, status, permission | Section 4 và 7 |
| Task backend | CRUD, assignment, status, permission | Section 5 và 7 |
| SRS backend | Generate, list, metadata, download | Section 6 và 7 |
| Frontend | Màn hình Requirement/Task dùng API adapter | Section 2-6 |
| Test/Security | Unit, API và permission tests | Section 3 và 7 |
| Integration | Chuẩn bị Jira sync, không ghi đè local contract | Section 3.6 và 8 |

## Quy tắc làm việc

1. Mỗi task có một primary owner và một reviewer/backup.
2. Branch và commit phải có Jira key.
3. Không thay đổi contract trong PR triển khai; đề xuất thay đổi phải cập nhật tài liệu trước.
4. Checkpoint giữa Sprint: có code chạy hoặc test/PoC; chỉ báo cáo miệng không được tính.
5. Task trễ checkpoint được Team Leader chuyển cho backup và ghi nhận đóng góp thực tế.
6. Chỉ chuyển `Done` sau khi đáp ứng Acceptance Criteria, test xanh, review và merge `main`.

## Definition of Ready cho task con

- Có Jira description và Acceptance Criteria rõ ràng.
- Chỉ ra section contract áp dụng.
- Xác định dependency và dữ liệu test.
- Có primary owner, reviewer và backup khi rủi ro cao.

## Definition of Done

- Endpoint/DTO đúng contract.
- Phân quyền kiểm tra ở backend.
- Có test happy path, validation, 401, 403 và out-of-scope.
- Không có secret trong code/log/PR.
- Có hướng dẫn kiểm tra hoặc Postman example.
- Build xanh và merge `main`.

