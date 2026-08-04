# Endpoint, Tham số và Cách xác thực

## 1. Xác thực (Authentication)

### Header

```http
Authorization: Bearer {{github_token}}
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

## 2. Endpoint lấy thông tin Repository

### Method

```http
GET
```

### Endpoint

```text
https://api.github.com/repos/{{owner}}/{{repo}}
```

### Path Parameters

| Tham số | Mô tả |
|----------|------|
| `owner` | Tên chủ sở hữu (user hoặc organization) của repository. |
| `repo` | Tên repository cần lấy thông tin. |

### Thông tin thu được

- Tên repository (`name`)
- Chủ sở hữu (`owner.login`)
- URL repository (`html_url`)
- Nhánh mặc định (`default_branch`)
- Trạng thái Public/Private (`private`)


## 3. Endpoint lấy danh sách Commit

### Method

```http
GET
```

### Endpoint

```text
https://api.github.com/repos/{{owner}}/{{repo}}/commits
```

### Path Parameters

| Tham số | Mô tả |
|----------|------|
| `owner` | Tên chủ sở hữu repository. |
| `repo` | Tên repository cần lấy danh sách commit. |

### Query Parameters (tùy chọn)

| Tham số | Mô tả |
|----------|------|
| `per_page` | Số lượng commit trả về trong một request. |
| `page` | Trang dữ liệu cần lấy. |
| `sha` | Lọc commit theo một nhánh hoặc commit SHA cụ thể. |

### Thông tin thu được

- Commit SHA (`sha`)
- Commit message (`commit.message`)
- Author (`commit.author.name`)
- Commit date (`commit.author.date`)
- Commit URL (`html_url`)

## 4. Biến môi trường sử dụng trong Postman

| Biến | Mục đích |
|------|----------|
| `github_token` | Lưu GitHub Personal Access Token dùng để xác thực. |
| `owner` | Tên chủ sở hữu repository. |
| `repo` | Tên repository cần truy cập. |
