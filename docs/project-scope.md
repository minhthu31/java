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

Hệ thống gồm bốn vai trò chính:

- Admin.
- Lecturer.
- Team Leader.
- Team Member.

Mỗi vai trò chỉ được truy cập các chức năng và dữ liệu phù hợp với nhiệm vụ của mình.

### 5.1. Admin

Admin chịu trách nhiệm quản lý cấu trúc chung của hệ thống và cấu hình kết nối với các nền tảng bên ngoài.

Các chức năng chính của Admin gồm:

- Quản lý các nhóm sinh viên.
- Tạo mới, xem và cập nhật thông tin nhóm.
- Thêm hoặc xóa sinh viên khỏi nhóm.
- Chỉ định Team Leader cho nhóm.
- Quản lý thông tin Lecturer.
- Phân công Lecturer phụ trách từng nhóm.
- Cấu hình Jira Project cho nhóm.
- Cấu hình GitHub Repository cho nhóm.
- Quản lý thông tin liên kết giữa hệ thống với Jira và GitHub.
- Kiểm tra trạng thái kết nối Jira và GitHub.
- Theo dõi các lỗi cấu hình hoặc lỗi đồng bộ cơ bản.

Admin không phải là người trực tiếp quản lý công việc hằng ngày của nhóm. Việc tạo và giao task cho thành viên thuộc trách nhiệm chính của Team Leader.

### 5.2. Lecturer

Lecturer chịu trách nhiệm theo dõi, quản lý và đánh giá các nhóm được Admin phân công.

Các chức năng chính của Lecturer gồm:

- Xem danh sách các nhóm được phân công.
- Xem danh sách sinh viên trong từng nhóm.
- Theo dõi thông tin và hoạt động của sinh viên trong nhóm phụ trách.
- Xem các requirements của dự án.
- Xem danh sách task và người được giao.
- Xem trạng thái và deadline của task.
- Xem tiến độ tổng thể của dự án.
- Xem tiến độ theo Sprint.
- Xem báo cáo phân công và thực hiện công việc.
- Xem thống kê commit của từng thành viên.
- Xem tổng hợp Pull Request.
- Xem kết quả auto test khi có.
- Xem mức độ đóng góp của từng thành viên.
- Xem các cảnh báo như task quá hạn, task chưa có commit hoặc commit chưa liên kết với task.

Lecturer chỉ được truy cập dữ liệu của các nhóm mà mình được phân công.

### 5.3. Team Leader

Team Leader là người trực tiếp quản lý yêu cầu, task và tiến độ của nhóm.

Các chức năng chính của Team Leader gồm:

- Quản lý các requirements của nhóm.
- Tạo mới và cập nhật requirement.
- Quản lý các task thuộc dự án.
- Tạo task trực tiếp trên website.
- Giao task cho thành viên thuộc nhóm.
- Chọn loại task.
- Đặt mức độ ưu tiên.
- Đặt deadline.
- Chọn Sprint.
- Xác định Feature hoặc Epic liên quan.
- Đẩy task từ website lên Jira.
- Theo dõi trạng thái đồng bộ với Jira.
- Theo dõi tiến độ của từng task.
- Theo dõi tiến độ của từng thành viên.
- Xem task quá hạn.
- Xem task chưa có commit liên quan.
- Xem commit chưa liên kết với task.
- Xem tổng hợp commit và Pull Request của nhóm.
- Xem báo cáo tiến độ và mức độ đóng góp của thành viên.

Team Leader chỉ được quản lý dữ liệu của nhóm mình.

### 5.4. Team Member

Team Member là người trực tiếp thực hiện các task được giao.

Các chức năng chính của Team Member gồm:

- Xem danh sách task được giao.
- Xem chi tiết task.
- Xem mô tả và Acceptance Criteria.
- Xem priority và deadline.
- Xem Sprint và Feature liên quan.
- Cập nhật trạng thái task.
- Thực hiện công việc trên branch riêng.
- Commit source code lên GitHub.
- Tạo Pull Request.
- Xem lịch sử commit cá nhân.
- Xem task đã hoàn thành.
- Xem task đang thực hiện.
- Xem task quá hạn.
- Xem thống kê task cá nhân.
- Xem thống kê commit cá nhân.
- Xem kết quả auto test liên quan đến commit hoặc task của mình.

Team Member không được tự ý giao task cho thành viên khác nếu không có quyền Team Leader.

---

## 6. Phạm vi chức năng chính của hệ thống

### 6.1. Đăng nhập và phân quyền

Hệ thống hỗ trợ:

- Đăng nhập.
- Đăng xuất.
- Xác định danh tính người dùng.
- Xác định vai trò của người dùng.
- Giới hạn chức năng theo từng vai trò.
- Ngăn người dùng truy cập dữ liệu không thuộc quyền quản lý.
- Bảo vệ các API nội bộ.
- Lưu thông tin tài khoản Jira và GitHub của thành viên.

Bốn vai trò chính:

```text
ADMIN
LECTURER
TEAM_LEADER
TEAM_MEMBER
```
### 6.2. Quản lí nhóm 

Hệ thống hỗ trợ:

- Tạo nhóm sinh viên.
- Xem danh sách nhóm.
- Xem chi tiết nhóm.
- Cập nhật thông tin nhóm.
- Thêm thành viên vào nhóm.
- Xóa thành viên khỏi nhóm.
- Chỉ định Team Leader.
- Phân công Lecturer.
- Gắn Jira Project với nhóm.
- Gắn GitHub Repository với nhóm.

Mỗi nhóm có thể lưu các thông tin như:

- Tên nhóm.
- Mã nhóm.
- Danh sách thành viên.
- Team Leader.
- Lecturer phụ trách.
- Jira Project Key.
- GitHub Repository.
- Ngày bắt đầu và ngày kết thúc dự án.

### 6.3. Quản lý yêu cầu phần mềm và SRS

Hệ thống hỗ trợ:

- Tạo và quản lý requirement.
- Lấy requirement hoặc issue từ Jira.
- Xem danh sách requirement.
- Cập nhật mô tả requirement.
- Phân loại requirement.
- Đặt mức độ ưu tiên.
- Theo dõi trạng thái requirement.
- Liên kết requirement với task.
- Tổ chức requirement theo cấu trúc SRS.
- Cho phép Team Leader và Lecturer xem SRS.

Một requirement trong SRS có thể gồm:

- Requirement ID.
- Tên yêu cầu.
- Mô tả.
- Actor.
- Priority.
- Precondition.
- Main Flow.
- Alternative Flow.
- Exception Flow.
- Postcondition.
- Related Tasks.
- Status.
### 6.4. Tạo và giao task

Team Leader có thể tạo task trên website với các thông tin:

- Tên task.
- Mô tả.
- Issue Type.
- Priority.
- Assignee.
- Deadline.
- Sprint.
- Feature hoặc Epic.
- Label.
- Acceptance Criteria.

Trước khi tạo task, hệ thống cần kiểm tra:

- Người được giao có thuộc nhóm không.
- Thành viên đã liên kết tài khoản Jira chưa.
- Jira Project đã được cấu hình chưa.
- Các trường bắt buộc đã được nhập đầy đủ chưa.
- Deadline có hợp lệ không.
- Task có thuộc đúng nhóm không.

Sau khi task được tạo trên website, hệ thống có thể gửi task đó lên Jira.

### 6.5. Tích hợp Jira

Hệ thống sử dụng Jira Cloud REST API để thực hiện các chức năng:

- Lấy danh sách project.
- Lấy thông tin issue.
- Lấy danh sách backlog.
- Lấy danh sách Sprint.
- Lấy assignee.
- Lấy status.
- Lấy priority.
- Lấy deadline.
- Lấy Epic hoặc Feature liên quan.
- Tạo Jira Issue.
- Giao assignee cho Issue.
- Cập nhật một số thông tin của task.
- Lưu Jira Issue ID và Jira Issue Key.
- Theo dõi trạng thái đồng bộ.

Các trạng thái đồng bộ dự kiến:

NOT_SYNCED
SYNCING
SYNCED
SYNC_FAILED

Khi đồng bộ thất bại:

- Task nội bộ vẫn được lưu.
- Hệ thống hiển thị nguyên nhân lỗi.
- Người dùng có thể thực hiện đồng bộ lại.
- Việc thử lại không được tạo task trùng trên Jira.
### 6.6. Tích hợp GitHub

Hệ thống sử dụng GitHub REST API để:

- Lấy thông tin Repository.
- Lấy danh sách commit.
- Lấy chi tiết commit.
- Lấy Commit SHA.
- Lấy Commit Message.
- Lấy tác giả commit.
- Lấy thời gian commit.
- Lấy danh sách Pull Request.
- Lấy trạng thái Pull Request.
- Lấy thông tin user hoặc contributor.
- Lấy kết quả workflow hoặc auto test khi có.
- Lấy thông tin các file được thay đổi khi cần.

Dữ liệu GitHub được sử dụng để tạo thống kê và liên kết với task Jira.

### 6.7. Liên kết task Jira với commit GitHub

Hệ thống dự kiến sử dụng Jira Issue Key để liên kết dữ liệu giữa Jira và GitHub.

Ví dụ:

Jira Task:
CNPM-15

Branch:
feature/CNPM-15-login-api

Commit:
CNPM-15 Implement login API

Pull Request:
CNPM-15 Complete login feature

Hệ thống tìm Jira Issue Key trong:

- Tên branch.
- Commit Message.
- Tiêu đề Pull Request.
- Mô tả Pull Request.

Sau đó hệ thống liên kết hoạt động GitHub với task tương ứng.

Hệ thống cũng cần phát hiện các trường hợp:

- Commit không chứa Jira Issue Key.
- Jira Issue Key trong commit không tồn tại.
- Task chưa có commit.
- Task đã Done nhưng chưa có commit.
- Commit được tạo bởi người khác với assignee.
- Pull Request chưa được merge.
- Auto test thất bại.
  
Xử lý Jira Issue Key không hợp lệ

Khi branch, commit hoặc Pull Request không chứa Jira Issue Key,
chứa sai định dạng hoặc chứa Issue Key không tồn tại, hệ thống:

- Không tự động liên kết hoạt động GitHub với bất kỳ Jira task nào.
- Đánh dấu commit hoặc Pull Request là chưa liên kết.
- Hiển thị cảnh báo để Team Leader kiểm tra.
- Cho phép liên kết thủ công với Jira task phù hợp khi cần.
- Không tự động đoán hoặc gán dữ liệu vào một Jira Issue khác.

Ví dụ:

- Hợp lệ: `CNPM-15 Implement login API`
- Sai định dạng: `CNPM15 Implement login API`
- Sai Issue Key: `CNPM-51 Implement login API` khi `CNPM-51`
  không tồn tại.
### 6.8. Báo cáo tiến độ dự án

Hệ thống hiển thị các thông tin:

- Tổng số task.
- Số task To Do.
- Số task In Progress.
- Số task In Review.
- Số task Done.
- Số task quá hạn.
- Tỷ lệ hoàn thành.
- Tiến độ theo Sprint.
- Tiến độ theo thành viên.
- Tiến độ theo Feature hoặc Epic.
- Thời điểm đồng bộ Jira gần nhất.

Cách tính tiến độ cơ bản:

Tiến độ = Số task đã hoàn thành / Tổng số task × 100%

Trong tương lai, nhóm có thể bổ sung cách tính dựa trên Story Point.

### 6.9. Báo cáo commit và mức độ đóng góp

Hệ thống thống kê:

- Tổng số commit.
- Số commit theo thành viên.
- Số commit theo ngày.
- Số commit theo tuần.
- Số commit theo Sprint.
- Số ngày có hoạt động.
- Commit đã liên kết với task.
- Commit chưa liên kết.
- Pull Request đã tạo.
- Pull Request đã merge.
- Kết quả auto test.
- Task đã hoàn thành.
- Task quá hạn.

Một số tiêu chí hỗ trợ đánh giá chất lượng commit:

- Commit Message có rõ ràng không.
- Commit có chứa Jira Issue Key không.
- Commit có liên quan task được giao không.
- Pull Request có được merge không.
- Auto test có Passed không.
- Commit có bị revert không.
- Kích thước thay đổi có bất thường không.

Các số liệu trên chỉ hỗ trợ Lecturer đánh giá, không tự động quyết định điểm cuối cùng.

### 6.10. Phân loại task

Hệ thống hỗ trợ phân loại task theo:

- Feature.
- Bug.
- Testing.
- Documentation.
- Refactoring.
- Logging.
- Deployment.
- Research.
UI/UX.

Hệ thống cũng cần xác định các task thuộc cùng một tính năng.

Ví dụ:

Feature: Đăng nhập
├── Thiết kế giao diện đăng nhập
├── Viết API đăng nhập
├── Viết unit test đăng nhập
└── Thêm log đăng nhập

Việc nhóm các task có thể được thực hiện bằng:

- Epic.
- Parent.
- Label.
- Component.
- Feature ID.
### 6.11. Auto test

Hệ thống dự kiến lấy kết quả kiểm thử tự động từ GitHub.

Luồng cơ bản:

- Team Member push code lên GitHub.
- GitHub Actions hoặc công cụ tương đương chạy test.
- Kết quả được ghi nhận là Passed hoặc Failed.
- Website lấy kết quả thông qua API.
- Website hiển thị kết quả theo commit hoặc task.

Thông tin có thể hiển thị:

- Tên workflow.
- Trạng thái chạy.
- Thời gian chạy.
- Commit liên quan.
- Người tạo commit.
- Kết quả Passed hoặc Failed.
### 6.12. Auto log task

Hệ thống tự động lưu lại lịch sử hoạt động của task.

Các hoạt động cần ghi log gồm:

- Task được tạo.
- Task được giao.
- Thay đổi assignee.
- Thay đổi priority.
- Thay đổi deadline.
- Thay đổi trạng thái.
- Đồng bộ lên Jira.
- Đồng bộ thất bại.
- Phát hiện commit liên quan.
- Phát hiện Pull Request.
- Pull Request được merge.
- Auto test Passed hoặc Failed.
- Task được chuyển sang Done.

Mỗi log có thể gồm:

- Thời gian.
- Người thực hiện.
- Loại hoạt động.
- Task liên quan.
- Nội dung thay đổi.
- Kết quả thực hiện.

## 7. Phạm vi MVP

MVP là phiên bản tối thiểu có thể chạy và thể hiện được giá trị cốt lõi của đề tài.

Nhóm ưu tiên hoàn thành các chức năng sau:

- Đăng nhập.
- Phân quyền bốn vai trò.
- Quản lý nhóm sinh viên.
- Quản lý Lecturer.
- Phân công Lecturer cho nhóm.
- Cấu hình Jira Project.
- Cấu hình GitHub Repository.
- Quản lý requirement cơ bản.
- Tạo SRS cơ bản.
- Team Leader tạo task trên website.
- Team Leader giao task cho thành viên.
- Đẩy task từ website lên Jira.
- Lưu Jira Issue ID và Jira Issue Key.
- Lấy task và trạng thái từ Jira.
- Lấy Sprint và tiến độ từ Jira.
- Lấy commit từ GitHub.
- Liên kết commit với task bằng Jira Issue Key.
- Hiển thị báo cáo tiến độ nhóm.
- Hiển thị thống kê task theo thành viên.
- Hiển thị thống kê commit theo thành viên.

MVP được xem là đạt khi luồng sau hoạt động:

Team Leader tạo task trên Website
        ↓
Website đẩy task lên Jira
        ↓
Team Member thực hiện task
        ↓
Team Member commit code lên GitHub
        ↓
Website lấy dữ liệu Jira và GitHub
        ↓
Website liên kết task với commit
        ↓
Website hiển thị báo cáo

MVP chưa cần có giao diện quá đẹp hoặc toàn bộ chức năng nâng cao. Mục tiêu chính là chứng minh luồng tích hợp Jira và GitHub hoạt động chính xác.

## 8. Chức năng thực hiện sau MVP

Sau khi luồng cốt lõi đã hoạt động, nhóm tiếp tục triển khai:

- Phân loại task chi tiết.
- Nhóm task theo Feature hoặc Epic.
- Auto test qua GitHub Actions.
- Auto log task chi tiết.
- Đánh giá chất lượng commit theo nhiều tiêu chí.
- Thống kê Pull Request chi tiết.
- Liên kết commit thủ công.
- Cảnh báo task quá hạn.
- Cảnh báo task Done nhưng chưa có commit.
- Cảnh báo commit chưa liên kết.
- Dashboard nâng cao.
- Đồng bộ Jira theo lịch.
- Tìm kiếm và lọc báo cáo.
- Xuất báo cáo cơ bản.

Trong đó:

- Phân loại task.
- Auto test.
- Auto log task.

là những nội dung có trong đề bài, vì vậy cần được triển khai ít nhất ở mức cơ bản sau khi MVP hoạt động ổn định.

## 9. Ngoài phạm vi phiên bản đầu

Các chức năng sau không bắt buộc trong phiên bản đầu:

- Dùng AI để đánh giá source code.
- Dùng AI để đánh giá commit.
- Dùng AI để phân loại task.
- Tự động đề xuất người nhận task.
- Tự động chấm điểm thành viên.
- Xếp hạng thành viên.
- Gửi thông báo qua email.
- Gửi thông báo qua Zalo, Discord hoặc Slack.
- Theo dõi thời gian làm việc chi tiết.
- Phân tích độ phức tạp của source code.
- Phát hiện code trùng lặp.
- Dự đoán task có nguy cơ trễ.
- So sánh nhiều nhóm bằng dashboard nâng cao.
- Hỗ trợ GitLab.
- Hỗ trợ Bitbucket.
- Phát triển ứng dụng điện thoại riêng.
- Đồng bộ thời gian thực bằng webhook.
- Giải quyết xung đột dữ liệu phức tạp.
- Hỗ trợ nhiều Jira Site cho một nhóm.
- Hỗ trợ nhiều Repository cho một dự án.
- Xuất báo cáo PDF hoặc Excel nâng cao.

Những chức năng này chỉ được thực hiện khi các yêu cầu cốt lõi đã hoàn thành và nhóm còn đủ thời gian.

## 10. Tiêu chí thành công

Phiên bản đầu của hệ thống không tập trung vào:

- Xây dựng lại toàn bộ Jira.
- Xây dựng lại toàn bộ GitHub.
- Thay thế Jira trong việc quản lý công việc.
- Thay thế GitHub trong việc quản lý mã nguồn.
- Tạo cùng một task trên cả Jira và GitHub Issues.
- Quản lý nội dung source code thay cho GitHub.
- Hỗ trợ nhiều nền tảng quản lý task ngoài Jira.
- Hỗ trợ nhiều nền tảng quản lý source code ngoài GitHub.
- Tự động quyết định điểm cuối kỳ của sinh viên.
- Thay thế hoàn toàn việc đánh giá của Lecturer.
- Phân tích chuyên sâu chất lượng thuật toán.
- Phân tích toàn bộ kiến trúc source code.
- Xây dựng hệ thống quản lý đào tạo hoàn chỉnh.
- Quản lý học phí, điểm danh hoặc thời khóa biểu.
- Hỗ trợ nhiều trường học hoặc nhiều tổ chức độc lập.
- Xây dựng ứng dụng di động riêng.
- Đồng bộ mọi trường dữ liệu của Jira.
- Sao chép toàn bộ chức năng Pull Request của GitHub vào website.
- Lưu trữ hoặc chỉnh sửa trực tiếp toàn bộ source code trên website.

Ranh giới chính của sản phẩm là:

- Website chỉ quản lý các dữ liệu cần thiết cho môn Công nghệ phần mềm, kết nối Jira và GitHub, sau đó tổng hợp thành báo cáo tiến độ và mức độ đóng góp.
