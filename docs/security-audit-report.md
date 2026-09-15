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
  * Tên key cấu hình: `app.security.integration-encryption-key` (lấy từ biến môi trường `${INTEGRATION_ENCRYPTION_KEY}`).
  * Định dạng lưu trữ thực tế trong database: `v1::<ciphertext+tag>`.
* **Kết quả:** **ĐẠT**. Dữ liệu trong database được mã hóa trước khi lưu, không lưu token dạng bản rõ.

---

### 2.2. API Response không trả Token về Frontend
* **Yêu cầu:** Các API trả về thông tin cấu hình tích hợp tuyệt đối không trả lại token gốc về client.
* **Cơ chế:** Dữ liệu phản hồi cấu hình tích hợp (GitHub, Jira) chỉ chứa các trường thông tin chung (`isConfigured: true/false`, URL kết nối, thời gian cập nhật), loại bỏ hoàn toàn trường `token`/`secret` khỏi payload phản hồi gửi về client.
* **Kết quả:** **ĐẠT**.

---

### 2.3. Rà soát Logging & Không lộ Secret (Masking)
* **Yêu cầu:** Không ghi log các header xác thực (`Authorization`, `Cookie`, `x-api-key`) hoặc body chứa secret khi giao tiếp nội bộ và bên thứ ba.
* **Cơ chế thực tế:** Xử lý làm sạch/che giấu (masking) cục bộ tại các vị trí tiếp nhận và xử lý token (`JiraSyncService`, GitHub services) trước khi ghi log hoặc debug.
* **Kết quả:** **ĐẠT**.

---

### 2.4. Thông báo lỗi & Xử lý ngoại lệ (Exception Handling)
* **Yêu cầu:** Không trả stack trace, câu lệnh SQL hoặc chi tiết cấu trúc bảng DB ra response khi xảy ra lỗi (4xx, 5xx).
* **Cơ chế triển khai:** Các exception từ tích hợp Jira/GitHub được bắt và xử lý tại tầng service/controller trước khi trả response, đảm bảo thông tin lỗi trả về client được chuẩn hóa và loại bỏ hoàn toàn chi tiết nhạy cảm.
* **Kết quả:** **ĐẠT**.

---

### 2.5. Phân quyền truy cập cấu hình (RBAC)
* **Quy tắc phân quyền thực tế:**
  * **Admin (System/Workspace Admin):** Toàn quyền lưu/cập nhật (`POST`/`PUT`) và kiểm tra kết nối (`TEST`) cấu hình tích hợp Jira & GitHub.
  * **Leader dự án:** Chỉ được quyền xem (`GET`) trạng thái cấu hình trong phạm vi project được phân công; **không** được phép thêm/sửa cấu hình tích hợp.
  * **Member / Developer / Role khác:** Bị từ chối truy cập toàn bộ (trả về HTTP 403 Forbidden).
* **File triển khai & kiểm thử:**
  * Triển khai: `vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService`
  * Annotation `@PreAuthorize` trên các endpoint cấu hình tích hợp.
  * Test kiểm chứng: `GitHubRbacIntegrationTest.java`, `GitHubIntegrationControllerTest.java`, `JiraSyncRbacControllerTest.java`
* **Kết quả:** **ĐẠT**.

---

## 3. Bằng Chứng Quét Lịch Sử Git (`--all`) & Mã Nguồn Thực Tế

### 3.1. Bảng Tổng Hợp Lệnh Quét & Kết Quả Thực Tế

| Pattern | Lệnh thực thi kiểm chứng | Kết quả khớp | Phân loại & Giải thích thực tế |
| :--- | :--- | :---: | :--- |
| `ghp_` | `git log --all -S "ghp_" --oneline` | **20 commits** | **False Positive:** Placeholder UI (`placeholder="ghp_..."`), mock data trong unit test/RBAC test và tài liệu markdown. Không chứa secret thật. |
| `gho_` | `git log --all -S "gho_" --oneline` | **3 commits** | **False Positive:** Comment giải thích định dạng token và các commit cập nhật tài liệu audit. |
| `github_pat_` | `git log --all -S "github_pat_" --oneline` | **5 commits** | **False Positive:** Regex kiểm tra định dạng token và các commit cập nhật tài liệu audit. |
| `ATATT` | `git log --all -S "ATATT" --oneline` | **3 commits** | **False Positive:** Hướng dẫn định dạng token Jira trong markdown và các commit cập nhật tài liệu audit. |
| `api_token` | `git log --all -S "api_token" --oneline` | **12 commits** | **Non-sensitive / Identifier:** Tên trường DTO, tham số Postman collection, endpoint docs và comment hướng dẫn. |
| `jwt.secret` | `git log --all -S "jwt.secret" --oneline` | **16 commits** | **Configuration Reference:** Cấu hình Spring Security, code inject biến môi trường `${JWT_SECRET}` và mock key trong test. |
| `jira.token=` | `git grep -in "jira.token="` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |
| `spring.datasource.password` | `git grep -in "spring.datasource.password"` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |

---

### 3.2. Trích Xuất Chi Tiết Log & Phân Loại Kiểm Chứng

```powershell
PS D:\java\project\java> git log --all -S "ghp_" --oneline
3046256 (HEAD -> feature/CNPM-111-security-and-sensitive-data-audit, origin/feature/CNPM-111-security-and-sensitive-data-audit) CNPM-111:fix audit report
de641a1 CNPM-111: update audit report
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
src/main/resources/application.yml:42:    secret: ${JWT_SECRET}

PS D:\java\project\java> git grep -in "integration-encryption-key"
src/main/resources/application.yml:45:    integration-encryption-key: ${INTEGRATION_ENCRYPTION_KEY}
src/main/java/vn/edu/cnpm/projectsupport/security/AesGcmIntegrationSecretService.java:24:            @Value("${app.security.integration-encryption-key}") String encryptionKey)

PS D:\java\project\java> ./mvnw clean test
[INFO] Results:
[INFO] Tests run: 394, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------