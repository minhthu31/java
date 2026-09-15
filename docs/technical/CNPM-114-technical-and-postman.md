# CNPM-114 — Tài liệu kỹ thuật, cấu hình tích hợp và Postman

> Phiên bản tài liệu: Final API snapshot  
> Ngày cập nhật: 2026-09-14  
> Mục tiêu: tài liệu phải khớp với controller/DTO hiện có, không ghi secret hoặc URL riêng tư.

## 1. Kiến trúc và yêu cầu môi trường

### Backend

- Java 21+
- Spring Boot 4.1.0
- Maven Wrapper (`mvnw.cmd` trên Windows)
- MySQL 8.x
- Flyway quản lý schema
- Backend mặc định: `http://localhost:8080` (Actuator)
- API base: `http://localhost:8080/api/v1`

### Frontend

- Node.js 22+
- npm
- React 18
- Frontend mặc định: `http://localhost:3000`
- API base mặc định: `http://localhost:8080/api/v1`

## 2. Cài đặt và chạy backend

Tại thư mục gốc repository:

```cmd
copy .env.example .env
```

Lưu ý: Spring Boot **không tự đọc `.env`**. Khi chạy bằng CMD, khai báo biến môi trường trực tiếp:

```cmd
set DB_NAME=cnpm_project_support
set DB_USERNAME=cnpm_user
set DB_PASSWORD=<local-db-password>
set JWT_SECRET=<random-secret-at-least-32-bytes>
set INTEGRATION_ENCRYPTION_KEY=<random-secret-at-least-32-characters>
mvnw.cmd spring-boot:run
```

Có thể dùng PowerShell:

```powershell
$env:DB_NAME="cnpm_project_support"
$env:DB_USERNAME="cnpm_user"
$env:DB_PASSWORD="<local-db-password>"
$env:JWT_SECRET="<random-secret-at-least-32-bytes>"
$env:INTEGRATION_ENCRYPTION_KEY="<random-secret-at-least-32-characters>"
.\mvnw.cmd spring-boot:run
```

Kiểm tra:

```text
GET http://localhost:8080/actuator/health
```

Không commit giá trị thật của `DB_PASSWORD`, `JWT_SECRET` hoặc `INTEGRATION_ENCRYPTION_KEY`.

## 3. Cấu hình database

MySQL cần có database và user ứng dụng:

```sql
CREATE DATABASE IF NOT EXISTS cnpm_project_support
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'cnpm_user'@'localhost'
  IDENTIFIED BY '<local-db-password>';

GRANT ALL PRIVILEGES ON cnpm_project_support.* TO 'cnpm_user'@'localhost';
FLUSH PRIVILEGES;
```

Sau khi backend khởi động, Flyway tự chạy migration. Hibernate dùng:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Vì vậy **không dùng `ddl-auto=update` và không sửa migration đã merge**.

Nếu MySQL dùng port khác 3306, đặt:

```cmd
set DB_URL=jdbc:mysql://localhost:<port>/cnpm_project_support?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh
```

## 4. Cài đặt và chạy frontend

Mở terminal thứ hai:

```cmd
cd frontend
copy .env.example .env
npm ci
npm start
```

`frontend/.env.example`:

```env
REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
```

Mở:

```text
http://localhost:3000
```

Frontend gửi JWT trong header:

```http
Authorization: Bearer <accessToken>
```

## 5. Luồng đăng nhập

### POST `/api/v1/auth/login`

Request:

```json
{
  "usernameOrEmail": "<username-or-email>",
  "password": "<password>"
}
```

Response thành công có dạng:

```json
{
  "data": {
    "accessToken": "<JWT>",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "username": "<username>",
    "email": "<email>",
    "fullName": "<full-name>",
    "role": "TEAM_LEADER",
    "id": 1,
    "projectId": 1
  },
  "timestamp": "<ISO-8601>"
}
```

Không ghi JWT thật vào tài liệu hoặc collection.

## 6. Cấu hình Jira

### POST/PUT flow

1. Đăng nhập tài khoản có quyền ADMIN.
2. Gửi `PUT /api/v1/projects/{projectId}/integrations/jira/config`.
3. `siteUrl` phải là HTTPS origin, không chứa path/query/user-info.
4. `projectKey` là Jira project key.
5. `email` là tài khoản Jira dùng cho API token.
6. `apiToken` chỉ gửi qua request và được backend mã hóa; không lưu trong Postman example.
7. `authType` hiện tại là `API_TOKEN`.

Request mẫu:

```json
{
  "siteUrl": "https://your-domain.atlassian.net",
  "projectKey": "CNPM",
  "email": "developer@example.com",
  "apiToken": "<jira-api-token>",
  "authType": "API_TOKEN"
}
```

API đọc config không trả secret:

```http
GET /api/v1/projects/{projectId}/integrations/jira/config
```

Kiểm tra:

```http
POST /api/v1/projects/{projectId}/integrations/jira/test-connection
```

Các API Jira chính:

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | `/projects/{projectId}/integrations/jira/config` | Đọc config đã che secret |
| PUT | `/projects/{projectId}/integrations/jira/config` | Lưu config |
| POST | `/projects/{projectId}/integrations/jira/test-connection` | Kiểm tra kết nối |
| GET | `/projects/{projectId}/integrations/jira/issues/{jiraIssueKey}` | Đọc issue |
| POST | `/projects/{projectId}/integrations/jira/tasks/{taskId}/sync` | Local Task → Jira |
| POST | `/projects/{projectId}/integrations/jira/tasks/{taskId}/retry` | Retry đồng bộ |
| POST | `/projects/{projectId}/integrations/jira/sync` | Jira → local |
| GET | `/projects/{projectId}/integrations/jira/sync/failed/latest` | SyncLog lỗi gần nhất |

Hai endpoint sync Task yêu cầu header bắt buộc:

```http
Idempotency-Key: <unique-key>
```

## 7. Cấu hình GitHub

1. Đăng nhập tài khoản ADMIN để lưu cấu hình.
2. Gửi owner và repository name đúng repository cần đồng bộ.
3. Có thể gửi API version; nếu bỏ trống backend dùng giá trị mặc định hiện tại.
4. Không commit access token.

Request mẫu:

```json
{
  "repositoryOwner": "your-org",
  "repositoryName": "your-repository",
  "accessToken": "<github-token>",
  "apiVersion": "2026-03-10"
}
```

API chính:

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | `/projects/{projectId}/integrations/github/config` | Đọc config |
| PUT | `/projects/{projectId}/integrations/github/config` | Lưu config |
| POST | `/projects/{projectId}/integrations/github/test-connection` | Kiểm tra kết nối |
| POST | `/projects/{projectId}/integrations/github/sync` | Đồng bộ commit + PR |
| POST | `/projects/{projectId}/integrations/github/check-runs/sync` | Đồng bộ check runs |
| GET | `/projects/{projectId}/integrations/github/check-runs?repositoryId={id}` | Xem check runs |
| GET | `/projects/{projectId}/integrations/github/repositories/{repositoryId}/commits` | Xem commits |
| GET | `/projects/{projectId}/integrations/github/repositories/{repositoryId}/pull-requests` | Xem PR |
| GET | `/projects/{projectId}/integrations/github/activities` | Xem activities |
| GET | `/projects/{projectId}/integrations/github/tasks/{taskId}/activities` | Activity của task |
| PUT | `/projects/{projectId}/integrations/github/members/{userId}/account-link` | Link tài khoản GitHub |
| GET | `/projects/{projectId}/integrations/github/members/unlinked` | Thành viên chưa link |

Các API phân trang dùng `page` và `size`; size của GitHub activity hiện giới hạn 1–100.

## 8. Task và Requirement

### Task

- `GET /api/v1/projects/{projectId}/tasks`
- `POST /api/v1/projects/{projectId}/tasks`
- `GET /api/v1/projects/{projectId}/tasks/{taskId}`
- `PUT /api/v1/projects/{projectId}/tasks/{taskId}`
- `PATCH /api/v1/projects/{projectId}/tasks/{taskId}/status`
- `PATCH /api/v1/projects/{projectId}/tasks/{taskId}/assignee`
- `DELETE /api/v1/projects/{projectId}/tasks/{taskId}`

Request tạo task:

```json
{
  "title": "Implement login validation",
  "description": "Validate username and password",
  "acceptanceCriteria": "Invalid credentials return an API error",
  "issueType": "TASK",
  "priority": "HIGH",
  "classification": "NEW_FEATURE",
  "requirementId": null,
  "featureId": null,
  "sprintId": null,
  "assigneeUserId": null,
  "deadline": null
}
```

Giá trị status hiện tại: `TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `BLOCKED`, `CANCELLED`.

### Requirement

- `GET /api/v1/projects/{projectId}/requirements`
- `POST /api/v1/projects/{projectId}/requirements`
- `GET /api/v1/projects/{projectId}/requirements/{requirementId}`
- `PUT /api/v1/projects/{projectId}/requirements/{requirementId}`
- `PATCH /api/v1/projects/{projectId}/requirements/{requirementId}/status`
- `DELETE /api/v1/projects/{projectId}/requirements/{requirementId}`

Request tạo mẫu:

```json
{
  "title": "User authentication",
  "actor": "Team Member",
  "description": "User can sign in",
  "precondition": "Account exists",
  "mainFlow": "Enter credentials and submit",
  "alternativeFlow": "Use email instead of username",
  "exceptionFlow": "Invalid credentials",
  "postcondition": "User receives access token",
  "priority": "HIGH",
  "status": "DRAFT"
}
```

## 9. Báo cáo

### GET `/api/v1/projects/{projectId}/reports/progress`

Query parameters đều tùy chọn:

```text
sprintId=<positive-number>
memberId=<positive-number>
from=<ISO-8601-instant>
to=<ISO-8601-instant>
```

Ví dụ:

```text
GET /api/v1/projects/1/reports/progress?sprintId=2&from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z
```

### GET `/api/v1/projects/{projectId}/reports/summary`

Dùng cùng bộ filter.

Report được tổng hợp từ dữ liệu local/Jira/GitHub theo contract reporting hiện tại; dữ liệu thiếu deadline hoặc assignee không được làm hỏng toàn bộ báo cáo.

## 10. Project members

```http
GET /api/v1/projects/{projectId}/members
```

API này trả danh sách thành viên active của project và yêu cầu quyền Team Leader.

## 11. Chuẩn response và lỗi

Các API dùng envelope tương ứng:

```json
{
  "data": {},
  "timestamp": "2026-09-14T06:00:00Z"
}
```

API dùng `ApiResponse` cũng có `data` và metadata theo implementation hiện tại.

Không dựa vào message lỗi để xử lý nghiệp vụ. Với Jira sync, client nên kiểm tra HTTP status và error code. Một số mã quan trọng:

- `SYNC_ALREADY_RUNNING` → 409
- `IDEMPOTENCY_KEY_REUSED` → 409
- `DUPLICATE_REMOTE_ISSUE` → 409
- `JIRA_AUTHENTICATION_FAILED` → 401
- `JIRA_AUTHORIZATION_FAILED` → 403
- `JIRA_RESOURCE_NOT_FOUND` → 404
- `JIRA_CONNECTION_FAILED` / `JIRA_SYNC_FAILED` → 502

## 12. Postman

Import:

```text
docs/postman/CNPM-Final-API-Collection.json
docs/postman/CNPM-Final-API-Environment.example.json
```

Environment gồm:

- `baseUrl`
- `projectId`
- `token_admin`: dùng cho API quản trị/tích hợp yêu cầu ADMIN.
- `token_leader`: token của tài khoản có role `TEAM_LEADER`; dùng cho các API yêu cầu Leader của project, ví dụ đồng bộ Jira từ Jira về local.
- `token_member`: token của tài khoản có role `TEAM_MEMBER`; dùng cho các API được phép của thành viên.
- `login_admin_username` / `login_admin_password`: thông tin đăng nhập mẫu của tài khoản ADMIN (để trống và tự điền khi chạy).
- `login_leader_username` / `login_leader_password`: thông tin đăng nhập mẫu của tài khoản TEAM_LEADER (để trống và tự điền khi chạy).
- `login_member_username` / `login_member_password`: thông tin đăng nhập mẫu của tài khoản TEAM_MEMBER (để trống và tự điền khi chạy).

Trong Postman, chạy `Login as Team Leader` để tự lưu access token vào `token_leader`, hoặc `Login as Team Member` để lưu vào `token_member`. Không dùng `token_admin` cho các endpoint yêu cầu Leader/Member. Không commit mật khẩu/token thật vào repository.
- `token_leader`: dùng cho API quản lý Task/Requirement/Project members và báo cáo.
- `token_member`: dùng cho API mà TEAM_MEMBER được phép xem/cập nhật trạng thái Task và dữ liệu GitHub theo quyền.
- Jira placeholder values
- GitHub placeholder values
- `taskId`, `requirementId`, `repositoryId`, `jiraIssueKey`

**Không điền token thật rồi commit environment vào Git.**

Luồng demo khuyến nghị:

1. Login → lưu token.
2. Lấy/cấu hình Jira hoặc GitHub.
3. Test connection.
4. Tạo Task/Requirement.
5. Sync Jira hoặc GitHub.
6. Kiểm tra dữ liệu đã đồng bộ.
7. Mở report progress/summary.

## 13. Kiểm tra trước khi merge CNPM-114

```cmd
mvnw.cmd clean verify
cd frontend
npm test -- --runInBand
npm run build
```

Checklist:

- [x] Hướng dẫn backend.
- [x] Hướng dẫn frontend.
- [x] Hướng dẫn MySQL + Flyway.
- [x] Hướng dẫn Jira.
- [x] Hướng dẫn GitHub.
- [x] Postman collection final.
- [x] Postman environment example.
- [x] Placeholder thay cho token/password/private URL.
- [x] Endpoint lấy từ controller hiện tại.
- [x] Request examples khớp DTO chính.

### Postman authentication variables

The Postman environment provides separate credentials for each application role. Fill these values in your local Postman environment before running the login requests:

- `login_admin_username` / `login_admin_password` → **ADMIN** → saved automatically as `token_admin`.
- `login_leader_username` / `login_leader_password` → **TEAM_LEADER** → saved automatically as `token_leader`.
- `login_member_username` / `login_member_password` → **TEAM_MEMBER** → saved automatically as `token_member`.

The three login requests send `Content-Type: application/json`. Do not put real passwords or tokens into the committed example environment file.
