# CNPM-52 - Sprint 2 API Contract

## 1. Trạng thái tài liệu

| Thuộc tính | Giá trị |
|---|---|
| Contract version | `1.0.0` |
| Base path | `/api/v1` |
| Phạm vi | Requirement, Task, SRS |
| Nguồn chuẩn | Tài liệu này và OpenAPI được sinh từ code sau khi triển khai |
| Ngày chốt | 14/08/2026 |

Sau khi contract được merge, thay đổi phá vỡ tương thích phải được Team Leader duyệt và tăng major version. Không đổi tên endpoint, field hoặc enum riêng trong một branch.

## 2. Quy ước chung

- JSON dùng `camelCase`; database dùng `snake_case`.
- ID nội bộ là số nguyên 64-bit dương.
- Thời gian dùng ISO-8601 UTC, ví dụ `2026-08-15T10:30:00Z`.
- Client gửi `Authorization: Bearer <access-token>` cho mọi endpoint trong tài liệu này.
- Header `X-Correlation-ID` là tùy chọn; backend nhận hoặc tự sinh và luôn trả lại.
- `Content-Type: application/json`, trừ endpoint tải file.
- Không trả JPA entity, password, token tích hợp hoặc encrypted secret.
- `page` bắt đầu từ `0`; `size` mặc định `20`, tối đa `100`.
- `sort` có dạng `field,direction`, ví dụ `updatedAt,desc`.
- Mọi truy vấn đều kiểm tra phạm vi project/group ở service, không chỉ ẩn nút trên giao diện.

### 2.1 Success envelope

```json
{
  "success": true,
  "data": {},
  "message": "Success",
  "timestamp": "2026-08-15T10:30:00Z",
  "correlationId": "41cc0c3b-2abd-4f54-bcec-a6f67b31a3a3"
}
```

### 2.2 Page response

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### 2.3 Error response

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "correlationId": "41cc0c3b-2abd-4f54-bcec-a6f67b31a3a3",
  "fieldErrors": {
    "title": "must not be blank"
  },
  "timestamp": "2026-08-15T10:30:00Z"
}
```

| HTTP | Ý nghĩa |
|---|---|
| `200` | Đọc/cập nhật thành công |
| `201` | Tạo thành công |
| `204` | Xóa thành công, không có body |
| `400` | Request/enum/transition không hợp lệ |
| `401` | Chưa đăng nhập hoặc token không hợp lệ |
| `403` | Đã đăng nhập nhưng không có quyền/phạm vi |
| `404` | Không tìm thấy tài nguyên trong phạm vi được phép |
| `409` | Xung đột dữ liệu, version hoặc quan hệ tham chiếu |
| `422` | Dữ liệu đúng cú pháp nhưng không đáp ứng quy tắc nghiệp vụ |
| `500` | Lỗi nội bộ; không trả stack trace |

Các error code chuẩn: `VALIDATION_FAILED`, `UNAUTHENTICATED`, `ACCESS_DENIED`, `RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`, `INVALID_STATUS_TRANSITION`, `RESOURCE_IN_USE`, `ASSIGNEE_OUTSIDE_GROUP`, `SRS_GENERATION_FAILED`, `INTEGRATION_UNAVAILABLE`.

## 3. Enum chuẩn

### 3.1 Priority

```text
HIGHEST, HIGH, MEDIUM, LOW, LOWEST
```

### 3.2 RequirementStatus

```text
DRAFT, APPROVED, SYNCED, ARCHIVED
```

Transition hợp lệ:

- `DRAFT -> APPROVED | ARCHIVED`
- `APPROVED -> DRAFT | SYNCED | ARCHIVED`
- `SYNCED -> APPROVED | ARCHIVED`
- `ARCHIVED` là trạng thái kết thúc trong Sprint 2.

### 3.3 TaskStatus

```text
TO_DO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED, CANCELLED
```

Transition hợp lệ:

- `TO_DO -> IN_PROGRESS | BLOCKED | CANCELLED`
- `IN_PROGRESS -> TO_DO | IN_REVIEW | BLOCKED | CANCELLED`
- `IN_REVIEW -> IN_PROGRESS | DONE | BLOCKED`
- `BLOCKED -> TO_DO | IN_PROGRESS | CANCELLED`
- `DONE` và `CANCELLED` là trạng thái kết thúc.

Team Member không được chuyển trực tiếp `TO_DO -> DONE` và không được tự đặt `CANCELLED`.

### 3.4 IssueType

```text
EPIC, STORY, TASK, BUG, SUBTASK
```

`SUBTASK` chỉ được dùng khi API được mở rộng thêm `parentTaskId`; Sprint 2 chưa cho tạo `SUBTASK` nếu schema chưa có quan hệ cha.

### 3.5 TaskClassification

```text
NEW_FEATURE, FEATURE_RELATED, AUTO_TEST, AUTO_LOG, OTHER
```

### 3.6 SyncStatus

```text
NOT_SYNCED, PENDING, SYNCING, SYNCED, FAILED
```

Client chỉ đọc `syncStatus`. Chỉ sync service được cập nhật giá trị này.

### 3.7 SrsGenerationStatus

```text
GENERATING, GENERATED, FAILED
```

Đây là trạng thái response của quá trình sinh tài liệu; không bắt buộc thêm cột vào `srs_versions` trong Sprint 2.

## 4. Contract Requirement

### 4.1 Endpoint summary

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/projects/{projectId}/requirements` | Danh sách requirement |
| `POST` | `/projects/{projectId}/requirements` | Tạo requirement |
| `GET` | `/projects/{projectId}/requirements/{requirementId}` | Chi tiết requirement |
| `PUT` | `/projects/{projectId}/requirements/{requirementId}` | Cập nhật toàn bộ nội dung |
| `PATCH` | `/projects/{projectId}/requirements/{requirementId}/status` | Chuyển trạng thái |
| `DELETE` | `/projects/{projectId}/requirements/{requirementId}` | Xóa requirement chưa được dùng |

### 4.2 List requirements

```http
GET /api/v1/projects/10/requirements?status=APPROVED&priority=HIGH&keyword=login&page=0&size=20&sort=updatedAt,desc
```

Query tùy chọn: `status`, `priority`, `keyword`, `jiraIssueKey`, `page`, `size`, `sort`.

### 4.3 Create/update request

```json
{
  "title": "Người dùng đăng nhập vào hệ thống",
  "description": "Xác thực bằng username hoặc email",
  "actor": "Admin, Lecturer, Team Leader, Team Member",
  "priority": "HIGH",
  "precondition": "Tài khoản tồn tại và đang ACTIVE",
  "mainFlow": "1. Mở trang đăng nhập\n2. Nhập thông tin\n3. Gửi yêu cầu",
  "alternativeFlow": "Người dùng nhập email thay cho username",
  "exceptionFlow": "Sai thông tin hoặc tài khoản inactive",
  "postcondition": "Người dùng nhận access token",
  "status": "DRAFT"
}
```

Validation:

- `title`: bắt buộc, 1-255 ký tự.
- `priority`: một giá trị `Priority`; có thể `null` khi draft.
- `status` khi tạo chỉ được là `DRAFT` hoặc bỏ trống.
- Các flow là plain text; backend không thực thi HTML từ client.
- `projectId` lấy từ path, không nhận trong body.
- `jiraIssueKey` do integration service gán, client không tự nhập ở API local.

### 4.4 Requirement response

```json
{
  "id": 101,
  "projectId": 10,
  "jiraIssueKey": null,
  "title": "Người dùng đăng nhập vào hệ thống",
  "description": "Xác thực bằng username hoặc email",
  "actor": "Admin, Lecturer, Team Leader, Team Member",
  "priority": "HIGH",
  "precondition": "Tài khoản tồn tại và đang ACTIVE",
  "mainFlow": "...",
  "alternativeFlow": "...",
  "exceptionFlow": "...",
  "postcondition": "Người dùng nhận access token",
  "status": "DRAFT",
  "createdAt": "2026-08-15T02:00:00Z",
  "updatedAt": "2026-08-15T02:00:00Z"
}
```

### 4.5 Change requirement status

```http
PATCH /api/v1/projects/10/requirements/101/status
```

```json
{
  "status": "APPROVED"
}
```

### 4.6 Delete rule

Chỉ Team Leader được xóa requirement ở trạng thái `DRAFT` và chưa có task tham chiếu. Trường hợp còn tham chiếu trả `409 RESOURCE_IN_USE`; dùng `ARCHIVED` thay cho xóa dữ liệu lịch sử.

## 5. Contract Task

### 5.1 Endpoint summary

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/projects/{projectId}/tasks` | Danh sách task trong phạm vi actor |
| `POST` | `/projects/{projectId}/tasks` | Tạo task |
| `GET` | `/projects/{projectId}/tasks/{taskId}` | Chi tiết task |
| `PUT` | `/projects/{projectId}/tasks/{taskId}` | Cập nhật nội dung task |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}/status` | Chuyển trạng thái task |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}/assignee` | Giao hoặc bỏ giao task |
| `DELETE` | `/projects/{projectId}/tasks/{taskId}` | Xóa task chưa đồng bộ/chưa hoạt động |

### 5.2 List tasks

```http
GET /api/v1/projects/10/tasks?status=IN_PROGRESS&assigneeId=4&sprintId=2&keyword=login&page=0&size=20&sort=deadline,asc
```

Query tùy chọn: `status`, `priority`, `issueType`, `classification`, `assigneeId`, `requirementId`, `featureId`, `sprintId`, `syncStatus`, `keyword`, `page`, `size`, `sort`.

Với Team Member, backend bắt buộc lọc theo user hiện tại dù client truyền `assigneeId` khác.

### 5.3 Create/update request

```json
{
  "requirementId": 101,
  "featureId": 12,
  "sprintId": 2,
  "assigneeUserId": 4,
  "title": "Xây dựng Login API",
  "description": "Tạo endpoint đăng nhập theo contract",
  "acceptanceCriteria": "Đăng nhập hợp lệ trả token; sai thông tin trả 401",
  "issueType": "TASK",
  "classification": "FEATURE_RELATED",
  "priority": "HIGH",
  "deadline": "2026-08-20T16:59:59Z"
}
```

Validation:

- `title`: bắt buộc, 1-255 ký tự.
- `acceptanceCriteria`, `issueType`, `priority`: bắt buộc.
- Các ID liên quan phải thuộc cùng `projectId`.
- `assigneeUserId` phải là thành viên ACTIVE của group sở hữu project.
- `deadline` không được trước thời điểm tạo.
- Khi tạo: `status=TO_DO`, `syncStatus=NOT_SYNCED`; client không được gửi hai field này.
- `idempotencyKey` lấy từ header `Idempotency-Key` khi luồng có tạo Jira issue, không lấy từ body.

### 5.4 Task response

```json
{
  "id": 501,
  "projectId": 10,
  "requirementId": 101,
  "featureId": 12,
  "sprintId": 2,
  "assignee": {
    "id": 4,
    "username": "member.test",
    "displayName": "Test Member"
  },
  "title": "Xây dựng Login API",
  "description": "Tạo endpoint đăng nhập theo contract",
  "acceptanceCriteria": "Đăng nhập hợp lệ trả token; sai thông tin trả 401",
  "issueType": "TASK",
  "classification": "FEATURE_RELATED",
  "priority": "HIGH",
  "deadline": "2026-08-20T16:59:59Z",
  "status": "TO_DO",
  "syncStatus": "NOT_SYNCED",
  "jiraIssueKey": null,
  "createdAt": "2026-08-15T02:00:00Z",
  "updatedAt": "2026-08-15T02:00:00Z"
}
```

### 5.5 Change task status

```http
PATCH /api/v1/projects/10/tasks/501/status
```

```json
{
  "status": "IN_PROGRESS",
  "reason": "Bắt đầu triển khai"
}
```

`reason` bắt buộc khi chuyển sang `BLOCKED` hoặc `CANCELLED`. Mọi thay đổi trạng thái phải ghi activity log.

### 5.6 Assign task

```http
PATCH /api/v1/projects/10/tasks/501/assignee
```

```json
{
  "assigneeUserId": 4
}
```

Gửi `null` để bỏ giao task. Chỉ Team Leader của group sở hữu project được gọi endpoint này.

### 5.7 Delete rule

Chỉ xóa task ở `TO_DO`, chưa có Jira issue, commit/PR link hoặc activity quan trọng. Các trường hợp khác trả `409 RESOURCE_IN_USE` và chuyển `CANCELLED` thay vì xóa.

## 6. Contract SRS

### 6.1 Endpoint summary

| Method | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/projects/{projectId}/srs-versions` | Danh sách phiên bản SRS |
| `POST` | `/projects/{projectId}/srs-versions` | Sinh snapshot SRS mới |
| `GET` | `/projects/{projectId}/srs-versions/{srsVersionId}` | Metadata một phiên bản |
| `GET` | `/projects/{projectId}/srs-versions/{srsVersionId}/download` | Tải tài liệu đã sinh |

### 6.2 Generate SRS request

```http
POST /api/v1/projects/10/srs-versions
```

```json
{
  "version": "1.0.0",
  "format": "DOCX",
  "requirementStatuses": ["APPROVED", "SYNCED"]
}
```

Enum `format` Sprint 2:

```text
DOCX, PDF
```

Validation:

- `version`: bắt buộc, tối đa 50 ký tự, duy nhất trong project.
- Chỉ đưa requirement thuộc project vào tài liệu.
- Mặc định lấy `APPROVED` và `SYNCED`; không xuất `ARCHIVED`.
- Nội dung SRS là snapshot bất biến. Sửa requirement sau đó không thay file cũ.
- Sinh file thất bại không tạo bản ghi hoàn chỉnh; trả `422 SRS_GENERATION_FAILED`.

### 6.3 SRS response

```json
{
  "id": 21,
  "projectId": 10,
  "version": "1.0.0",
  "format": "DOCX",
  "status": "GENERATED",
  "generatedBy": {
    "id": 3,
    "username": "leader.test"
  },
  "generatedAt": "2026-08-15T04:00:00Z",
  "sourceSyncedAt": "2026-08-15T03:55:00Z",
  "downloadUrl": "/api/v1/projects/10/srs-versions/21/download",
  "checksum": "sha256:..."
}
```

Endpoint download trả đúng `Content-Type`, `Content-Disposition: attachment` và kiểm tra quyền trước khi đọc file.

## 7. Permission contract

Ký hiệu: `R` đọc, `C` tạo, `U` cập nhật, `D` xóa, `S` cập nhật trạng thái, `A` phân công, `G` sinh SRS.

| Actor | Requirement | Task | SRS | Phạm vi |
|---|---|---|---|---|
| Admin | Không | Không | Không | Quản trị tài khoản, nhóm và cấu hình; không sửa dữ liệu học thuật |
| Lecturer | `R` | `R` | `R`/download | Chỉ group được phân công |
| Team Leader | `R/C/U/D/S` | `R/C/U/D/S/A` | `R/G`/download | Chỉ project của group mình lãnh đạo |
| Team Member | Không | `R/S` | Không | Chỉ task được giao cho chính mình |

Quy tắc bắt buộc:

- Không tin `groupId`, role hoặc user ID do frontend khai báo; lấy identity từ access token và kiểm tra database.
- Tài nguyên ngoài phạm vi trả `404` khi cần tránh lộ sự tồn tại; hành động đúng tài nguyên nhưng sai quyền trả `403`.
- Lecturer chỉ đọc, không thay đổi requirement/task/SRS.
- Team Member chỉ dùng endpoint status và chỉ theo transition được phép.
- Team Leader không được gán task cho user ngoài group.

## 8. Ownership giữa các nhánh Sprint 2

| Phần | Owner triển khai | Không được tự thay đổi |
|---|---|---|
| Requirement API | Nhánh Requirement backend | Enum, endpoint và JSON field trong contract |
| Task API | Nhánh Task backend | Transition, assignment rule và permission |
| SRS | Nhánh SRS backend | Snapshot/version/download contract |
| Frontend | Nhánh UI Requirement/Task | Không tự đổi endpoint/enum; dùng adapter API chung |
| Security | Nhánh permission test | Không nới quyền để test đi qua |
| Integration Jira | Nhánh Jira sync | Không cho client ghi trực tiếp `syncStatus`/`jiraIssueKey` |

## 9. Acceptance checklist CNPM-52

- [x] Base path và response/error format được chốt.
- [x] Endpoint Requirement được chốt.
- [x] Endpoint Task và assignment/status được chốt.
- [x] Endpoint SRS generation/version/download được chốt.
- [x] Enum priority, status, issue type, classification và sync status được chốt.
- [x] Permission cho bốn actor và phạm vi group/project được chốt.
- [x] Validation, status transition và error code được mô tả.
- [x] Quy tắc thay đổi contract giữa các branch được mô tả.
- [x] Không chứa secret hoặc thông tin đăng nhập thật.

