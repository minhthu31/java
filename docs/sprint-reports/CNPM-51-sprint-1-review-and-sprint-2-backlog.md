# CNPM-51 - Tổng kết Sprint 1 và tạo Sprint 2 Backlog

## 1. Sprint 1 Review

### Sprint Goal

Tạo nền tảng backend, database, đăng nhập và phân quyền cho bốn vai trò để nhóm có thể phát triển các nghiệp vụ Jira/GitHub trong Sprint 2.

### Kết quả đạt được

- Có Spring Boot project skeleton và quy tắc kiến trúc thống nhất.
- Kết nối MySQL bằng cấu hình môi trường; Flyway quản lý schema.
- Có User, Role, repository và bốn tài khoản kiểm thử.
- Có đăng nhập bằng username hoặc email, BCrypt và JWT.
- Có 401/403, validation và exception response thống nhất.
- Có role-based access control cho Admin, Lecturer, Team Leader và Team Member.
- Có giao diện đăng nhập và trang đích cơ bản theo vai trò.
- Có test tự động cho database contract, authentication, JWT và security.
- Có Architecture, ERD và Data Dictionary bám theo source hiện tại.

### Hạn chế và technical debt

- Chưa triển khai nghiệp vụ quản lý requirement/task hoàn chỉnh.
- Chưa kết nối thật với Jira Cloud và GitHub REST API.
- Chưa có cơ chế refresh token/logout hoàn chỉnh.
- Các trang theo vai trò hiện là trang đích cơ bản, chưa phải dashboard đầy đủ.
- Maven Wrapper trên một số PowerShell/Windows cần chạy bằng IntelliJ hoặc Maven distribution đã tải sẵn.
- Không sửa migration đã merge; mọi thay đổi schema phải tạo migration mới.

### Bài học quy trình

- Không nghiệm thu theo số lượng commit; phải đối chiếu Acceptance Criteria và chạy thử.
- Một work item chỉ chuyển `Done` khi có PR, reviewer và bằng chứng kiểm tra.
- Task có rủi ro trễ hạn phải có người backup và checkpoint giữa sprint.
- Không cho phép một PR tự ý thay giao diện hoặc sửa vượt phạm vi task.
- Mỗi endpoint phải chốt contract trước khi backend và frontend làm song song.

## 2. Definition of Done áp dụng từ Sprint 2

Một task chỉ được chuyển `Done` khi đáp ứng đủ:

- Code/tài liệu nằm trên branch đúng quy ước và có Jira key trong commit/PR.
- Đáp ứng toàn bộ Acceptance Criteria.
- Build và test liên quan chạy thành công.
- Không chứa password, token, secret hoặc dữ liệu cá nhân.
- Có reviewer khác người thực hiện.
- Có hướng dẫn kiểm tra hoặc ảnh/Postman response khi phù hợp.
- Đã merge vào `main`; chỉ tồn tại trên branch không được tính là hoàn thành.

## 3. Sprint 2 Goal

Hoàn thành lát cắt nghiệp vụ đầu tiên: cấu hình tích hợp, quản lý requirement/task nội bộ, đồng bộ Jira cơ bản, thu thập GitHub cơ bản và hiển thị dữ liệu thật trên giao diện.

Thời lượng đề xuất: **1 tuần**.

## 4. Sprint 2 Backlog

Các mã bên dưới tiếp tục từ CNPM-52. Nếu Jira đã cấp mã khác, giữ nguyên nội dung và thay bằng mã thực tế.

| Key | Work item | Epic đề xuất | Sản phẩm chính | Phụ thuộc | Mức độ |
|---|---|---|---|---|---|
| CNPM-52 | Khởi tạo Sprint 2 và chốt API contract | Project Setup | Sprint goal, contract, branch/PR rules | Sprint 1 closure | High |
| CNPM-53 | Tạo API quản lý cấu hình Jira/GitHub | Backend Foundation | DTO, service, controller; secret được mã hóa | CNPM-52 | High |
| CNPM-54 | Xây dựng Jira REST client và xác thực kết nối | Integration Research | Jira client, `/myself`, error mapping | CNPM-53 | High |
| CNPM-55 | Đồng bộ project và issue từ Jira | Integration Research | Sync service, mapping, pagination, sync log | CNPM-54 | High |
| CNPM-56 | Xây dựng Requirement API | Backend Foundation | CRUD/list/detail, validation, permission | CNPM-52 | High |
| CNPM-57 | Xây dựng Task API và phân công thành viên | Backend Foundation | CRUD, assign, status transition | CNPM-56 | High |
| CNPM-58 | Xây dựng GitHub REST client | Integration Research | Repository/commit/PR client, rate limit | CNPM-53 | High |
| CNPM-59 | Thu thập commit và pull request GitHub | Integration Research | Ingestion service, persistence, sync log | CNPM-58 | High |
| CNPM-60 | Liên kết Task - Commit - Pull Request | Backend Foundation | Auto/manual linking, audit information | CNPM-57, CNPM-59 | Medium |
| CNPM-61 | Giao diện cấu hình tích hợp | System Design | Form Jira/GitHub, test connection, error state | CNPM-53 | Medium |
| CNPM-62 | Giao diện Requirement và Task | System Design | Danh sách, chi tiết, tạo/giao task | CNPM-56, CNPM-57 | High |
| CNPM-63 | Kiểm thử Jira/GitHub client bằng mock | Integration Research | Unit/contract tests, error/rate-limit cases | CNPM-54, CNPM-58 | High |
| CNPM-64 | Kiểm thử phân quyền nghiệp vụ Sprint 2 | Authentication and Authorization | Permission tests theo actor/group scope | CNPM-56, CNPM-57 | High |
| CNPM-65 | Tích hợp, demo và Sprint 2 Review | Project Setup | Main xanh, demo script, report | Tất cả task Sprint 2 | High |

## 5. Quy tắc phân công cho 7 thành viên

- Team Leader giữ CNPM-52 và CNPM-65, chịu trách nhiệm contract, tích hợp và nghiệm thu.
- Người đã làm database/backend foundation ưu tiên CNPM-53, CNPM-56 hoặc CNPM-57.
- Người đã làm authentication/security ưu tiên CNPM-64 và review quyền của CNPM-56/57.
- Người đã làm frontend HM ưu tiên CNPM-61/62; không để PR tích hợp thay đổi thiết kế ngoài phạm vi.
- Người đã nghiên cứu Jira ưu tiên CNPM-54/55/63.
- Người đã nghiên cứu GitHub ưu tiên CNPM-58/59/60.
- Thành viên có lịch sử nộp trễ vẫn được gán task chính trên Jira để đánh giá đóng góp, nhưng mỗi task phải có backup owner khác và checkpoint sau 50% thời gian sprint. Backup được phép tiếp quản ngay khi checkpoint không đạt.

Không nhân đôi code giữa primary và backup. Hai người phối hợp trên cùng contract; backup review sớm, giữ test/PoC và chỉ tiếp quản branch khi Team Leader xác nhận.

## 6. Kế hoạch một tuần

| Ngày | Mốc |
|---|---|
| Ngày 1 | Chốt contract, owner/backup và chuẩn bị cấu hình test |
| Ngày 2 | Hoàn thành client/API skeleton; FE dùng mock contract |
| Ngày 3 | Checkpoint giữa sprint; tiếp quản task trễ nếu cần |
| Ngày 4 | Hoàn thành nghiệp vụ chính, unit/contract test |
| Ngày 5 | Tích hợp backend - frontend - database |
| Ngày 6 | Fix lỗi, regression test và chuẩn bị demo |
| Ngày 7 | Sprint Review, Retrospective và chốt Sprint 3 backlog |

## 7. Điều kiện khép Sprint 1 trên Jira

1. Merge PR tài liệu CNPM-18/19/20.
2. Merge PR CNPM-50/51.
3. Chuyển các task đã merge và đạt Acceptance Criteria sang `Done`.
4. Task chưa hoàn thành phải chuyển sang Sprint 2, ghi lý do và người tiếp nhận; không để ở Sprint 1 chỉ để làm đẹp báo cáo.
5. Kiểm tra Sprint Report, lưu số lượng Done/Not Done và ghi nhận phần việc Team Leader đã tiếp quản.
6. Nhấn `Complete sprint`, sau đó xác nhận các task chưa xong được chuyển sang Sprint 2.

