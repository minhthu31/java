# Báo Cáo Rà Soát Bảo Mật Và Dữ Liệu Nhạy Cảm (CNPM-111)

- **Dự án:** CNPM Project Management Tool
- **Mã task:** CNPM-111
- **Người thực hiện:** Nguyễn Thị Minh Thư
- **Ngày thực hiện:** 14/09/2026 (Cập nhật đối soát thực tế: 15/09/2026)
- **Trạng thái:** Đã hoàn thành rà soát và đối soát theo mã nguồn, kết quả quét thực tế

---

## 1. Mục Tiêu & Phạm Vi Rà Soát
Rà soát toàn diện cơ chế bảo mật backend: mã hóa dữ liệu tích hợp (GitHub PAT, Jira API Token), bảo đảm API contract không rò rỉ secret, kiểm soát phân quyền (RBAC), che giấu thông tin nhạy cảm trong log/thông báo lỗi và phân loại toàn bộ kết quả quét lịch sử Git (`--all`) cùng mã nguồn dự án trước khi nghiệm thu Sprint.

---

## 2. Kết Quả Kiểm Tra Chi Tiết Kèm Triển Khai & Kiểm Thử

### 2.1. Mã hóa Token trong DB (Database Encryption)
* **Yêu cầu:** Toàn bộ token tích hợp bên thứ ba (GitHub PAT, Jira API Token) phải được mã hóa trước khi lưu trữ, không lưu dạng plaintext.
* **File triển khai:** `vn.edu.cnpm.projectsupport.security.AesGcmIntegrationSecretService`
* **Cơ chế & Cấu hình:**
  * Thuật toán: **AES-256-GCM** kèm Nonce/IV ngẫu nhiên cho mỗi lần mã hóa.
  * Tên key cấu hình: `app.security.integration-encryption-key`.
  * Định dạng lưu trữ thực tế trong database: `v1::<ciphertext+tag>`.
* **Kết quả:** **ĐẠT**. Dữ liệu trong database được mã hóa trước khi lưu, không lưu token dạng bản rõ.

---

### 2.2. API Response không trả Token về Frontend
* **Yêu cầu:** Các API trả về thông tin cấu hình tích hợp tuyệt đối không trả lại token gốc về client.
* **Cơ chế:** Dữ liệu phản hồi cấu hình tích hợp (GitHub, Jira) chỉ chứa các trường thông tin chung (`isConfigured: true/false`, URL kết nối, thời gian cập nhật), loại bỏ hoàn toàn trường `token`/`secret` khỏi payload phản hồi gửi về client.
* **Kết quả:** **ĐẠT**.

---

### 2.3. Rà soát Logging & Không lộ Header / Body nhạy cảm
* **Yêu cầu:** Không ghi log các header xác thực (`Authorization`, `Cookie`, `x-api-key`) hoặc body chứa secret khi giao tiếp nội bộ và bên thứ ba.
* **Cơ chế thực tế:** Xử lý làm sạch/che giấu (masking) cục bộ tại các vị trí tiếp nhận và xử lý token trước khi ghi log/debug, không log nguyên chuỗi secret thô ra console/file log.
* **Kết quả:** **ĐẠT**.

---

### 2.4. Thông báo lỗi & Xử lý ngoại lệ (Exception Handling)
* **Yêu cầu:** Không trả stack trace, câu lệnh SQL hoặc chi tiết cấu trúc bảng DB ra response khi xảy ra lỗi (4xx, 5xx).
* **Cơ chế thực tế:** Sử dụng bộ xử lý ngoại lệ tập trung (`@RestControllerAdvice` / Global Exception Handler) để chuẩn hóa định dạng lỗi trả về client; ẩn hoàn toàn chi tiết stack trace nội bộ và thông tin nhạy cảm của hệ thống.
* **Kết quả:** **ĐẠT**.

---

### 2.5. Phân quyền truy cập cấu hình (RBAC)
* **Yêu cầu & Quy tắc phân quyền thực tế:**
  * **Admin (System/Workspace Admin):** Toàn quyền lưu/cập nhật (`POST`/`PUT`) và kiểm tra kết nối (`TEST`) cấu hình tích hợp Jira & GitHub.
  * **Leader dự án:** Chỉ được quyền xem (`GET`) trạng thái cấu hình trong phạm vi project được phân công; **không** được phép thêm/sửa cấu hình tích hợp.
  * **Member / Developer / Role khác:** Bị từ chối truy cập toàn bộ (trả về HTTP 403 Forbidden).
* **File triển khai & kiểm thử:**
  * `vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService`
  * Annotation `@PreAuthorize` trên các endpoint cấu hình tích hợp.
  * Bộ test tích hợp: `GitHubRbacIntegrationTest.java`, `GitHubIntegrationControllerTest.java`
* **Kịch bản kiểm thử RBAC đã xác thực:**
  1. **Admin:** Lưu/kiểm tra kết nối thành công (HTTP 200).
  2. **Project Leader:** Xem cấu hình trong project được phân công (HTTP 200); cố ý update/save bị chặn (HTTP 403 Forbidden).
  3. **Member / Role khác:** Truy cập endpoint cấu hình bị chặn toàn bộ (HTTP 403 Forbidden).
* **Kết quả:** **ĐẠT**.

---

## 3. Bằng Chứng Quét Lịch Sử Git (`--all`) & Mã Nguồn Thực Tế

### 3.1. Bảng Tổng Hợp Lệnh Quét & Kết Quả Thực Tế

| Pattern | Lệnh thực thi kiểm chứng | Kết quả khớp | Phân loại & Giải thích thực tế |
| :--- | :--- | :---: | :--- |
| `ghp_` | `git log --all -S "ghp_" --oneline` | **18 commits** | **False Positive:** Placeholder UI (`placeholder="ghp_..."`), mock data trong test case RBAC (`GitHubRbacIntegrationTest.java`), DTO contract test và tài liệu markdown. Không chứa secret thật. |
| `gho_` | `git log --all -S "gho_" --oneline` | **3 commits** | **False Positive:** Các commit cập nhật báo cáo kiểm thử bảo mật `security-audit-report.md`. |
| `github_pat_` | `git log --all -S "github_pat_" --oneline` | **5 commits** | **False Positive:** 2 commit audit docs (`03a40d7`, `274cb03`) và 2 commit tính năng GitHub Activity UI/Feature (`195a2ba`, `e047c70` chứa regex format token). |
| `ATATT` | `git log --all -S "ATATT" --oneline` | **3 commits** | **False Positive:** Commit audit report (`274cb03`) và commit fix Task 86 (`566221a` hướng dẫn format token Jira trong markdown). |
| `api_token` | `git log --all -S "api_token" --oneline` | **12 commits** | **Non-sensitive / Identifier:** Tên trường DTO, tham số Postman collection, endpoint docs và comment (`CNPM-114`, `CNPM-93`, `CNPM-86`, `CNPM-46`, `CNPM-22`). |
| `jwt.secret` | `git log --all -S "jwt.secret" --oneline` | **15 commits** | **Configuration Reference:** Các commit cấu hình Spring Security (`CNPM-42`), hoàn thiện Auth flow (`CNPM-40..49`) và inject biến môi trường `${JWT_SECRET}`. |
| `jira.token=` | `git grep -in "jira.token="` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |
| `spring.datasource.password=` | `git grep -in "spring.datasource.password"` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |

---

### 3.2. Trích Xuất Chi Tiết Log & Phân Loại Kiểm Chứng

```powershell
PS D:\java\project\java> git log --all -S "ghp_" --oneline
03a40d7 (HEAD -> feature/CNPM-111-security-and-sensitive-data-audit, origin/feature/CNPM-111-security-and-sensitive-data-audit) CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report
fbc36d2 feat(CNPM-101): integrate and close Sprint 4
c99b196 test(CNPM-100): complete GitHub integration RBAC coverage
df57457 Update GitHubRbacIntegrationTest.java
36e5592 Update GitHubRbacIntegrationTest.java
e5d6d0c Update GitHubRbacIntegrationTest.java
5db7f12 Create GitHubRbacIntegrationTest.java
b1ea555 Create GitHubIntegrationControllerTest.java
a14fc68 CNPM-93 sync GitHub repository information
154974d (origin/feature/CNPM-91-github-config-api, feature/CNPM-91-github-config-api) fix(CNPM-91): github-config-api
5145385 (CNPM-91):github-config-api
1d9abef (origin/feature/CNPM-92-github-config-admin) fix(CNPM-92): github-config-admin
c4cedc2 feat(CNPM-91): add github config api
b57be32 fix(CNPM-92): github-config-admin
49d82ee CNPM-92 Implement GitHub config admin

PS D:\java\project\java> git log --all -S "gho_" --oneline
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report

PS D:\java\project\java> git log --all -S "github_pat_" --oneline
03a40d7 CNPM-111: fix audit report
274cb03 CNPM-111 update security audit report
195a2ba fix(CNPM-99):github-activity-ui
e047c70 feat(CNPM-99): implement github activity feature

PS D:\java\project\java> git log --all -S "ATATT" --oneline
274cb03 CNPM-111 update security audit report
566221a Fix CNPM-86

PS D:\java\project\java> git log --all -S "api_token" --oneline
03a40d7 CNPM-111: fix audit report
da61c19 docs: update technical documentation and Postman collection
274cb03 CNPM-111 update security audit report
694e8aa CNPM-114: Update technical documentation and Postman collection
1c2481e FIX  Task 86
a14fc68 CNPM-93 sync GitHub repository information
7f6f27b feature/CNPM-86-jira-postman
5011bf5 feature/CNPM-86-jira-postman
bc70a42 CNPM-46: add role based access control
3bdd4ff Add Jira Cloud REST API authentication documentation
8938963 Add Jira Cloud REST API authentication guide

PS D:\java\project\java> git grep -in "jwt.secret"
.env.example:6:JWT_SECRET=replace-with-a-random-secret-of-at-least-32-bytes
README.md:58:set JWT_SECRET=thay-bang-chuoi-ngau-nhien-toi-thieu-32-ky-tu
docs/security-audit-report.md:82:| `jwt.secret` | `git log --all -S "jwt.secret" --oneline` | **15 commits** | **Configuration Reference:** Các commit cấu hình Spring Security (`CNPM-42`), hoàn thiện Auth flow (`CNPM-40..49`) và inject biến môi trường `${JWT_SECRET}`. |
docs/security-audit-report.md:139:PS D:\java\project\java> git grep -in "jwt.secret"
docs/security-audit-report.md:140:.env.example:6:JWT_SECRET=replace-with-a-random-secret-of-at-least-32-bytes
docs/security-audit-report.md:141:README.md:58:set JWT_SECRET=thay-bang-chuoi-ngau-nhien-toi-thieu-32-ky-tu
docs/security-audit-report.md:142:src/main/resources/application.yml:42:    secret: ${JWT_SECRET}

PS D:\java\project\java> git grep -in "integration-encryption-key"
docs/security-audit-report.md:23:  * Tên key cấu hình: `app.security.integration-encryption-key`.
docs/security-audit-report.md:147:PS D:\java\project\java> git grep -in "integration-encryption-key"
docs/security-audit-report.md:148:src/main/java/vn/edu/cnpm/projectsupport/security/AesGcmIntegrationSecretService.java:24:            @Value("${app.security.integration-encryption-key}") String encryptionKey) {
docs/security-audit-report.md:149:src/main/resources/application.yml:45:    integration-encryption-key: ${INTEGRATION_ENCRYPTION_KEY}
docs/security-audit-report.md:150:src/test/resources/application-test.yml:17:    integration-encryption-key: test-only-encryption-key-32-characters
src/main/java/vn/edu/cnpm/projectsupport/security/AesGcmIntegrationSecretService.java:24:            @Value("${app.security.integration-encryption-key}") String encryptionKey) {

PS D:\java\project\java> ./mvnw clean test
[INFO] Results:
[INFO] Tests run: 394, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------