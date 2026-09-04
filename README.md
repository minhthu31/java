# CNPM Project Support Backend

Backend cho hệ thống hỗ trợ quản lý yêu cầu và tiến độ đồ án CNPM thông qua Jira và GitHub.

## Phạm vi

Website không thay thế Jira hoặc GitHub:

- Jira là nguồn chuẩn của requirement, issue, task, Sprint và trạng thái sau đồng bộ.
- GitHub là nguồn chuẩn của repository, commit, Pull Request và workflow.
- Database nội bộ là nguồn chuẩn của tài khoản, nhóm, phân quyền, mapping, báo cáo, SRS snapshot và log.

## Công nghệ

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC, Validation, Data JPA, Security, Actuator
- MySQL 8.4
- Flyway database migration
- JUnit 5, Spring Security Test và H2
- Maven Wrapper

## Chạy local

Yêu cầu: JDK 21+, Docker Desktop.

1. Sao chép `.env.example` thành `.env` và thay toàn bộ mật khẩu mẫu.
2. Khởi động MySQL:

   ```bash
   docker compose --env-file .env up -d mysql
   ```

3. Đặt biến môi trường trong IDE theo `.env`, sau đó chạy:

   ```bash
   ./mvnw spring-boot:run
   ```

   Trên Windows dùng `mvnw.cmd spring-boot:run`.

4. Kiểm tra: `GET http://localhost:8080/actuator/health`.

Không đưa `.env`, token Jira/GitHub, mật khẩu database hoặc key mã hóa lên Git.

## Kiểm thử

```bash
./mvnw clean verify
```

Test dùng profile `test` và H2, không phụ thuộc MySQL của thành viên.

## Cấu trúc source

```text
vn.edu.cnpm.projectsupport
├── common          response/error, persistence base, correlation ID
├── security        cấu hình bảo mật dùng chung
├── auth            đăng nhập, đăng xuất, refresh/session
├── identity        user, role, external-account mapping
├── group           group, member, Lecturer và Leader assignment
├── project         project, feature và sprint
├── requirement     requirement và SRS version/export
├── task            task, assignment, classification, transition
├── integration
│   ├── jira        Jira client, mapping, pagination, retry/idempotency
│   └── github      GitHub client, rate limit, commit/PR/workflow ingestion
├── linking         Task-Commit/PR auto/manual link
├── autotest        trạng thái workflow/check run
├── reporting       progress/contribution report
├── audit           activity timeline
└── monitoring      sync log, cảnh báo và abnormal data
```

Mỗi module nên tổ chức tiếp thành `controller`, `service`, `domain`, `repository`, `dto` và `mapper` khi bắt đầu phát triển. Không tạo một thư mục controller/service khổng lồ dùng chung cho mọi nghiệp vụ.

## Tài liệu thiết kế

Bộ tài liệu thiết kế đã đối chiếu với source code và Flyway migration hiện tại:

- [CNPM-18, CNPM-19 và CNPM-20](docs/system-design/README.md)
- [Kiến trúc hệ thống](docs/system-design/architecture.md)
- [ERD](docs/system-design/erd.md)
- [Data Dictionary](docs/system-design/database-design.md)

Các sơ đồ nguồn Draw.io nằm cùng thư mục để nhóm có thể tiếp tục chỉnh sửa và xuất PNG/PDF khi nộp bài.

## Database

Flyway là nguồn chuẩn của schema. Không dùng `spring.jpa.hibernate.ddl-auto=update`.

- `V1__create_core_schema.sql`: schema theo Data Dictionary baseline 2.0.
- `V2__seed_roles.sql`: bốn role nghiệp vụ.
- Mỗi thay đổi schema tạo migration mới; không sửa migration đã merge vào `main`.

## Quy tắc kiến trúc

- Controller chỉ nhận/validate request và trả DTO; không trả JPA entity.
- Service giữ nghiệp vụ, phân quyền theo phạm vi `group_id` và transaction.
- Repository chỉ truy xuất dữ liệu.
- Adapter Jira/GitHub không được nằm trong controller.
- Mọi thao tác tạo Jira issue phải idempotent; retry không tạo issue trùng.
- Mọi danh sách từ API ngoài phải xử lý pagination và rate limit.
- Không log token, password, Authorization header hoặc payload nhạy cảm.
- Liên kết tự động chỉ tạo sau khi Issue Key được xác minh trong Jira project của nhóm.
- Liên kết thủ công phải ghi actor, thời gian và lý do.
- Không dùng riêng số commit để kết luận mức đóng góp hoặc điểm số.

## Báo cáo Sprint

- [CNPM-50 - Tích hợp và kiểm tra kết quả Sprint 1](docs/sprint-reports/CNPM-50-sprint-1-integration-test-report.md)
- [CNPM-51 - Tổng kết Sprint 1 và tạo Sprint 2 Backlog](docs/sprint-reports/CNPM-51-sprint-1-review-and-sprint-2-backlog.md)

## Sprint 2

- [CNPM-52 - API Contract](docs/sprint-2-api-contract.md)
- [Sprint 2 Kickoff](docs/sprint-2-kickoff.md)
- [CNPM-72 - Báo cáo tích hợp Sprint 2](docs/sprint-reports/CNPM-72-sprint-2-integration-report.md)
- [CNPM-73 - Tổng kết Sprint 2 và Sprint 3 Backlog](docs/sprint-reports/CNPM-73-sprint-2-review-and-sprint-3-backlog.md)

## Sprint 3

- [CNPM-74 - Jira field mapping và API contract](docs/integrations/CNPM-74-jira-field-mapping-and-api-contract.md)
- [CNPM-74 - OpenAPI Jira Integration v1](docs/api/jira-integration-v1.openapi.yaml)

## Quy trình Git

- Branch: `<type>/<JIRA-KEY>-<short-description>`
- Commit: `<JIRA-KEY> <mô tả có ý nghĩa>`
- Pull Request: `<JIRA-KEY> <kết quả>`

Ví dụ: `feature/CNPM-30-login-api` và `CNPM-30 Implement login validation`.

Trước khi merge: build xanh, không có secret, có test phù hợp, có reviewer và đáp ứng Acceptance Criteria của Jira task.
