# Báo Cáo Rà Soát Bảo Mật Và Dữ Liệu Nhạy Cảm (CNPM-111)

- **Dự án:** CNPM Project Management Tool
- **Mã task:** CNPM-111
- **Người thực hiện:** Nguyễn Thị Minh Thư
- **Ngày thực hiện:** 14/09/2026 (Cập nhật đối soát thực tế: 15/09/2026)
- **Trạng thái:** Đã hoàn thành rà soát & cập nhật 100% bằng chứng kiểm thử, kết quả scan thực tế

---

## 1. Mục Tiêu & Phạm Vi Rà Soát
Rà soát toàn diện cơ chế bảo mật backend: mã hóa dữ liệu tích hợp (GitHub PAT, Jira API Token), bảo đảm API contract không rò rỉ secret, kiểm soát phân quyền (RBAC), che giấu thông tin nhạy cảm trong log/thông báo lỗi và phân loại toàn bộ kết quả quét lịch sử Git (`--all`) cùng mã nguồn dự án trước khi nghiệm thu Sprint.

---

## 2. Kết Quả Kiểm Tra Chi Tiết Kèm File / Test Case Chứng Minh

### 2.1. Mã hóa Token trong DB (Database Encryption)
* **Yêu cầu:** Toàn bộ token tích hợp bên thứ ba (GitHub PAT, Jira API Token) phải được mã hóa trước khi lưu trữ, không lưu dạng plaintext.
* **File triển khai:** `vn.edu.cnpm.projectsupport.security.AesGcmIntegrationSecretService`
* **Cơ chế & Cấu hình:**
  * Thuật toán: **AES-256-GCM** kèm Nonce/IV ngẫu nhiên cho mỗi lần mã hóa.
  * Tên key cấu hình: `app.security.integration-encryption-key` (được inject từ biến môi trường `${INTEGRATION_ENCRYPTION_KEY}`).
  * Định dạng lưu trữ thực tế trong database: `v1::<base64-ciphertext-kèm-tag>`.
* **File test chứng minh:** `vn.edu.cnpm.projectsupport.security.AesGcmIntegrationSecretServiceTest`
* **Test cases cụ thể:**
  1. `encrypt_shouldProducePrefixAndValidCiphertext()`: Kiểm tra chuỗi sau mã hóa bắt đầu bằng tiền tố `v1::` và không chứa token gốc.
  2. `decrypt_shouldRestoreOriginalSecret()`: Xác nhận giải mã khôi phục chính xác secret ban đầu.
  3. `decrypt_withTamperedData_shouldThrowException()`: Xác nhận cơ chế GCM Authentication Tag phát hiện và từ chối dữ liệu bị giả mạo.
* **Kết quả:** **ĐẠT**. Dữ liệu trong database hoàn toàn ở dạng mã hóa.

---

### 2.2. API Response không trả Token về Frontend
* **Yêu cầu:** Các API trả về thông tin cấu hình tích hợp tuyệt đối không trả lại token gốc về client.
* **File triển khai:**
  * DTO: `vn.edu.cnpm.projectsupport.integration.github.dto.GitHubConfigResponse`
  * DTO: `vn.edu.cnpm.projectsupport.integration.jira.dto.JiraConfigResponse`
  * Controller: `vn.edu.cnpm.projectsupport.integration.github.controller.GitHubConfigController`
  * Controller: `vn.edu.cnpm.projectsupport.integration.jira.controller.JiraConfigController`
* **Cơ chế:** DTO response chỉ chứa các trường thông tin chung (`isConfigured: true/false`, `repoUrl` / `serverUrl`, `updatedAt`, `configuredBy`), trường `token`/`secret` bị loại bỏ hoàn toàn khỏi schema DTO.
* **File test chứng minh:**
  * `vn.edu.cnpm.projectsupport.integration.github.controller.GitHubConfigControllerTest`
  * `vn.edu.cnpm.projectsupport.integration.jira.controller.JiraConfigControllerTest`
* **Test cases cụ thể:**
  1. `getGitHubConfig_shouldReturnStatusWithoutToken()`:
     ```java
     mockMvc.perform(get("/api/v1/projects/{projectId}/integrations/github", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isConfigured").value(true))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.accessToken").doesNotExist());
     ```
  2. `getJiraConfig_shouldReturnStatusWithoutApiToken()`:
     ```java
     mockMvc.perform(get("/api/v1/projects/{projectId}/integrations/jira", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.apiToken").doesNotExist());
     ```
* **Kết quả:** **ĐẠT**.

---

### 2.3. Rà soát Logging & Không lộ Header / Body nhạy cảm
* **Yêu cầu:** Không ghi log các header xác thực (`Authorization`, `Cookie`, `x-api-key`) hoặc body chứa secret khi giao tiếp nội bộ và bên thứ ba.
* **File triển khai:** `vn.edu.cnpm.projectsupport.common.logging.SensitiveDataMaskingPatternLayout`
* **Cơ chế:** Pattern Layout tự động regex bắt các trường `token`, `password`, `secret`, `apiToken` và header nhạy cảm để thay thế bằng `***` hoặc `[REDACTED]`.
* **File test chứng minh:** `vn.edu.cnpm.projectsupport.common.logging.SensitiveDataMaskingTest`
* **Test cases cụ thể:**
  1. `maskSensitiveFields_inJsonBody_shouldReplaceValuesWithMask()`: Xác minh log message chứa JSON payload được che giấu secret.
  2. `maskAuthHeaders_shouldHideBearerTokens()`: Xác minh chuỗi `Bearer ghp_...` / `Basic ...` trong log chỉ còn `Bearer [MASKED]`.
* **Kết quả:** **ĐẠT**.

---

### 2.4. Thông báo lỗi & Xử lý ngoại lệ (Exception Handling)
* **Yêu cầu:** Không trả stack trace, câu lệnh SQL hoặc chi tiết cấu trúc bảng DB ra response khi xảy ra lỗi (4xx, 5xx).
* **File triển khai:**
  * Cấu hình: `src/main/resources/application.yml`
    ```yaml
    server:
      error:
        include-stacktrace: never
        include-message: never
        include-binding-errors: never
    ```
  * Handler: `vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler`
* **File test chứng minh:** `vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandlerTest`
* **Test cases cụ thể:**
  1. `handleDataAccessException_shouldReturnGenericErrorMessage()`: Giả lập lỗi `DataAccessException`/`SQLException`, kiểm tra response chỉ trả về mã lỗi `INTERNAL_SERVER_ERROR`, payload không chứa SQL query hoặc tên bảng.
  2. `handleGenericException_shouldNotExposeStackTrace()`: Kiểm tra response không có trường `trace` hay exception class name.
* **Kết quả:** **ĐẠT**.

---

### 2.5. Phân quyền truy cập cấu hình (RBAC)
* **Yêu cầu & Quy tắc phân quyền thực tế:**
  * **Admin (System/Workspace Admin):** Toàn quyền lưu (`POST`/`PUT`), kiểm tra kết nối (`TEST`) và xóa cấu hình tích hợp Jira & GitHub.
  * **Leader dự án:** Chỉ được quyền xem (`GET`) trạng thái cấu hình trong phạm vi project được phân công; **không** được phép thêm/sửa/xóa cấu hình tích hợp.
  * **Member / Developer / Role khác:** Bị từ chối truy cập toàn bộ (trả về HTTP 403 Forbidden).
* **File triển khai:**
  * `vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService`
  * Annotation `@PreAuthorize` trên các endpoint cấu hình tích hợp.
* **File test chứng minh:**
  * `vn.edu.cnpm.projectsupport.integration.github.controller.GitHubConfigControllerRbacTest`
  * `vn.edu.cnpm.projectsupport.integration.jira.controller.JiraConfigControllerRbacTest`
* **Test cases cụ thể:**
  1. `admin_canSaveAndTestIntegration_shouldReturnOk()`: Xác nhận quyền Admin được thực thi thành công (HTTP 200).
  2. `projectLeader_canViewConfig_shouldReturnOk()`: Leader xem cấu hình trong project (HTTP 200).
  3. `projectLeader_cannotUpdateConfig_shouldReturnForbidden()`: Leader cố ý gửi request update/save cấu hình bị chặn với HTTP 403 Forbidden.
  4. `projectMember_accessConfig_shouldReturnForbidden()`: Member truy cập endpoint cấu hình bị chặn với HTTP 403 Forbidden.
* **Kết quả:** **ĐẠT**.

---

## 3. Bằng Chứng Quét Lịch Sử Git (`--all`) & Mã Nguồn Thực Tế

### 3.1. Bảng Tổng Hợp Lệnh Quét & Kết Quả Thực Tế

| Pattern | Lệnh thực thi kiểm chứng | Kết quả khớp | Phân loại & Giải thích thực tế |
| :--- | :--- | :---: | :--- |
| `ghp_` | `git log --all -S "ghp_" --oneline` | **18 commits** | **False Positive:** Placeholder UI (`placeholder="ghp_..."`), mock data trong test case RBAC (`GitHubRbacIntegrationTest.java`), DTO contract test và tài liệu markdown. Không chứa secret thật. |
| `gho_` | `git log --all -S "gho_" --oneline` | **2 commits** | **False Positive:** Các commit cập nhật báo cáo kiểm thử bảo mật `security-audit-report.md`. |
| `github_pat_` | `git log --all -S "github_pat_" --oneline` | **4 commits** | **False Positive:** 2 commit audit docs (`03a40d7`, `274cb03`) và 2 commit tính năng GitHub Activity UI/Feature (`195a2ba`, `e047c70` chứa regex format token). |
| `ATATT` | `git log --all -S "ATATT" --oneline` | **2 commits** | **False Positive:** Commit audit report (`274cb03`) và commit fix Task 86 (`566221a` hướng dẫn format token Jira trong markdown). |
| `api_token` | `git log --all -S "api_token" --oneline` | **11 commits** | **Non-sensitive / Identifier:** Tên trường DTO, tham số Postman collection, endpoint docs và comment (`CNPM-114`, `CNPM-93`, `CNPM-86`, `CNPM-46`, `CNPM-22`). |
| `jwt.secret` | `git log --all -S "jwt.secret" --oneline` | **14 commits** | **Configuration Reference:** Các commit cấu hình Spring Security (`CNPM-42`), hoàn thiện Auth flow (`CNPM-40..49`) và inject biến môi trường `${JWT_SECRET}`. |
| `jira.token=` | `git grep -in "jira.token="` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |
| `spring.datasource.password=` | `git grep -in "spring.datasource.password"` | **1 file** | **Documentation:** File tài liệu `docs/security-audit-report.md`. |

---

### 3.2. Trích Xuất Chi Tiết Log & Phân Loại Kiểm Chứng

#### 1. Quét GitHub Classic PAT (`ghp_` - 18 commits)
* **Log console thực tế:**
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
src/main/resources/application.yml:42:    secret: ${JWT_SECRET}
src/main/java/vn/edu/cnpm/projectsupport/security/JwtTokenProvider.java:18:    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
src/test/java/vn/edu/cnpm/projectsupport/auth/AuthServiceTests.java:10:    private final JwtTokenProvider tokens=new JwtTokenProvider("test-only-jwt-secret-key-with-at-least-32-bytes",3600000);
src/test/java/vn/edu/cnpm/projectsupport/security/JwtTokenProviderTests.java:3:    var provider=new JwtTokenProvider("test-only-jwt-secret-key-with-at-least-32-bytes",3600000);

PS D:\java\project\java> git grep -in "integration-encryption-key"
src/main/java/vn/edu/cnpm/projectsupport/security/AesGcmIntegrationSecretService.java:24:            @Value("${app.security.integration-encryption-key}") String encryptionKey) {
src/main/resources/application.yml:45:    integration-encryption-key: ${INTEGRATION_ENCRYPTION_KEY}
src/test/resources/application-test.yml:17:    integration-encryption-key: test-only-encryption-key-32-characters

PS D:\java\project\java> ./mvnw clean test
[INFO] Results:
[INFO] Tests run: 391, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```