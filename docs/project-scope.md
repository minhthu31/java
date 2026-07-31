# PHẠM VI VÀ RANH GIỚI HỆ THỐNG

## 1. Thông tin đề tài

### 1.1. Tên tiếng Việt

**Công cụ hỗ trợ quản lý yêu cầu và tiến độ dự án phần mềm cho môn Công nghệ phần mềm thông qua Jira và GitHub.**

### 1.2. Tên tiếng Anh

**A Supporting Tool for Requirements and Project Progress Management in Software Project Course Using Jira and GitHub.**

---

## 2. Bối cảnh và vấn đề cần giải quyết

Trong quá trình thực hiện đồ án môn Công nghệ phần mềm, các nhóm sinh viên thường sử dụng nhiều công cụ khác nhau để quản lý dự án.

Jira thường được sử dụng để quản lý:

- Yêu cầu phần mềm.
- Công việc cần thực hiện.
- Người được giao công việc.
- Backlog và Sprint.
- Trạng thái và tiến độ của task.

GitHub thường được sử dụng để quản lý:

- Mã nguồn của dự án.
- Branch.
- Commit.
- Pull Request.
- Lịch sử thay đổi mã nguồn.
- Quá trình kiểm thử và tích hợp mã nguồn.

Việc dữ liệu bị phân tán trên nhiều nền tảng làm phát sinh một số khó khăn:

- Team Leader phải tổng hợp tiến độ của nhóm thủ công.
- Lecturer phải kiểm tra riêng Jira và GitHub để đánh giá nhóm.
- Khó xác định một commit trên GitHub đang phục vụ task nào trên Jira.
- Khó biết thành viên nào đang thực hiện công việc nào.
- Khó đánh giá chính xác mức độ đóng góp của từng thành viên.
- Số lượng commit không phản ánh đầy đủ chất lượng công việc.
- Có thể xuất hiện task đã hoàn thành nhưng không có commit liên quan.
- Có thể xuất hiện commit nhưng không liên kết với bất kỳ task nào.
- Việc tổng hợp yêu cầu thành tài liệu SRS mất nhiều thời gian.
- Lịch sử phân công, cập nhật trạng thái và thực hiện công việc chưa được tổng hợp trên một giao diện thống nhất.

Vì vậy, nhóm xây dựng một website trung gian để kết nối Jira và GitHub, đồng thời tổng hợp dữ liệu thành các báo cáo phục vụ Team Member, Team Leader, Lecturer và Admin.

---

## 3. Mục tiêu của hệ thống

Hệ thống được xây dựng với các mục tiêu chính sau:

1. Hỗ trợ quản lý yêu cầu phần mềm của từng nhóm sinh viên.
2. Tổng hợp các yêu cầu được quản lý trên Jira thành tài liệu Đặc tả Yêu cầu Phần mềm — SRS.
3. Cho phép Team Leader tạo và giao task cho thành viên trên website.
4. Đẩy task được tạo trên website lên Jira thông qua Jira Cloud REST API.
5. Lấy project, issue, backlog, sprint, assignee và tiến độ từ Jira.
6. Lấy repository, commit, Pull Request và thông tin người dùng từ GitHub.
7. Liên kết task trên Jira với commit hoặc Pull Request trên GitHub.
8. Tổng hợp báo cáo phân công và thực hiện công việc của các thành viên.
9. Thống kê tần suất và chất lượng commit.
10. Hỗ trợ Lecturer đánh giá mức độ đóng góp của từng thành viên.
11. Hỗ trợ Team Leader theo dõi tiến độ của toàn nhóm.
12. Hỗ trợ Team Member theo dõi task và thống kê cá nhân.
13. Phân loại task và xác định các task thuộc cùng một tính năng.
14. Hỗ trợ theo dõi kết quả auto test.
15. Tự động lưu lại lịch sử hoạt động liên quan đến task.

---

## 4. Vai trò của các nền tảng

Hệ thống được xây dựng dựa trên sự kết hợp giữa website của nhóm, Jira và GitHub. Mỗi nền tảng đảm nhiệm một vai trò riêng, tránh trùng lặp chức năng và giúp quá trình quản lý dự án được rõ ràng hơn.

Website của nhóm đóng vai trò trung gian để kết nối, tổng hợp và phân tích dữ liệu. Jira là nền tảng quản lý yêu cầu và công việc. GitHub là nền tảng quản lý mã nguồn và hoạt động phát triển phần mềm.

### 4.1. Website của nhóm

Website của nhóm là hệ thống trung tâm giúp người dùng quản lý và theo dõi toàn bộ quá trình thực hiện dự án trên một giao diện thống nhất.

Các nhiệm vụ chính của website gồm:

- Quản lý tài khoản và phân quyền theo bốn vai trò: Admin, Lecturer, Team Leader và Team Member.
- Quản lý nhóm sinh viên, thành viên, Team Leader và Lecturer phụ trách.
- Lưu thông tin liên kết giữa tài khoản trong hệ thống với tài khoản Jira và GitHub của từng thành viên.
- Quản lý các yêu cầu phần mềm của nhóm.
- Tổng hợp các yêu cầu được quản lý trên Jira để hỗ trợ tạo tài liệu SRS.
- Cho phép Team Leader tạo task và giao task cho thành viên trực tiếp trên website.
- Gửi thông tin task từ website lên Jira thông qua Jira Cloud REST API.
- Lấy dữ liệu project, issue, backlog, sprint, trạng thái và tiến độ từ Jira.
- Lấy dữ liệu repository, commit, pull request và người đóng góp từ GitHub.
- Liên kết task trên Jira với commit hoặc pull request trên GitHub.
- Tổng hợp báo cáo phân công và thực hiện công việc của từng thành viên.
- Thống kê tần suất và chất lượng commit.
- Hỗ trợ Lecturer và Team Leader theo dõi tiến độ dự án và mức độ đóng góp của từng thành viên.
- Lưu nhật ký các hoạt động quan trọng như tạo task, giao task, thay đổi trạng thái và đồng bộ dữ liệu.

Website không có mục tiêu thay thế hoàn toàn Jira hoặc GitHub. Website chỉ cung cấp các chức năng quản lý cần thiết cho môn học, đồng thời kết nối dữ liệu từ hai nền tảng để tạo báo cáo tổng hợp.

### 4.2. Jira

Jira là nền tảng chính được sử dụng để quản lý yêu cầu phần mềm, công việc và tiến độ thực hiện dự án.

Các dữ liệu được quản lý trên Jira gồm:

- Project.
- Requirement.
- Epic hoặc Feature.
- Story.
- Task.
- Bug.
- Subtask.
- Backlog.
- Sprint.
- Người được giao công việc.
- Trạng thái công việc.
- Mức độ ưu tiên.
- Thời hạn hoàn thành.
- Mối quan hệ giữa các task và tính năng.

Trong hệ thống, Jira có hai hướng trao đổi dữ liệu với website.

#### Website gửi dữ liệu lên Jira

Team Leader tạo task trên website, chọn thành viên thực hiện và nhập các thông tin cần thiết như:

- Tên task.
- Mô tả.
- Loại task.
- Người thực hiện.
- Mức độ ưu tiên.
- Thời hạn.
- Sprint hoặc tính năng liên quan.

Website gọi Jira Cloud REST API để tạo một issue tương ứng trên Jira. Sau khi tạo thành công, Jira trả về Issue ID và Issue Key, ví dụ `CNPM-15`. Website lưu các thông tin này để liên kết task nội bộ với task trên Jira.

#### Website lấy dữ liệu từ Jira

Website lấy các dữ liệu mới nhất từ Jira, gồm:

- Danh sách project.
- Requirements và issues.
- Backlog và sprint.
- Người được giao task.
- Trạng thái task.
- Deadline và priority.
- Tiến độ thực hiện.

Dữ liệu lấy từ Jira được sử dụng để hiển thị dashboard, báo cáo tiến độ và tình trạng công việc của từng thành viên.

Jira là nơi quản lý task chính của dự án. Nhóm không tạo cùng một task trên cả Jira và GitHub Issues để tránh dữ liệu bị trùng lặp hoặc không đồng nhất.

### 4.3. GitHub

GitHub là nền tảng được sử dụng để lưu trữ mã nguồn và quản lý quá trình phát triển phần mềm của nhóm.

Các dữ liệu được quản lý trên GitHub gồm:

- Repository.
- Source code.
- Branch.
- Commit.
- Pull Request.
- GitHub user và contributor.
- Lịch sử thay đổi mã nguồn.
- Kết quả build và auto test khi có.
- Các file được thêm, sửa hoặc xóa.

Mỗi thành viên thực hiện task trên một branch riêng. Tên branch, commit message và Pull Request nên chứa Jira Issue Key để hệ thống có thể liên kết hoạt động trên GitHub với task tương ứng trên Jira.

Ví dụ:

- Jira Issue Key: `CNPM-15`
- Branch: `feature/CNPM-15-login-api`
- Commit: `CNPM-15 Implement login API`
- Pull Request: `CNPM-15 Complete login feature`

Website sử dụng GitHub REST API để lấy:

- Danh sách repository.
- Danh sách commit.
- Commit SHA.
- Commit message.
- Người tạo commit.
- Thời gian commit.
- Danh sách Pull Request.
- Trạng thái Pull Request.
- Thông tin người dùng và contributor.
- Kết quả workflow hoặc auto test khi có.

Dữ liệu GitHub được sử dụng để:

- Thống kê số lượng và tần suất commit.
- Kiểm tra commit có liên kết với task hay không.
- Theo dõi Pull Request của từng thành viên.
- Hỗ trợ đánh giá chất lượng commit.
- Hỗ trợ xác định mức độ đóng góp cá nhân.
- Phát hiện commit không chứa Jira Issue Key hoặc chưa liên kết với task.

GitHub không phải nền tảng giao task chính trong phạm vi hệ thống. Thành viên nhận task từ website hoặc Jira, sau đó sử dụng GitHub để phát triển mã nguồn, commit và tạo Pull Request.
## 5. Các vai trò người dùng

### 5.1. Admin

[Quyền hạn của Admin]

### 5.2. Lecturer

[Quyền hạn của Lecturer]

### 5.3. Team Leader

[Quyền hạn của Team Leader]

### 5.4. Team Member

[Quyền hạn của Team Member]

## 6. Phạm vi chức năng chính

[Liệt kê những nhóm chức năng thuộc phạm vi hệ thống]

## 7. Phạm vi MVP

[Liệt kê những chức năng phải hoàn thành trước]

## 8. Chức năng thực hiện sau MVP

[Liệt kê chức năng làm sau khi MVP ổn định]

## 9. Ngoài phạm vi phiên bản đầu

[Liệt kê những nội dung nhóm không triển khai trong phiên bản đầu]

## 10. Tiêu chí thành công

[Điều kiện để xác định sản phẩm đạt yêu cầu]
