# GitHub REST API Research

## 1. Mục tiêu

Nghiên cứu GitHub REST API phục vụ việc theo dõi hoạt động phát triển của thành viên trong hệ thống.

Các API được nghiên cứu:

- Pull Request API
- User API

# 2. Authentication

## Request Header

```http
Authorization: Bearer <YOUR_GITHUB_TOKEN>
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

> **Lưu ý**

> - Sử dụng Environment Variable.

# 3. Pull Request API

## 3.1 Lấy danh sách Pull Request

### Endpoint

```http
GET /repos/{owner}/{repo}/pulls
```

### URL

```
https://api.github.com/repos/{owner}/{repo}/pulls
```

### Query Parameters

| Parameter | Kiểu | Mặc định | Mô tả |
|-----------|------|----------|-------|
| state | string | open | open, closed hoặc all |
| sort | string | created | created, updated, popularity |
| direction | string | desc | asc hoặc desc |
| per_page | int | 30 | Số lượng kết quả mỗi trang |
| page | int | 1 | Trang hiện tại |

### Response chính

| Field | Kiểu | Mô tả |
|------|------|------|
| id | int | ID Pull Request |
| number | int | Số Pull Request |
| title | string | Tiêu đề Pull Request |
| state | string | Trạng thái |
| user.login | string | Username người tạo |
| head.ref | string | Nhánh nguồn |
| base.ref | string | Nhánh đích |
| created_at | datetime | Thời gian tạo |
| updated_at | datetime | Thời gian cập nhật |

### Response mẫu

```json
[
  {
    "id": 123456789,
    "number": 15,
    "title": "Add login feature",
    "state": "open",
    "user": {
      "login": "dev01"
    },
    "head": {
      "ref": "feature/login"
    },
    "base": {
      "ref": "main"
    },
    "created_at": "2026-08-01T09:30:00Z",
    "updated_at": "2026-08-02T11:00:00Z"
  }
]
```

### Kết quả thử nghiệm

![Kết quả xác thực GitHub API thành công](./assets/Pull-Request.png)


## 3.2 Lấy chi tiết Pull Request

### Endpoint

```http
GET /repos/{owner}/{repo}/pulls/{pull_number}
```

### URL

```
https://api.github.com/repos/{owner}/{repo}/pulls/{pull_number}
```

### Path Parameters

| Parameter | Mô tả |
|------------|-------|
| owner | Chủ sở hữu repository |
| repo | Tên repository |
| pull_number | Số Pull Request |

### Response chính

| Field | Kiểu | Mô tả |
|------|------|------|
| title | string | Tiêu đề |
| body | string | Nội dung mô tả |
| state | string | open hoặc closed |
| merged | bool | Đã merge hay chưa |
| user.login | string | Người tạo |
| head.ref | string | Nhánh nguồn |
| base.ref | string | Nhánh đích |
| created_at | datetime | Ngày tạo |
| updated_at | datetime | Ngày cập nhật |
| merged_at | datetime/null | Ngày merge |

### Response mẫu

```json
{
  "title": "Add login feature",
  "body": "Implement login API",
  "state": "closed",
  "merged": true,
  "user": {
    "login": "dev01"
  },
  "head": {
    "ref": "feature/login"
  },
  "base": {
    "ref": "main"
  },
  "created_at": "2026-07-20T09:30:00Z",
  "updated_at": "2026-07-21T11:00:00Z",
  "merged_at": "2026-07-22T08:15:00Z"
}
```

### Xác định trạng thái Pull Request

| state | merged | Kết quả |
|---------|---------|---------|
| open | false | Open |
| closed | false | Closed |
| closed | true | Merged |

### Kết quả thử nghiệm

![Kết quả xác thực GitHub API thành công](./assets/Pull-Request-Detail.png)

## 3.3 Lấy danh sách Commit của Pull Request

### Endpoint

```http
GET /repos/{owner}/{repo}/pulls/{pull_number}/commits
```

### URL

```
https://api.github.com/repos/{owner}/{repo}/pulls/{pull_number}/commits
```

### Response chính

| Field | Mô tả |
|--------|------|
| sha | Commit SHA |
| commit.message | Nội dung commit |
| author.login | Người commit |
| commit.author.date | Thời gian commit |

### Response mẫu

```json
[
  {
    "sha": "abc123...",
    "commit": {
      "message": "Add login validation"
    },
    "author": {
      "login": "dev01"
    }
  }
]
```

# 4. User API

## 4.1 Lấy thông tin GitHub User

### Endpoint

```http
GET /users/{username}
```

### URL

```
https://api.github.com/users/{username}
```

### Path Parameters

| Parameter | Mô tả |
|------------|-------|
| username | GitHub Username |

### Response chính

| Field | Kiểu | Mô tả |
|------|------|------|
| login | string | Username |
| id | int | User ID |
| name | string | Tên hiển thị |
| avatar_url | string | Ảnh đại diện |
| html_url | string | URL trang cá nhân |
| email | string/null | Public Email |

### Response mẫu

```json
{
  "login": "abc",
  "id": 583231,
  "name": "The Octocat",
  "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4",
  "html_url": "https://github.com/octocat",
  "email": null
}
```

### Kết quả thử nghiệm

![Kết quả xác thực GitHub API thành công](./assets/User.png)


# 5. Thông tin sử dụng để liên kết thành viên trong hệ thống

Đề xuất sử dụng các trường sau:

| GitHub | Hệ thống |
|---------|----------|
| id | githubUserId |
| login | githubUsername |
| html_url | githubProfileUrl |
| avatar_url | avatar |

> Nên sử dụng `id` làm khóa liên kết vì `login` có thể thay đổi.

# 6. Các lỗi thường gặp

| HTTP Status | Nguyên nhân | Cách xử lý |
|-------------|------------|------------|
| 400 Bad Request | Request không hợp lệ | Kiểm tra endpoint và tham số |
| 401 Unauthorized | Token không hợp lệ hoặc hết hạn | Kiểm tra Personal Access Token |
| 403 Forbidden | Không đủ quyền hoặc vượt Rate Limit | Kiểm tra quyền token hoặc chờ hết giới hạn |
| 404 Not Found | Repository, Pull Request hoặc User không tồn tại | Kiểm tra owner, repo và số Pull Request |
| 422 Unprocessable Entity | Tham số không hợp lệ | Kiểm tra giá trị truyền vào |
