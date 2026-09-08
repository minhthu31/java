# CNPM-102 - Contract báo cáo và chỉ số đánh giá

## 1. Phạm vi

Tài liệu này là nguồn contract chung cho các task báo cáo Sprint 5. Báo cáo tổng hợp
trạng thái Task local và snapshot hoạt động GitHub đã đồng bộ; request báo cáo không
gọi trực tiếp Jira hoặc GitHub.

Endpoint chuẩn:

```http
GET /api/v1/projects/{projectId}/reports/summary
```

Response dùng `ApiResponse<ReportSummaryResponse>`. Thời gian là ISO-8601 UTC, tên
field JSON dùng `camelCase`, và mọi phép đếm đều trả số `0` thay vì `null`.

## 2. Bộ lọc

| Filter | Bắt buộc | Quy tắc |
| --- | --- | --- |
| `projectId` | Có | Path parameter; Project phải tồn tại và người gọi phải có quyền |
| `sprintId` | Không | Số dương; Sprint phải thuộc Project |
| `memberId` | Không | Số dương; user phải là thành viên active của group sở hữu Project |
| `from` | Không | Inclusive, ISO-8601 UTC |
| `to` | Không | Exclusive, ISO-8601 UTC và phải lớn hơn `from` |

Khoảng thời gian chuẩn là `[from, to)`. Bỏ cả hai mốc nghĩa là không giới hạn thời
gian. Chỉ truyền một mốc tạo khoảng mở về phía còn lại. Backend chụp một `asOf` duy
nhất khi bắt đầu request và dùng mốc đó cho toàn bộ phép tính.

Ý nghĩa filter theo nguồn:

- Task: `projectId`, `sprintId`, `assigneeUserId`; khoảng thời gian áp dụng trên
  `tasks.created_at`.
- Commit: thời gian áp dụng trên `github_commits.committed_at`.
- Pull Request: thời gian áp dụng trên thời điểm tạo PR của GitHub
  (`github_pull_requests.remote_created_at`). Schema hiện tại chưa có field này;
  task hiện thực báo cáo phải thêm bằng migration mới, không sửa migration cũ.
- Khi có `sprintId`, commit/PR chỉ được tính nếu có ít nhất một Task link thuộc Sprint;
  một hoạt động liên kết nhiều Task vẫn chỉ được đếm một lần.
- Khi có `memberId`, Task lọc theo assignee; commit/PR lọc qua tài khoản GitHub đã liên
  kết với đúng local user.

## 3. Công thức chỉ số Task

Tập `T` là các Task thỏa bộ lọc.

| Chỉ số | Công thức |
| --- | --- |
| `totalTasks` | `COUNT(DISTINCT T.id)` |
| `completedTasks` | `COUNT(DISTINCT T.id WHERE status = DONE)` |
| `overdueTasks` | `COUNT(DISTINCT T.id WHERE deadline < asOf AND status NOT IN (DONE, CANCELLED))` |
| `tasksByStatus` | Count theo đủ sáu trạng thái `TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`, `BLOCKED`, `CANCELLED` |

Task không có deadline không phải Task trễ. `completedTasks` phản ánh trạng thái tại
`asOf`, không suy đoán thời điểm hoàn thành vì schema hiện chưa lưu `completedAt`.
Tổng các giá trị trong `tasksByStatus` phải bằng `totalTasks`.

## 4. Công thức đóng góp thành viên

Mỗi thành viên trả một dòng `MemberContributionResponse`:

| Chỉ số | Công thức |
| --- | --- |
| `commits` | Số `DISTINCT github_commits.id` do GitHub account đã liên kết thực hiện |
| `pullRequests` | Số `DISTINCT github_pull_requests.id` do GitHub account đã liên kết tạo |
| `linkedTasks` | Số `DISTINCT task_id` trong hợp của Task-Commit link và Task-PR link của thành viên |

Không cộng một Task hai lần khi Task đó đồng thời liên kết commit và PR. Không dùng
email commit để tự động nhận diện thành viên. Chỉ mapping
`user_external_accounts(provider=GITHUB)` được xác minh mới có giá trị quy thuộc.

Các chỉ số trên là số liệu hoạt động, không tự động suy ra điểm chất lượng cá nhân.
Nếu Sprint sau bổ sung điểm đánh giá, công thức và trọng số phải được version hóa
riêng, không thay đổi ý nghĩa ba chỉ số này.

## 5. DTO response

```json
{
  "data": {
    "projectId": 1,
    "sprintId": 5,
    "memberId": null,
    "from": "2026-09-08T00:00:00Z",
    "to": "2026-09-15T00:00:00Z",
    "asOf": "2026-09-09T02:00:00Z",
    "taskMetrics": {
      "totalTasks": 15,
      "completedTasks": 4,
      "overdueTasks": 2,
      "tasksByStatus": {
        "TO_DO": 5,
        "IN_PROGRESS": 3,
        "IN_REVIEW": 2,
        "DONE": 4,
        "BLOCKED": 1,
        "CANCELLED": 0
      }
    },
    "memberContributions": [
      {
        "memberId": 7,
        "username": "member.test",
        "fullName": "Test Team Member",
        "commits": 12,
        "pullRequests": 3,
        "linkedTasks": 5
      }
    ],
    "dataStatus": "COMPLETE",
    "sources": [
      { "source": "LOCAL_TASK", "status": "CURRENT", "lastSyncedAt": null },
      { "source": "JIRA", "status": "CURRENT", "lastSyncedAt": "2026-09-09T01:45:00Z" },
      { "source": "GITHUB", "status": "CURRENT", "lastSyncedAt": "2026-09-09T01:50:00Z" }
    ],
    "warnings": []
  },
  "timestamp": "2026-09-09T02:00:00Z"
}
```

`tasksByStatus` luôn chứa đủ sáu key. Khi `memberId` được truyền,
`memberContributions` có tối đa một phần tử. Khi không truyền, danh sách chứa mọi
thành viên active trong phạm vi Project, kể cả người có số liệu bằng `0`.

## 6. Nguồn dữ liệu và độ mới

| Source | Nguồn chuẩn cho báo cáo | `lastSyncedAt` |
| --- | --- | --- |
| `LOCAL_TASK` | Bảng `tasks` hiện tại | `null`; đọc trực tiếp tại `asOf` |
| `JIRA` | Snapshot Jira và `sync_logs` local | Lần sync Jira thành công gần nhất của Project |
| `GITHUB` | Repository, commit, PR và Task link local | Lần sync GitHub thành công gần nhất của Project |

`ReportSourceStatus`:

- `CURRENT`: nguồn local hoặc lần sync thành công bao phủ đến `to`; nếu không có
  `to`, lần sync nằm trong ngưỡng `reporting.freshness-threshold` (mặc định 24 giờ).
- `STALE`: đã từng sync thành công nhưng chưa bao phủ kỳ báo cáo hoặc quá ngưỡng.
- `NOT_SYNCED`: chưa có lần sync thành công.
- `SYNC_FAILED`: lần sync mới nhất thất bại sau lần thành công gần nhất.

`ReportDataStatus`:

- `COMPLETE`: các nguồn cần thiết đều `CURRENT`.
- `PARTIAL`: ít nhất một nguồn là `STALE` hoặc `SYNC_FAILED`, nhưng vẫn có dữ liệu
  local để trả.
- `NOT_SYNCED`: chưa từng có snapshot Jira/GitHub cần cho báo cáo.

Không có dữ liệu hợp lệ trả HTTP `200` với count bằng `0`. Chưa đồng bộ không được
giả dạng “không có hoạt động”: response phải có `dataStatus`, `sources` và warning
như `GITHUB_NOT_SYNCED`, `JIRA_SYNC_FAILED` hoặc `GITHUB_ACCOUNT_NOT_LINKED`.
Request báo cáo không tự khởi chạy sync và partial data không làm mất dữ liệu thành
công đã có.

## 7. Phân quyền

- `ADMIN`: xem mọi Project.
- `LECTURER`: xem Project thuộc group được phân công.
- `TEAM_LEADER`: xem Project của group mình quản lý.
- `TEAM_MEMBER`: chỉ xem Project mình tham gia; backend ép `memberId` về chính user
  hiện tại và không trả số liệu cá nhân của thành viên khác.

Không tin `memberId` do frontend gửi để quyết định data scope. Mọi kiểm tra Project,
Sprint và member phải thực hiện ở backend.

## 8. Error contract

Lỗi dùng `ApiError(code, message, correlationId, fieldErrors, timestamp)`.

| HTTP | Code | Trường hợp |
| --- | --- | --- |
| `400` | `REPORT_FILTER_INVALID` | ID không dương, `from >= to`, sai định dạng thời gian |
| `401` | `UNAUTHORIZED` | Thiếu hoặc sai access token |
| `403` | `REPORT_ACCESS_DENIED` | Vượt phạm vi Project hoặc xem member khác |
| `404` | `PROJECT_NOT_FOUND` | Project không tồn tại |
| `404` | `SPRINT_NOT_FOUND` | Sprint không thuộc Project |
| `404` | `MEMBER_NOT_FOUND` | Member không active trong Project |

Không trả stack trace, token Jira/GitHub, email commit hoặc encrypted secret trong
response và log. Mọi response tiếp tục trả header `X-Correlation-ID`.

## 9. Checklist bàn giao

- [x] Công thức Task, commit, PR và linked Task đã chốt, có quy tắc distinct.
- [x] Filter Project, Sprint, member và `[from,to)` đã chốt.
- [x] DTO request/response và enum nguồn dữ liệu đã có trong code.
- [x] OpenAPI `docs/api/reporting-v1.openapi.yaml` khớp DTO.
- [x] Quy tắc zero data, partial data và chưa sync đã chốt.
- [x] Quyền theo bốn vai trò đã chốt.
- [ ] Một reviewer backend xác nhận công thức/query khả thi.
- [ ] Một reviewer frontend xác nhận field và trạng thái UI.
- [ ] Người kiểm thử xác nhận scenario filter, RBAC và dữ liệu chưa sync.

Ba mục review cuối là phê duyệt của con người trên Pull Request; không được tự đánh
dấu hoàn thành trước khi có review thực tế.
