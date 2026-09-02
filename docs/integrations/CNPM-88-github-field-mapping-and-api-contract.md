# CNPM-88 - GitHub field mapping và API contract

## 1. Mục tiêu và phạm vi

Tài liệu này là nguồn contract chung cho CNPM-89 đến CNPM-101. GitHub là nguồn dữ liệu gốc của Repository, Commit, Pull Request và GitHub User; hệ thống chỉ lưu snapshot cần thiết để liên kết Task Jira và lập báo cáo.

Phạm vi Sprint 4:

- Một Project local cấu hình một GitHub Repository chính.
- Đọc repository, commit, pull request, user và workflow run qua GitHub REST API.
- Đồng bộ theo cơ chế upsert, không tạo bản ghi trùng khi chạy lại.
- Liên kết hoạt động GitHub với Task bằng Jira Issue Key `CNPM-xx`.
- Token chỉ đi từ frontend đến backend lúc lưu cấu hình; không bao giờ được trả lại.

Ngoài phạm vi: sửa source trên GitHub, merge Pull Request, tạo GitHub Issue và hỗ trợ provider ngoài GitHub.

## 2. Enum thống nhất

| Enum | Giá trị |
| --- | --- |
| `IntegrationProvider` | `GITHUB` |
| `GitHubConnectionStatus` | `NOT_CONFIGURED`, `NOT_CHECKED`, `CONNECTED`, `CONNECTION_FAILED` |
| `GitHubSyncStatus` | `NOT_SYNCED`, `SYNCING`, `SYNCED`, `SYNC_FAILED` |
| `GitHubPullRequestState` | `OPEN`, `CLOSED`, `MERGED` |
| `GitHubWorkflowStatus` | `QUEUED`, `IN_PROGRESS`, `COMPLETED` |
| `TaskLinkSource` | `AUTO`, `MANUAL` |
| `TaskLinkMatchedFrom` | `BRANCH`, `COMMIT_MESSAGE`, `PR_TITLE`, `PR_BODY`, `MANUAL` |

Không dùng giá trị tự do cho status trong entity hoặc DTO. Giá trị chưa biết từ GitHub được ghi log an toàn và ánh xạ sang trạng thái gần nhất, không làm hỏng toàn bộ lượt sync.

## 3. Field mapping

### 3.1 Repository

| GitHub REST field | DTO/local field | Lưu DB | Quy tắc |
| --- | --- | --- | --- |
| `id` | `githubRepositoryId` | Có | ID bất biến; unique toàn hệ thống |
| `node_id` | `nodeId` | Tùy chọn | Dùng khi cần GraphQL sau này |
| `name` | `name` | Có | Tên repository |
| `full_name` | `fullName` | Có | `owner/repository`, unique |
| `owner.id` | `ownerGithubUserId` | Có | ID GitHub của owner |
| `owner.login` | `ownerLogin` | Có | Không dùng làm khóa bất biến |
| `private` | `privateRepository` | Có | Không suy ra quyền truy cập từ cờ này |
| `default_branch` | `defaultBranch` | Có | Ví dụ `main` |
| `html_url` | `htmlUrl` | Có | Chỉ chấp nhận `https://github.com/...` |
| `archived` | `archived` | Có | Repository archived vẫn giữ lịch sử |
| `updated_at` | `remoteUpdatedAt` | Có | ISO-8601 UTC |
| thời điểm sync | `lastSyncedAt` | Có | Do backend tạo |

Unique bắt buộc: `github_repository_id`, `full_name`, và một cấu hình `project_id + provider`.

### 3.2 Commit

| GitHub REST field | DTO/local field | Quy tắc |
| --- | --- | --- |
| `sha` | `sha` | Unique trong repository; lưu đủ SHA |
| `commit.message` | `message` | Giữ nguyên để phát hiện Issue Key |
| `commit.author.name` | `gitAuthorName` | Có thể khác GitHub login |
| `commit.author.email` | `gitAuthorEmail` | Không hiển thị công khai nếu không cần |
| `commit.author.date` | `committedAt` | ISO-8601 UTC |
| `author.id` | `authorGithubUserId` | Nullable với commit không gắn tài khoản |
| `author.login` | `authorLogin` | Nullable |
| `html_url` | `htmlUrl` | URL commit GitHub |
| `stats.additions` | `additions` | Mặc định `0` nếu endpoint chi tiết không trả |
| `stats.deletions` | `deletions` | Mặc định `0` |
| `files.length` | `filesChanged` | Nullable khi chưa đọc chi tiết |
| `parents[].sha` | `parentShas` | Dùng nhận biết merge commit |

Upsert key: `repository_id + sha`. Force-push không được xóa lịch sử ngay; đánh dấu commit không còn trên nhánh mặc định ở lượt đối soát sau.

### 3.3 Pull Request

| GitHub REST field | DTO/local field | Quy tắc |
| --- | --- | --- |
| `id` | `githubPullRequestId` | ID bất biến từ GitHub |
| `number` | `number` | Unique trong repository |
| `title` | `title` | Nguồn phát hiện Issue Key |
| `body` | `body` | Nullable; sanitize trước khi hiển thị |
| `user.id`, `user.login` | `authorGithubUserId`, `authorLogin` | Tác giả PR |
| `head.ref` | `headRef` | Nguồn ưu tiên cao nhất để liên kết Task |
| `head.sha` | `headSha` | SHA đầu PR |
| `base.ref` | `baseRef` | Nhánh đích |
| `state`, `merged_at` | `state`, `mergedAt` | Có `mergedAt` thì state local là `MERGED` |
| `draft` | `draft` | Boolean |
| `merge_commit_sha` | `mergeCommitSha` | Nullable |
| `commits` | `commitCount` | Số commit trong PR |
| `additions`, `deletions`, `changed_files` | cùng tên | Dùng thống kê đóng góp |
| `html_url` | `htmlUrl` | URL PR GitHub |
| `created_at`, `updated_at`, `closed_at` | cùng tên | ISO-8601 UTC |

Upsert key: `repository_id + number`; `github_pull_request_id` cũng phải unique.

### 3.4 GitHub User và tài khoản ngoài

| GitHub REST field | DTO/local field | Quy tắc |
| --- | --- | --- |
| `id` | `externalAccountId` | Chuyển sang chuỗi; khóa bất biến |
| `login` | `username` | Có thể đổi, không dùng làm khóa chính |
| `name` | `displayName` | Nullable |
| `avatar_url` | `avatarUrl` | Chỉ URL HTTPS |
| `html_url` | `profileUrl` | Chỉ `https://github.com/...` |
| `email` | `email` | Nullable, không dùng để tự động gán nếu không xác minh |

Liên kết local dùng `user_external_accounts(user_id, provider=GITHUB, external_account_id, username)`. Một GitHub ID chỉ liên kết với một user local; thay đổi liên kết cần lưu audit log.

## 4. Quy tắc liên kết Task - branch - commit - Pull Request

Issue Key chuẩn: `[A-Z][A-Z0-9_]{1,29}-[1-9][0-9]*`.

Thứ tự phát hiện:

1. `headRef` của Pull Request.
2. Commit message.
3. Tiêu đề Pull Request.
4. Nội dung Pull Request.

Ví dụ hợp lệ: `feature/CNPM-88-github-contract`, `CNPM-88 Chốt GitHub contract`.

Quy tắc:

- Chỉ liên kết khi Issue Key tồn tại trong đúng Jira project của Project local.
- Một commit/PR có thể liên kết nhiều Task nếu chứa nhiều Issue Key hợp lệ.
- Không tìm thấy key thì giữ hoạt động ở danh sách `unlinked`; không tự đoán Task.
- Key không tồn tại hoặc khác project được ghi `linkWarning`, không làm hỏng lượt sync.
- Auto link dùng `linkSource=AUTO` và ghi `matchedFrom`; chỉnh tay dùng `MANUAL`, `linkedByUserId`, `reason`.
- Unique: `task_id + commit_id` và `task_id + pull_request_id`.

## 5. API nội bộ

Base path: `/api/v1/projects/{projectId}/integrations/github`.

| Method và path | Quyền | Mục đích |
| --- | --- | --- |
| `GET /config` | Admin hoặc Leader đúng project | Đọc cấu hình đã che token |
| `PUT /config` | Admin | Tạo/cập nhật repository và token |
| `POST /test-connection` | Admin | Kiểm tra user, quyền và repository |
| `POST /sync` | Admin hoặc Leader đúng project | Đồng bộ repository, commit, PR và user |
| `GET /repositories` | Lecturer, Leader, Member thuộc project | Danh sách repository snapshot |
| `GET /repositories/{repositoryId}/commits` | Thành viên project | Danh sách commit |
| `GET /repositories/{repositoryId}/pull-requests` | Thành viên project | Danh sách PR |
| `GET /activities` | Thành viên project | Hoạt động hợp nhất có filter |
| `GET /tasks/{taskId}/activities` | Thành viên project | Commit/PR đã liên kết với Task |
| `PUT /members/{userId}/account-link` | Admin hoặc Leader đúng project | Liên kết user local với GitHub ID |

### 5.1 Cấu hình

`PUT /config`:

```json
{
  "repositoryOwner": "minhthu31",
  "repositoryName": "java",
  "accessToken": "<write-only>",
  "apiVersion": "2026-03-10"
}
```

Response không có `accessToken`:

```json
{
  "data": {
    "projectId": 1,
    "repositoryFullName": "minhthu31/java",
    "configured": true,
    "status": "NOT_CHECKED",
    "lastTestedAt": null,
    "lastTestSucceeded": null
  },
  "timestamp": "2026-09-02T06:00:00Z"
}
```

Khi cập nhật mà không muốn đổi token, frontend gửi `accessToken=null`; backend giữ secret cũ. Chuỗi rỗng là validation error, không được dùng để xóa token ngoài ý muốn.

### 5.2 Test connection

`POST /test-connection` gọi GitHub `GET /user` và `GET /repos/{owner}/{repo}`. Thành công trả login, GitHub user ID, repository ID, permission và rate-limit hiện tại.

### 5.3 Sync

`POST /sync` nhận `Idempotency-Key` bắt buộc. Response:

```json
{
  "data": {
    "projectId": 1,
    "repositoryId": 20,
    "commitsSynced": 42,
    "pullRequestsSynced": 8,
    "usersSynced": 5,
    "linksCreated": 31,
    "unlinkedActivities": 4,
    "errors": 0,
    "lastSyncedAt": "2026-09-02T06:10:00Z",
    "correlationId": "9cb1e6f0-0d49-4af8-aeb7-111111111111"
  },
  "timestamp": "2026-09-02T06:10:00Z"
}
```

## 6. Pagination và filter

Mọi endpoint danh sách dùng query chung:

- `page`: bắt đầu từ `0`, mặc định `0`.
- `size`: mặc định `20`, tối đa `100`.
- `sort`: mặc định `remoteUpdatedAt,desc`; phải có khóa phụ bất biến (`id,desc`) để không nhảy bản ghi.
- `from`, `to`: ISO-8601 UTC, `from <= to`.
- `actorUserId`, `type`, `issueKey`, `state`: filter tùy endpoint.

Response dùng đúng `ApiResponse<PageResponse<T>>` hiện có:

```json
{
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "timestamp": "2026-09-02T06:20:00Z"
}
```

GitHub client đọc `Link` header và dùng `per_page=100` nội bộ. API frontend không được lộ URL pagination hoặc token của GitHub.

## 7. Error model

Lỗi dùng `ApiError(code, message, correlationId, fieldErrors, timestamp, retryable, retryAfterSeconds)`.

| HTTP | Code | Retry | Trường hợp |
| --- | --- | --- | --- |
| `400` | `GITHUB_CONFIG_INVALID` | Không | owner/name/version không hợp lệ |
| `401` | `GITHUB_AUTHENTICATION_FAILED` | Không | token thiếu, hết hạn hoặc sai |
| `403` | `GITHUB_AUTHORIZATION_FAILED` | Không | token không có quyền đọc repository |
| `404` | `GITHUB_REPOSITORY_NOT_FOUND` | Không | repository không tồn tại hoặc không nhìn thấy |
| `409` | `GITHUB_SYNC_ALREADY_RUNNING` | Có | project đang có lượt sync chạy |
| `409` | `IDEMPOTENCY_KEY_REUSED` | Không | cùng key nhưng payload/fingerprint khác |
| `429` | `GITHUB_RATE_LIMITED` | Có | primary hoặc secondary rate limit |
| `502` | `GITHUB_PROVIDER_UNAVAILABLE` | Có | timeout, DNS, GitHub 5xx |
| `503` | `GITHUB_SYNC_TEMPORARILY_UNAVAILABLE` | Có | backend tạm không nhận sync |

Với rate limit, ưu tiên `Retry-After`; nếu không có thì tính từ `X-RateLimit-Reset`. Không retry 401, 403, 404 hoặc validation error. Không trả nguyên response body của GitHub cho frontend.

## 8. Bảo mật và logging

- Mã hóa PAT trước khi lưu bằng cùng cơ chế `IntegrationSecretService`.
- Không đưa token vào DTO response, URL, exception, SyncLog hoặc log HTTP.
- Không cho frontend đọc lại token đã lưu; form token luôn rỗng khi mở lại.
- Chỉ cho phép GitHub API origin chính thức; không nhận base URL tùy ý trong MVP.
- Dùng header `Accept: application/vnd.github+json`, `Authorization: Bearer ...`, `X-GitHub-Api-Version: 2026-03-10` và User-Agent của ứng dụng.
- Timeout bắt buộc; log provider status, endpoint template, correlation ID và rate-limit metadata đã loại secret.
- Khuyến nghị fine-grained PAT chỉ có quyền Metadata read, Contents read, Pull requests read và Actions read khi dùng workflow run.

## 9. Idempotency và đồng bộ

- Repository upsert theo GitHub repository ID.
- Commit upsert theo repository + SHA.
- Pull Request upsert theo repository + number.
- User upsert theo GitHub user ID.
- Mỗi sync tạo một SyncLog cha và dùng cùng correlation ID cho log con.
- Partial failure không xóa snapshot đã đồng bộ thành công; response tăng `errors`.
- Cùng `Idempotency-Key` và fingerprint trả kết quả cũ; cùng key khác fingerprint trả conflict.

## 10. Checklist review backend/frontend

- [x] Mapping Repository, Commit, Pull Request và GitHub User đã chốt.
- [x] Endpoint config, test connection, sync và đọc hoạt động đã chốt.
- [x] Pagination dùng chung `PageResponse` của hệ thống.
- [x] Error code cho authentication, authorization, repository not found, rate limit và provider unavailable đã chốt.
- [x] Quy tắc liên kết `CNPM-xx` từ branch, commit và Pull Request đã chốt.
- [x] Token là write-only và không xuất hiện trong response.
- [x] Contract dùng base path và response envelope thống nhất với frontend hiện tại.

Thay đổi contract sau khi CNPM-88 được merge phải cập nhật tài liệu này và OpenAPI trong cùng Pull Request, đồng thời được ít nhất một người backend và một người frontend review.
