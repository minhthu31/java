# Jira – GitHub Linking Convention

## Mục tiêu

Quy định cách đặt tên branch, commit message và Pull Request để liên kết hoạt động trên GitHub với Jira Issue tương ứng, giúp theo dõi tiến độ và truy vết thay đổi một cách nhất quán.

## 1. Quy ước Jira Issue Key

Mỗi công việc sử dụng Jira Issue Key do Jira tự tạo.

**Định dạng:**
```
<ProjectKey>-<IssueNumber>
```

**Ví dụ:**

| Hợp lệ | Không hợp lệ |
|---|---|
| `CNPM-15` | `CNPM15` |
| `PROJ1-102` | `cnpm-15` |
| `S0-5` | `#15` |

**Quy tắc**
- Phải sử dụng đúng Project Key.
- Project Key viết hoa.
- Có dấu `-` ngăn cách Project Key và số Issue.
- Không tự ý thay đổi Issue Key.


## 2. Quy ước đặt tên Branch

**Định dạng**
```
<branch-type>/<issue-key>-<short-description>
```

**Ví dụ**
```
feature/CNPM-15-login-api
fix/CNPM-20-jira-sync-error
docs/CNPM-25-update-srs
refactor/CNPM-30-user-service
test/CNPM-41-login-test
chore/CNPM-50-update-ci
```

**Branch Type**

| Type | Mục đích |
|---|---|
| `feature` | Phát triển chức năng mới |
| `fix` | Sửa lỗi |
| `docs` | Cập nhật tài liệu |
| `refactor` | Cải thiện cấu trúc code |
| `test` | Viết hoặc cập nhật kiểm thử |
| `chore` | Công việc cấu hình, bảo trì |

**Quy tắc**
- Branch phải chứa Jira Issue Key.
- Mô tả ngắn gọn.
- Sử dụng chữ thường và dấu `-` để ngăn cách các từ trong phần mô tả.
- Không sử dụng khoảng trắng.

## 3. Quy ước Commit Message

**Định dạng**
```
<issue-key> <commit-message>
```

**Ví dụ hợp lệ**
```
CNPM-15 Implement login API
CNPM-15 Add login validation
CNPM-20 Fix Jira synchronization error
CNPM-25 Update SRS document
```

**Ví dụ không hợp lệ**
```
update
fix code
done
Implement login API
CNPM15 Fix login
```

**Quy tắc**
- Commit phải bắt đầu bằng Jira Issue Key.
- Nội dung mô tả ngắn gọn thay đổi đã thực hiện.
- Không sử dụng các thông điệp chung chung như:
  - `update`
  - `fix code`
  - `done`

## 4. Quy ước Pull Request

### Tiêu đề

**Định dạng**
```
<issue-key> <pull-request-title>
```

**Ví dụ**
```
CNPM-15 Complete login feature
CNPM-20 Fix Jira synchronization error
CNPM-30 Refactor authentication service
```

### Mô tả Pull Request

Mỗi Pull Request nên bao gồm các nội dung sau:

**Jira Issue**
```
CNPM-15
```

**Nội dung đã thực hiện**
- Mô tả các thay đổi chính.
- Liệt kê chức năng hoặc lỗi đã xử lý.

**Cách kiểm tra**

Ví dụ:
1. Chạy ứng dụng.
2. Đăng nhập bằng tài khoản hợp lệ.
3. Kiểm tra phản hồi API.
4. Xác nhận không phát sinh lỗi.

**Kết quả kiểm thử**

Đính kèm nếu có:
- Ảnh chụp màn hình
- Video
- Kết quả test
- Log hệ thống

## 5. Phát hiện Jira Issue Key

Hệ thống tìm Jira Issue Key trong một trong các vị trí sau:
- Tên branch
- Commit message
- Tiêu đề Pull Request

**Biểu thức định dạng**
```
[A-Z][A-Z0-9]+-[0-9]+
```

**Ví dụ phát hiện được**
```
CNPM-15
S0-5
PROJ1-102
```

## 6. Xử lý Commit không chứa Issue Key

Nếu commit không chứa Jira Issue Key:

1. Commit sẽ không được tự động liên kết với Jira Issue.
2. Thành viên cần sửa commit message trước khi push nếu có thể.
3. Nếu commit đã được push:
   - Liên kết thủ công với Jira, hoặc
   - Ghi rõ Jira Issue Key trong Pull Request.
4. Commit có thể được đưa vào danh sách chưa liên kết để Leader kiểm tra.

Không được tự động gán commit vào bất kỳ Jira Issue nào khi không xác định được Issue Key.

## 7. Ví dụ tổng hợp

| Thành phần | Hợp lệ | Không hợp lệ |
|---|---|---|
| Branch | `feature/CNPM-15-login-api` | `feature/login-api` |
| Branch | `fix/CNPM-20-auth-error` | `CNPM-20-fix` |
| Commit | `CNPM-15 Implement login API` | `Implement login API` |
| Commit | `CNPM-20 Fix authentication bug` | `fix code` |
| PR Title | `CNPM-15 Complete login feature` | `Complete login feature` |
| Issue Key | `CNPM-15` | `CNPM15`, `cnpm-15`, `#15` |

