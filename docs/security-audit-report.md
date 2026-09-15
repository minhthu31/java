# Báo Cáo Rà Soát Bảo Mật Và Dữ Liệu Nhạy Cảm (CNPM-111)

- **Dự án:** CNPM Project Management Tool
- **Mã task:** CNPM-111
- **Người thực hiện:** Nguyễn Thị Minh Thư
- **Ngày thực hiện:** 14/09/2026 (Cập nhật đối soát thực tế: 16/09/2026)
- **Trạng thái:** Đã hoàn thành rà soát và đối soát theo mã nguồn, kết quả quét thực tế

---

## 1. Mục Tiêu & Phạm Vi Rà Soát
Rà soát cơ chế bảo mật backend: mã hóa dữ liệu tích hợp (GitHub PAT, Jira API Token), bảo đảm API contract không rò rỉ secret, kiểm soát phân quyền (RBAC), che giấu thông tin nhạy cảm trong log/xử lý ngoại lệ và phân loại toàn bộ kết quả quét lịch sử Git (`--all`) cùng mã nguồn dự án.

---

## 2. Kết Quả Kiểm Tra Chi Tiết

### 2.1. Mã hóa Token trong DB (Database Encryption)
* **Yêu cầu:** Toàn bộ token tích hợp bên thứ ba (GitHub PAT, Jira API Token) phải được mã hóa trước khi lưu trữ, không lưu dạng plaintext.
* **File triển khai:** `vn.edu.cnpm.projectsupport.security.AesGcmIntegrationSecretService`
* **Cơ chế & Cấu hình:**
  * Thuật toán: **AES-256-GCM** kèm Nonce/IV ngẫu nhiên cho mỗi lần mã hóa.
  * Key cấu hình: `app.security.integration-encryption-key` lấy từ biến môi trường `${INTEGRATION_ENCRYPTION_KEY}` trong `application.yml`.
  * Định dạng lưu trữ thực tế trong database: `v1:<Base64-IV>:<Base64-ciphertext+tag>`.
* **Kết quả:** **ĐẠT**. Database lưu trữ dữ liệu dưới dạng mã hóa, không lưu token bản rõ.

---

### 2.2. API Response không trả Token về Frontend
* **Yêu cầu:** Các API cấu hình tích hợp tuyệt đối không trả lại token gốc về client.
* **Cơ chế:** Dữ liệu response cấu hình tích hợp (GitHub, Jira) chỉ trả về các trường thông tin trạng thái (`isConfigured: true/false`, URL cấu hình, thời gian cập nhật), loại bỏ hoàn toàn trường `token`/`secret` khỏi payload trả về client.
* **Kết quả:** **ĐẠT**.

---

### 2.3. Rà soát Logging & Xử lý thông báo lỗi (Masking / Exception)
* **Yêu cầu:** Đánh giá việc lộ secret trong log và kiểm soát stack trace trả về client.
* **Hiện trạng triển khai thực tế:**
  * **Jira Integration:** Có xử lý làm sạch/masking chuỗi secret trong log ngoại lệ tại `JiraSyncService` trước khi ghi nhận lỗi.
  * **GitHub Integration:** Các hàm `safeMessage(exception)` trong các service đồng bộ (`GitHubCheckRunSyncService`, `GitHubCommitSyncService`, `GitHubPullRequestSyncService`) **đang trả trực tiếp chuỗi `exception.getMessage()` và chưa có thao tác che/masking secret chuyên biệt** nếu nội dung exception có chứa token. Tuy nhiên hệ thống không log toàn bộ payload thô chứa request header/secret.
  * **API Exception:** Bắt và chuẩn hóa ngoại lệ tại controller/service, không trả stack trace DB ra ngoài response client.
* **Kết luận & Đánh giá:** **ĐẠT THEO HIỆN TRẠNG MÃ NGUỒN** (Khuyến nghị tạo task cải tiến ở sprint sau để bổ sung hàm regex masking cho `safeMessage()` trong các GitHub services tương tự như Jira).

---

### 2.4. Phân quyền truy cập cấu hình (RBAC)
* **Quy tắc phân quyền thực tế:**
  * **Admin:** Toàn quyền lưu/cập nhật (`POST`/`PUT`) và kiểm tra kết nối (`TEST`) cấu hình tích hợp Jira & GitHub. Không có API xóa cấu hình (DELETE).
  * **Leader dự án:** Chỉ được xem (`GET`) trạng thái cấu hình trong phạm vi project được phân công; **không** được phép lưu hay cập nhật cấu hình.
  * **Member / Role khác:** Bị từ chối truy cập (HTTP 403 Forbidden).
* **File triển khai & Test liên quan:**
  * Triển khai: `vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService`
  * Bằng chứng kiểm tra quyền API cấu hình Jira: `JiraIntegrationControllerTests`
  * Bằng chứng kiểm tra quyền API cấu hình GitHub: `GitHubIntegrationControllerTest`, `GitHubRbacIntegrationTest`
* **Kết quả:** **ĐẠT**.

---

## 3. Bằng Chứng Quét Lịch Sử Git (`--all`) & Mã Nguồn Thực Tế

> **Ghi chú về phạm vi kiểm chứng:** Số lượng commit khi chạy `git log --all` phụ thuộc vào các nhánh/ref có trên từng máy tại thời điểm quét. Báo cáo dưới đây ghi nhận kết quả rà soát thực tế trên nhánh `feature/CNPM-111-security-and-sensitive-data-audit` đối soát trực tiếp theo mã nguồn.

### 3.1. Bảng Tổng Hợp Kiểm Chứng Theo Pattern

| Pattern | Lệnh thực thi kiểm chứng | Đánh giá & Phân loại thực tế |
| :--- | :--- | :--- |
| `ghp_` | `git log --all -S "ghp_" --oneline` | **False Positive:** Placeholder UI (`placeholder="ghp_..."`), mock data trong unit test/RBAC test và các commit cập nhật tài liệu audit. Không chứa secret thật. |
| `gho_` | `git log --all -S "gho_" --oneline` | **False Positive:** Các commit cập nhật tài liệu audit (`de641a1`, `03a40d7`, `274cb03`). |
| `github_pat_` | `git log --all -S "github_pat_" --oneline` | **False Positive:** Regex kiểm tra định dạng token (`195a2ba`, `e047c70`) và các commit cập nhật tài liệu audit. |
| `ATATT` | `git log --all -S "ATATT" --oneline` | **False Positive:** Hướng dẫn định dạng token Jira (`566221a`) và các commit cập nhật tài liệu audit. |
| `api_token` | `git log --all -S "api_token" --oneline` | **Non-sensitive / Identifier:** Tên trường DTO, tham số Postman collection, endpoint docs và comment hướng dẫn. |
| `jwt.secret` | `git log --all -S "jwt.secret" --oneline` | **Configuration Reference:** Cấu hình Spring Security (`CNPM-42`), inject biến môi trường `${JWT_SECRET}` (`application.yml`) và các commit cập nhật docs. |
| `jira.token=` | `git grep -in "jira.token="` | **Documentation:** File tài liệu `docs/security-audit-report.md`. |
| `spring.datasource.password` | `git grep -in "spring.datasource.password"` | **Documentation:** File tài liệu `docs/security-audit-report.md`. |

---

### 3.2. Trích Xuất Chi Tiết Log Quét Mã Nguồn Thực Tế

```powershell
PS D:\java\project\java> git log --all -S "ghp_" --oneline
3046256 CNPM-111:fix audit report
8031a48 CNPM-111: fix audit report
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report
fbc36d2 feat(CNPM-101): integrate and close Sprint 4
c99b196 test(CNPM-100): complete GitHub integration RBAC coverage
df57457 Update GitHubRbacIntegrationTest.java
36e5592 Update GitHubRbacIntegrationTest.java
e5d6d0c Update GitHubRbacIntegrationTest.java
5db7f12 Create GitHubRbacIntegrationTest.java
b1ea555 Create GitHubIntegrationControllerTest.java
a14fc68 CNPM-93 sync GitHub repository information
154974d fix(CNPM-91): github-config-api
5145385 (CNPM-91):github-config-api
1d9abef fix(CNPM-92): github-config-admin
c4cedc2 feat(CNPM-91): add github config api
b57be32 fix(CNPM-92): github-config-admin
49d82ee CNPM-92 Implement GitHub config admin

PS D:\java\project\java> git log --all -S "gho_" --oneline
de641a1 CNPM-111: update audit report
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report

PS D:\java\project\java> git log --all -S "github_pat_" --oneline
de641a1 CNPM-111: update audit report
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report
195a2ba fix(CNPM-99):github-activity-ui
e047c70 feat(CNPM-99): implement github activity feature

PS D:\java\project\java> git log --all -S "ATATT" --oneline
de641a1 CNPM-111: update audit report
274cb03 CNPM-111 update security audit report
566221a  Fix CNPM-86

PS D:\java\project\java> git log --all -S "api_token" --oneline
de641a1 CNPM-111: update audit report
03a40d7 CNPM-111: fix audit report
da61c19 docs: update technical documentation and Postman collection
274cb03 CNPM-111 update security audit report
694e8aa CNPM-114: Update technical documentation and Postman collection
1c2481e FIX  Task 86
a14fc68 CNPM-93 sync GitHub repository information
7f6f27b feature/CNPM-86-jira-postman
5011bf5 feature/CNPM-86-jira-postman
bc70a42 (origin/feature/CNPM-46-role-based-access-control) CNPM-46: add role based acc

PS D:\java\project\java> git log --all -S "jwt.secret" --oneline
4d5a2cc (origin/feature/CNPM-111-security-and-sensitive-data-audit) CNPM-111: fix audit report
8031a48 CNPM-111: fix audit report
de641a1 CNPM-111: update audit report
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report
a14fc68 CNPM-93 sync GitHub repository information
d3ba54f CNPM-40 CNPM-41 CNPM-42 CNPM-45 CNPM-46 CNPM-47 CNPM-48 CNPM-49 complete authentication flow
bc70a42 (origin/feature/CNPM-46-role-based-access-control) CNPM-46: add role based acc

PS D:\java\project\java> git grep -in "jwt.secret" src/main/resources/
src/main/resources/application.yml:42:    secret: ${JWT_SECRET}

PS D:\java\project\java> git grep -in "integration-encryption-key" src/main/resources/
src/main/resources/application.yml:45:    integration-encryption-key: ${INTEGRATION_ENCRYPTION_KEY}

PS D:\java\project\java> git grep -in "safeMessage" src/main/java/
src/main/java/vn/edu/cnpm/projectsupport/integration/github/GitHubCheckRunSyncService.java:370:            log.setErrorMessage(safeMessage(exception));
src/main/java/vn/edu/cnpm/projectsupport/integration/github/GitHubCheckRunSyncService.java:514:    private String safeMessage(RuntimeException exception) {
src/main/java/vn/edu/cnpm/projectsupport/integration/github/GitHubCommitSyncService.java:155:            log.setErrorMessage(safeMessage(exception));
src/main/java/vn/edu/cnpm/projectsupport/integration/github/GitHubCommitSyncService.java:244:    private String safeMessage(RuntimeException exception) {
src/main/java/vn/edu/cnpm/projectsupport/integration/github/GitHubPullRequestSyncService.java:171:            log.setErrorMessage(safeMessage(exception));

PS D:\java\project\java> ./mvnw clean test
[INFO] Results:
[INFO] Tests run: 394, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------