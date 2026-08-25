# CNPM-74 - Jira field mapping và API contract Sprint 3

## 1. Trạng thái contract

| Thuộc tính | Giá trị |
|---|---|
| Contract version | `1.0.0` |
| Ngày chốt kỹ thuật | 23/08/2026 |
| Base path nội bộ | `/api/v1` |
| Jira API đích | Jira Cloud REST API v3 và Jira Software Cloud REST API |
| Nguồn chuẩn máy đọc | [`docs/api/jira-integration-v1.openapi.yaml`](../api/jira-integration-v1.openapi.yaml) |
| Nguồn chuẩn backend | Package `integration.jira.contract` |
| Nguồn chuẩn frontend | `frontend/src/JiraIntegrationService.js` |

Tài liệu này bổ sung contract Sprint 2, không đổi endpoint Task đã triển khai bởi
CNPM-60/CNPM-61. Thay đổi phá vỡ tương thích phải tăng major version và được
backend, frontend cùng review.

## 2. Quyết định tích hợp Jira Cloud

- MVP dùng `API_TOKEN`: `siteUrl`, email Atlassian và API token. Password Jira
  không được hỗ trợ. Token phải được mã hóa trước khi lưu và không bao giờ xuất
  hiện trong response, log hoặc exception.
- `siteUrl` chỉ nhận HTTPS origin, không nhận path, query, fragment, user-info
  hoặc port tùy ý. Implementation phải chặn loopback, private/link-local address
  sau mỗi lần DNS resolve để tránh SSRF và DNS rebinding.
- `PUT config` chỉ lưu cấu hình. `POST test-connection` mới gọi Jira để kiểm tra
  credential và project.
- Test connection gọi `GET /rest/api/3/myself`, sau đó xác minh project key và
  create metadata. Cấu hình chỉ được đánh dấu hợp lệ khi tài khoản nhìn thấy
  project và có metadata cần để tạo Issue.
- Description của Jira Cloud REST API v3 dùng Atlassian Document Format (ADF),
  không gửi HTML và không gửi chuỗi plain text trực tiếp vào `fields.description`.
- ID của issue type và priority được resolve theo project từ create metadata.
  Không hard-code ID như `10001` vì ID thay đổi giữa các Jira site.
- Assignee dùng `accountId`, không dùng username hoặc email trong payload Jira.
- MVP dùng timezone cố định `Asia/Ho_Chi_Minh` khi đổi `deadline` từ `Instant`
  sang Jira `duedate`; mở rộng nhiều timezone phải tăng contract version.
- Sprint được gắn sau khi Issue tồn tại bằng Jira Software API; không hard-code
  Sprint custom-field ID.
- Feature/Epic dùng `fields.parent.key` với Jira hierarchy hiện hành. Không dùng
  Epic Link custom field đã cũ và phụ thuộc từng site.

## 3. Bảng ánh xạ Task local sang Jira Issue

| Task local | Jira outbound | Jira inbound | Quy tắc và lỗi |
|---|---|---|---|
| `id` | label `cnpm-local-task-{id}` | liên kết qua `jira_issues.task_id` | Label xác định giúp reconcile sau timeout; một Task chỉ có một Jira Issue |
| `projectId` | `fields.project.id` | project ID/key | Resolve từ cấu hình project; thiếu mapping trả `JIRA_PROJECT_NOT_CONFIGURED` |
| `title` | `fields.summary` | `fields.summary` | Bắt buộc, tối đa 255 ký tự |
| `description` | phần **Description** trong ADF | plain text chuẩn hóa từ ADF | Không render HTML từ Jira |
| `acceptanceCriteria` | phần **Acceptance Criteria** trong cùng ADF | plain text trong description | Luôn giữ nội dung này khi tạo/cập nhật |
| `issueType` | `fields.issuetype.id` | issue type ID/name | Resolve theo bảng 3.1; không tìm thấy trả `ISSUE_TYPE_MAPPING_MISSING` |
| `priority` | `fields.priority.id` | priority ID/name | Resolve theo bảng 3.2; không hard-code ID |
| `assigneeUserId` | `fields.assignee.accountId` | account ID/display name | Có assignee local nhưng chưa liên kết Jira trả `ASSIGNEE_MAPPING_MISSING`; không tự đoán theo email |
| `deadline` | `fields.duedate` dạng `YYYY-MM-DD` | date | Chuyển `Instant` theo `Asia/Ho_Chi_Minh`; Jira chỉ giữ ngày, local vẫn giữ thời điểm đầy đủ |
| `sprintId` | `POST /rest/agile/1.0/sprint/{jiraSprintId}/issue` | sprint ID/name | Thực hiện sau create/update Issue; thiếu `jiraSprintId` trả `SPRINT_MAPPING_MISSING` |
| `featureId` | `fields.parent.key` | parent Issue key | Chỉ áp dụng cho Issue không phải Epic; thiếu `jiraEpicKey` trả `EPIC_MAPPING_MISSING` |
| `status` | chưa ghi trong CNPM-74 | Jira status ID/name/category | Workflow từng site khác nhau; đồng bộ transition là task riêng, không tự map bằng tên |
| `syncStatus` | không gửi Jira | không lấy từ Jira | Là trạng thái kỹ thuật nội bộ, theo state machine ở mục 5 |
| `jiraIssueKey` | nhận từ response create | `issue.key` | Client local chỉ đọc, chỉ integration service được ghi |

### 3.1 Issue type

| Local | Jira type cần resolve | Ghi chú |
|---|---|---|
| `EPIC` | `Epic` | Không gắn `parent` |
| `STORY` | `Story` | Có thể gắn Feature/Epic làm parent |
| `TASK` | `Task` | Có thể gắn Feature/Epic làm parent |
| `BUG` | `Bug` | Có thể gắn Feature/Epic làm parent nếu Jira project cho phép |
| `SUBTASK` | Sub-task type của project | Chưa cho sync cho đến khi local có `parentTaskId` |

Backend phải dùng issue type ID do create metadata trả về. Tên trong bảng chỉ là
khóa logic mặc định và có thể được cấu hình nếu Jira site dùng tên khác.

### 3.2 Priority

| Local | Jira priority mặc định |
|---|---|
| `HIGHEST` | `Highest` |
| `HIGH` | `High` |
| `MEDIUM` | `Medium` |
| `LOW` | `Low` |
| `LOWEST` | `Lowest` |

Backend resolve priority ID theo tên trong metadata. Không có priority tương ứng
thì trả `PRIORITY_MAPPING_MISSING`; không âm thầm hạ về Medium.

### 3.3 Payload Jira chuẩn hóa

Ví dụ payload do adapter Jira tạo ra, không phải payload frontend gửi trực tiếp:

```json
{
  "fields": {
    "project": { "id": "10000" },
    "summary": "Chốt Jira field mapping và API contract Sprint 3",
    "description": {
      "version": 1,
      "type": "doc",
      "content": [
        {
          "type": "paragraph",
          "content": [{ "type": "text", "text": "Description: ..." }]
        },
        {
          "type": "paragraph",
          "content": [{ "type": "text", "text": "Acceptance Criteria: ..." }]
        }
      ]
    },
    "issuetype": { "id": "10001" },
    "priority": { "id": "3" },
    "assignee": { "accountId": "5b10ac8d82e05b22cc7d4ef5" },
    "duedate": "2026-08-23",
    "parent": { "key": "CNPM-3" },
    "labels": ["cnpm-local-task-74"]
  }
}
```

Nếu Task thuộc Sprint đã map, sau khi nhận Issue key backend gọi:

```http
POST /rest/agile/1.0/sprint/{jiraSprintId}/issue
Content-Type: application/json

{"issues":["CNPM-74"]}
```

## 4. API nội bộ Sprint 3

| Method | Endpoint | Service interface | Quyền |
|---|---|---|---|
| `GET` | `/projects/{projectId}/integrations/jira/config` | `getConnection` | Admin; Team Leader chỉ đọc bản đã che secret |
| `PUT` | `/projects/{projectId}/integrations/jira/config` | `configureConnection` | Admin |
| `POST` | `/projects/{projectId}/integrations/jira/test-connection` | `testConnection` | Admin |
| `POST` | `/projects/{projectId}/integrations/jira/tasks/{taskId}/sync` | `syncTask` | Team Leader |
| `POST` | `/projects/{projectId}/integrations/jira/tasks/{taskId}/retry` | `retryTaskSync` | Team Leader |
| `GET` | `/projects/{projectId}/integrations/jira/issues/{jiraIssueKey}` | `getIssue` | Lecturer, Team Leader; Member chỉ task được giao |

Request/response schema, status code và ví dụ được chốt trong OpenAPI. Tất cả
response thành công dùng envelope hiện có của dự án:

```json
{
  "data": {},
  "timestamp": "2026-08-23T16:00:00Z"
}
```

API token chỉ có trong request `PUT config` dưới dạng write-only. `GET config`
chỉ trả `configured=true/false`.

## 5. State machine đồng bộ

```text
NOT_SYNCED --sync--> SYNCING --success--> SYNCED
                              --error----> SYNC_FAILED
SYNC_FAILED --retry----------> SYNCING
SYNCED --sync after change---> SYNCING
```

Bộ enum duy nhất:

```text
NOT_SYNCED, SYNCING, SYNCED, SYNC_FAILED
```

Quy tắc:

- Client không được gửi `syncStatus` trong Task create/update.
- Integration service là thành phần duy nhất thay đổi `syncStatus`.
- Chuyển sang `SYNCING` và tạo `sync_logs` phải nằm trong cùng transaction local.
- Chỉ ghi `SYNCED` sau khi đã lưu `jira_issue_id`, `jira_issue_key`, URL và
  snapshot/hash.
- `SYNC_FAILED` phải có `errorCode`, message đã loại secret và cờ `retryable`.
- `syncStatus` không phải Jira workflow status. Hai khái niệm không được dùng lẫn.

Migration V5 chuyển dữ liệu cũ `PENDING -> SYNCING` và
`FAILED -> SYNC_FAILED`.

## 6. Idempotency và retry

### 6.1 Idempotency-Key

- Header `Idempotency-Key` bắt buộc ở cả `sync` và `retry`, dài 8-100 ký tự.
- Phạm vi duy nhất là `(projectId, provider, entityType, entityId, direction,
  idempotencyKey)` trong `sync_logs`.
- Backend lưu `requestFingerprint` SHA-256 từ Task snapshot, mapping version và
  loại operation trước khi gọi Jira.
- Cùng key và cùng fingerprint trả lại cùng kết quả đã lưu, không gọi Jira lần hai.
- Cùng key nhưng khác fingerprint trả `409 IDEMPOTENCY_KEY_REUSED`.
- Task đang `SYNCING` bởi key khác trả `409 SYNC_ALREADY_RUNNING`.
- Retry phải có key mới; không tái sử dụng key của lần thất bại.

### 6.2 Chống tạo Jira Issue trùng

1. Kiểm tra `jira_issues.task_id` trước khi create; đã có mapping thì chuyển sang update.
2. Gửi label xác định `cnpm-local-task-{taskId}` khi create.
3. Sau timeout không rõ kết quả, tìm Issue trong đúng Jira project bằng label trên
   trước khi retry create.
4. Tìm thấy đúng một Issue thì nhận lại mapping; không tạo mới.
5. Tìm thấy nhiều Issue thì dừng với `DUPLICATE_REMOTE_ISSUE` để người dùng xử lý.

### 6.3 Phân loại lỗi

| Nhóm lỗi | Ví dụ | Retry tự động |
|---|---|---|
| Validation/mapping | 400, field không có trên create screen | Không |
| Jira auth/permission | 401, 403 | Không; sửa cấu hình/quyền trước |
| Không tìm thấy remote | 404 project/sprint/epic | Không; sửa mapping trước |
| Rate limit | 429 | Có, theo `Retry-After` |
| Timeout/network | timeout, DNS, connection reset | Có, sau bước reconcile |
| Jira server | 5xx | Có, exponential backoff có giới hạn |

MVP tối đa 3 lần gọi provider cho một operation. Mọi attempt phải ghi
`sync_logs`; không log Basic Authorization, email-token pair, raw token hoặc toàn
bộ response có dữ liệu nhạy cảm.

## 7. Error code nội bộ

| HTTP | Code | Ý nghĩa |
|---|---|---|
| `400` | `VALIDATION_FAILED` | URL, key, header hoặc enum không hợp lệ |
| `403` | `ACCESS_DENIED` | Actor không có quyền project/operation |
| `404` | `RESOURCE_NOT_FOUND` | Project, Task, config hoặc Issue không tồn tại trong phạm vi |
| `409` | `IDEMPOTENCY_KEY_REUSED` | Cùng key nhưng request khác |
| `409` | `SYNC_ALREADY_RUNNING` | Task đang có operation đồng bộ khác |
| `422` | `JIRA_PROJECT_NOT_CONFIGURED` | Project chưa có cấu hình Jira hợp lệ |
| `422` | `ISSUE_TYPE_MAPPING_MISSING` | Không resolve được issue type ID |
| `422` | `PRIORITY_MAPPING_MISSING` | Không resolve được priority ID |
| `422` | `ASSIGNEE_MAPPING_MISSING` | User local chưa có Jira accountId |
| `422` | `SPRINT_MAPPING_MISSING` | Sprint local chưa có Jira sprint ID |
| `422` | `EPIC_MAPPING_MISSING` | Feature local chưa có Jira epic key |
| `502` | `JIRA_UNAVAILABLE` | Jira timeout, rate limit hết retry hoặc 5xx |

Error trả theo `ApiError` hiện tại và luôn có `correlationId`. Message không chứa
API token hoặc Authorization header.

## 8. Đồng nhất CNPM-60 và CNPM-61

Task local giữ nguyên interface đã merge:

| Controller CNPM-61 | TaskService CNPM-60 |
|---|---|
| `GET /projects/{projectId}/tasks` | `getTasks(projectId, filter)` |
| `POST /projects/{projectId}/tasks` | `createTask(projectId, request, idempotencyKey)` |
| `GET /projects/{projectId}/tasks/{taskId}` | `getTaskById(projectId, taskId)` |
| `PUT /projects/{projectId}/tasks/{taskId}` | `updateTask(projectId, taskId, request)` |
| `PATCH .../{taskId}/status` | `updateTaskStatus(projectId, taskId, request)` |
| `PATCH .../{taskId}/assignee` | `updateTaskAssignee(projectId, taskId, request)` |
| `DELETE .../{taskId}` | `deleteTask(projectId, taskId)` |

Jira integration không thêm method vào `TaskService` và không để
`TaskController` gọi Jira trực tiếp. Boundary mới là `JiraIntegrationService`;
implementation sau này chỉ đọc Task qua repository/service được cấp quyền và ghi
mapping qua integration repository.

Frontend chỉ gọi API nội bộ bằng `JiraIntegrationService.js`, không gọi
`*.atlassian.net`, không nhận API token sau khi lưu và không tự sửa
`jiraIssueKey/syncStatus`.

## 9. Review và Definition of Done

### Kiểm tra tự động trong branch CNPM-74

- [x] Backend enum chỉ còn đúng bốn trạng thái Sprint 3.
- [x] Migration chuẩn hóa enum cũ và thêm khóa idempotency cho `sync_logs`.
- [x] Java contract compile cùng `TaskService`/`TaskController` hiện tại.
- [x] Frontend adapter có test cho config, connection, sync, retry và read Issue.
- [x] Contract test khóa endpoint, field mapping và enum trong OpenAPI.
- [x] Backend **117/117 test đạt**; Flyway chạy thành công V1 đến V5.
- [x] Frontend **62/62 test đạt** trên 7 suite; production build thành công.
- [x] OpenAPI YAML parse thành công với 5 path và 6 operation.
- [x] Không chứa credential Jira thật.

### Sign-off bắt buộc trước khi chuyển Done

| Vai trò review | Người review | Kết quả |
|---|---|---|
| Backend | Chưa ký | Pending |
| Frontend | Chưa ký | Pending |

Hai reviewer cần xác nhận trên PR rằng frontend adapter, OpenAPI và Java contract
không tự đổi endpoint, field hoặc enum. Đây là bước con người nên không được đánh
dấu hoàn thành chỉ bằng automated test.

## 10. Nguồn Jira chính thức

- [Jira Cloud REST API v3 - Issues](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/)
- [Jira Cloud REST API v3 - Current user](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-myself/#api-rest-api-3-myself-get)
- [Atlassian Document Format](https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/)
- [Jira Software Cloud REST API - Sprint](https://developer.atlassian.com/cloud/jira/software/rest/api-group-sprint/)
- [Basic authentication for Jira Cloud REST APIs](https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/)
