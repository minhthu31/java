# QUY TRÌNH LÀM VIỆC TRÊN JIRA VÀ GITHUB

## 1. Mục đích

Tài liệu này quy định quy trình chung để các thành viên nhận task, thực hiện công việc, cập nhật tiến độ, commit, tạo Pull Request, review và hoàn thành task.

Mục tiêu của quy trình:

- Tạo lịch sử làm việc rõ ràng trên Jira.
- Tạo lịch sử branch, commit và Pull Request trên GitHub.
- Liên kết công việc trên Jira với kết quả trên GitHub.
- Tránh làm việc trực tiếp trên branch `main`.
- Đảm bảo mọi sản phẩm đều được review trước khi hoàn thành.
- Giúp Team Leader và Lecturer theo dõi mức độ đóng góp của từng thành viên.

---

## 2. Vai trò trong quy trình phát triển

### 2.1. Team Leader

Team Leader chịu trách nhiệm:

- Tạo task trên Jira.
- Viết Description và Acceptance Criteria.
- Gắn task vào đúng Epic và Sprint.
- Chọn Assignee, Priority và Due date.
- Theo dõi tiến độ.
- Hỗ trợ khi task bị Blocked.
- Kiểm tra Pull Request.
- Xác nhận task đủ điều kiện hoàn thành.
- Chuẩn bị backlog cho Sprint tiếp theo.

### 2.2. Assignee

Assignee là thành viên được giao thực hiện task.

Assignee chịu trách nhiệm:

- Đọc đầy đủ nội dung task.
- Xác nhận đã nhận task.
- Chuyển trạng thái đúng thời điểm.
- Tạo branch riêng.
- Commit từng phần có ý nghĩa.
- Tự kiểm tra kết quả.
- Tạo Pull Request.
- Sửa theo góp ý của reviewer.
- Cập nhật Jira trong suốt quá trình thực hiện.

### 2.3. Reviewer

Reviewer chịu trách nhiệm:

- Kiểm tra kết quả có đúng yêu cầu không.
- Kiểm tra Acceptance Criteria.
- Kiểm tra file thay đổi có liên quan task không.
- Kiểm tra tên branch, commit và Pull Request.
- Kiểm tra token, mật khẩu hoặc dữ liệu bí mật.
- Approve hoặc yêu cầu chỉnh sửa.
- Không duyệt qua loa chỉ để hoàn thành task.

---

## 3. Quy định khi tạo task trên Jira

Mỗi task phải có tối thiểu:

- Summary rõ ràng.
- Description.
- Assignee.
- Parent Epic.
- Sprint.
- Priority.
- Due date.
- Deliverable.
- Acceptance Criteria.
- Work type phù hợp.

Không sử dụng tên task mơ hồ như:

- Làm tài liệu.
- Làm API.
- Sửa code.
- Hoàn thiện dự án.

Tên task nên mô tả kết quả cụ thể, ví dụ:

- Phân tích actor và quyền hạn trong hệ thống.
- Thiết kế ERD cơ sở dữ liệu ban đầu.
- Nghiên cứu xác thực Jira Cloud REST API.
- Xây dựng Login API.

---

## 4. Workflow trên Jira

Workflow của nhóm:

```text
To Do
  ↓
In Progress
  ↓
In Review
  ↓
Done
```

Có thể sử dụng thêm trạng thái:

```text
Blocked
```

### 4.1. To Do

Task đã được tạo và giao nhưng thành viên chưa bắt đầu.

### 4.2. In Progress

Thành viên đã đọc task, xác nhận yêu cầu và bắt đầu thực hiện.

Khi chuyển sang `In Progress`, thành viên phải bình luận:

- Đã nhận task.
- Kế hoạch thực hiện.
- Branch dự kiến.
- Thời gian dự kiến hoàn thành.

### 4.3. Blocked

Task không thể tiếp tục vì đang phụ thuộc vào công việc hoặc thông tin khác.

Khi chuyển sang `Blocked`, thành viên phải ghi:

- Nguyên nhân bị chặn.
- Task hoặc người đang phụ thuộc.
- Nội dung cần hỗ trợ.
- Ảnh hưởng đến deadline.

Khi vấn đề được giải quyết, task được chuyển lại `In Progress`.

### 4.4. In Review

Task đã có Deliverable và Pull Request, đang chờ người khác kiểm tra.

Không chuyển sang `In Review` nếu:

- Chưa có kết quả.
- Chưa có Pull Request.
- Chưa tự kiểm tra.
- Chưa đáp ứng Acceptance Criteria.

### 4.5. Done

Task chỉ được chuyển sang `Done` khi:

- Deliverable đã hoàn thành.
- Pull Request đã được review.
- Các góp ý quan trọng đã được sửa.
- Pull Request đã được merge vào `main`.
- Jira đã có link Pull Request.
- Acceptance Criteria đã được đáp ứng.

Không chuyển trực tiếp:

```text
To Do → Done
```

---

## 5. Quy định đặt tên branch

Cấu trúc chung:

```text
<type>/<JIRA-KEY>-<short-name>
```

Các loại branch:

| Type | Mục đích |
|---|---|
| `docs/` | Viết tài liệu |
| `design/` | Sơ đồ, ERD, Use Case, sitemap, wireframe |
| `research/` | Nghiên cứu API hoặc Proof of Concept |
| `setup/` | Thiết lập project, công cụ hoặc môi trường |
| `feature/` | Lập trình chức năng mới |
| `bugfix/` | Sửa lỗi |
| `test/` | Viết kiểm thử |
| `refactor/` | Cải tổ code không thay đổi chức năng |

Ví dụ:

```text
docs/CNPM-5-project-scope
setup/CNPM-6-jira-project-setup
docs/CNPM-7-development-workflow
design/CNPM-15-database-erd
research/CNPM-20-jira-authentication
feature/CNPM-30-login-api
bugfix/CNPM-40-jira-sync-error
```

Không sử dụng tên branch như:

```text
tuan
code-moi
test
branch1
abc
docs/actor-permission-matrix
```

Branch bắt buộc phải có Jira Issue Key.

---

## 6. Quan hệ giữa Jira Epic và GitHub branch

Epic trên Jira và tiền tố branch GitHub có mục đích khác nhau.

- Epic cho biết task thuộc nhóm công việc lớn nào.
- Tiền tố branch cho biết loại công việc đang được thực hiện.
- Jira Issue Key cho biết branch thuộc task cụ thể nào.

Ví dụ:

```text
Epic:
System Design

Task:
CNPM-15 – Thiết kế ERD

Branch:
design/CNPM-15-database-erd
```

Không bắt buộc tên Epic xuất hiện trong tên branch.

---

## 7. Quy định commit

Mỗi commit phải bắt đầu bằng Jira Issue Key.

Cấu trúc:

```text
<JIRA-KEY> <mô tả thay đổi>
```

Ví dụ đúng:

```text
CNPM-7 Create development workflow outline
CNPM-7 Define branch and commit conventions
CNPM-7 Add Definition of Done
CNPM-15 Add initial database ERD
CNPM-30 Implement login service
```

Ví dụ không đúng:

```text
update
fix
done
add files via upload
delete docs
abc
commit mới
```

Commit phải phản ánh một thay đổi thật và có ý nghĩa.

Không tạo commit giả hoặc sửa một dấu cách chỉ để làm lịch sử đẹp hơn.

---

## 8. Quy trình thực hiện một task

### Bước 1: Nhận task

Thành viên mở Jira và đọc:

- Summary.
- Description.
- Acceptance Criteria.
- Due date.
- Parent Epic.
- Các task phụ thuộc.

Nếu chưa hiểu, phải hỏi Team Leader trước khi thực hiện.

### Bước 2: Bắt đầu task

Chuyển:

```text
To Do → In Progress
```

Bình luận kế hoạch thực hiện trên Jira.

### Bước 3: Tạo branch

Tạo branch từ phiên bản mới nhất của `main`.

Ví dụ:

```text
docs/CNPM-9-actor-permission-matrix
```

### Bước 4: Thực hiện công việc

Làm từng phần nhỏ, kiểm tra kết quả và commit sau mỗi phần có ý nghĩa.

### Bước 5: Cập nhật tiến độ

Khi hoàn thành một phần quan trọng, bình luận trên Jira:

- Nội dung đã hoàn thành.
- Branch đang sử dụng.
- Nội dung tiếp theo.
- Vấn đề đang gặp nếu có.

### Bước 6: Tự kiểm tra

Trước khi tạo Pull Request, thành viên phải kiểm tra:

- Deliverable có đầy đủ không.
- Có đạt Acceptance Criteria không.
- Có file không liên quan không.
- Có lỗi biên dịch không.
- Có token hoặc mật khẩu không.
- Nội dung Markdown có hiển thị đúng không.
- Branch đã lấy thay đổi mới nhất từ `main` chưa.

### Bước 7: Tạo Pull Request

Tạo Pull Request:

```text
branch của task → main
```

Tiêu đề phải có Jira Issue Key.

Ví dụ:

```text
CNPM-7 Define Jira and GitHub development workflow
```

### Bước 8: Chuyển sang review

Dán link Pull Request vào Jira và chuyển:

```text
In Progress → In Review
```

### Bước 9: Review và sửa

Reviewer kiểm tra và chọn:

- Comment.
- Approve.
- Request changes.

Nếu bị yêu cầu sửa, Assignee tiếp tục sửa trên chính branch hiện tại và commit bổ sung.

### Bước 10: Merge và hoàn thành

Sau khi Pull Request được Approve:

- Merge vào `main`.
- Xóa branch nếu không còn sử dụng.
- Bình luận kết quả cuối cùng trên Jira.
- Chuyển task sang `Done`.

---

## 9. Quy định Pull Request

Tiêu đề Pull Request:

```text
<JIRA-KEY> <tên kết quả>
```

Ví dụ:

```text
CNPM-7 Define Jira and GitHub development workflow
```

Mô tả Pull Request gồm:

```markdown
## Jira Work Item

CNPM-XX

## Nội dung đã thực hiện

- ...
- ...
- ...

## Deliverable

- ...

## Cách kiểm tra

1. ...
2. ...
3. ...

## Nội dung cần review

- ...
- ...
```

Mỗi Pull Request phải:

- Có Jira Issue Key.
- Chỉ chứa file liên quan đến task.
- Có ít nhất một reviewer.
- Không chứa token hoặc mật khẩu.
- Được kiểm tra trước khi merge.

---

## 10. Quy trình review

Reviewer mở tab:

```text
Files changed
```

Reviewer kiểm tra:

- Nội dung có đúng task không.
- Có đáp ứng Acceptance Criteria không.
- Có file thừa không.
- Có xóa nhầm file không.
- Code hoặc tài liệu có rõ ràng không.
- Commit có Jira Issue Key không.
- Có dữ liệu bí mật không.

Kết quả review:

- `Approve`: đồng ý merge.
- `Request changes`: phải sửa trước khi merge.
- `Comment`: góp ý nhưng chưa quyết định.

Reviewer không nên tự sửa toàn bộ thay cho Assignee. Assignee phải tự sửa để lịch sử đóng góp được rõ ràng.

---

## 11. Definition of Done

Một task được xem là hoàn thành khi đáp ứng tất cả điều kiện:

- Có Deliverable rõ ràng.
- Deliverable nằm trên GitHub.
- Branch chứa Jira Issue Key.
- Commit chứa Jira Issue Key.
- Có Pull Request.
- Pull Request có Jira Issue Key.
- Có reviewer kiểm tra.
- Đã sửa các góp ý quan trọng.
- Pull Request đã được merge.
- Đạt Acceptance Criteria.
- Jira có link Pull Request.
- Không chứa mật khẩu, token hoặc thông tin bí mật.
- Không có file không liên quan.
- Team Leader hoặc reviewer đã chấp nhận kết quả.

File chỉ nằm trong máy cá nhân không được xem là hoàn thành.

---

## 12. Xử lý lỗi phát hiện sau khi task đã Done

Nếu lỗi nhỏ và liên quan trực tiếp đến task vừa hoàn thành, Team Leader có thể mở lại task:

```text
Done → In Progress
```

Tạo branch sửa lỗi:

```text
bugfix/CNPM-XX-short-description
```

Nếu lỗi là một công việc độc lập, phải tạo Jira Bug mới.

Ví dụ:

```text
CNPM-45 – Login API accepts invalid email
```

Branch:

```text
bugfix/CNPM-45-login-email-validation
```

Không âm thầm sửa code mà không có Jira task.

---

## 13. Quy định bảo mật

Không được commit:

- API token.
- Mật khẩu.
- File `.env`.
- Jira token.
- GitHub Personal Access Token.
- Thông tin tài khoản cá nhân.
- File cấu hình chứa secret.

Các file nhạy cảm phải được đưa vào `.gitignore`.

Nếu phát hiện secret đã được commit, phải:

1. Thu hồi hoặc thay token ngay lập tức.
2. Thông báo Team Leader.
3. Xóa dữ liệu khỏi repository.
4. Kiểm tra lịch sử commit nếu cần.

---

## 14. Quy định dành cho branch `main`

- Không thực hiện công việc trực tiếp trên `main`.
- Mọi thay đổi phải đi qua branch riêng và Pull Request.
- Chỉ merge khi đã được review.
- Không force push lên `main`.
- Không xóa `main`.
- Nên sử dụng `Squash and merge` nếu branch có nhiều commit thử nghiệm hoặc lịch sử lộn xộn.

---

## 15. Ví dụ quy trình hoàn chỉnh

Jira task:

```text
CNPM-9 – Phân tích actor và quyền hạn
```

Branch:

```text
docs/CNPM-9-actor-permission-matrix
```

Commits:

```text
CNPM-9 Create actor permission document outline
CNPM-9 Define permissions for four actors
CNPM-9 Update permissions after review
```

Pull Request:

```text
CNPM-9 Complete actor and permission analysis
```

Luồng trạng thái:

```text
To Do
→ In Progress
→ In Review
→ Done
```

Task chỉ chuyển sang `Done` sau khi Pull Request đã được review và merge vào `main`.

---

## 16. Kết luận

Quy trình Jira và GitHub giúp nhóm:

- Theo dõi rõ ai đang làm task nào.
- Ghi nhận lịch sử công việc.
- Liên kết task với branch, commit và Pull Request.
- Hạn chế thay đổi không được kiểm tra.
- Đảm bảo kết quả được review trước khi hoàn thành.
- Cung cấp dữ liệu phục vụ báo cáo tiến độ và mức độ đóng góp.
