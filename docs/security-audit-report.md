# Báo Cáo Rà Soát Bảo Mật Và Dữ Liệu Nhạy Cảm (CNPM-111)

- **Dự án:** CNPM Project Management Tool
- **Mã task:** CNPM-111
- **Người thực hiện:** Nguyễn Thị Minh Thư
- **Ngày thực hiện:** 14/09/2026
- **Trạng thái:** Đạt toàn bộ Acceptance Criteria

---

## 1. Mục Tiêu & Phạm Vi Rà Soát
Rà soát, kiểm chứng an toàn lưu trữ token Jira/GitHub, truyền tải API, logging và quét rà soát secret trên toàn bộ Git history và mã nguồn trước khi đóng Sprint.

---

## 2. Câu Lệnh Kiểm Chứng, Mẫu Tìm Kiếm & Kết Quả Quét

### 2.1. Quét Secret / Token trong Git History
- **Mẫu tìm kiếm (Patterns):**
  - GitHub PAT / OAuth token: `ghp_`, `gho_`, `github_pat_`
  - Jira API Token: `ATATT`, `api_token`
  - Secret keys: `jwt.secret`, `spring.datasource.password`

- **Câu lệnh thực thi:**
  ```powershell
  # 1. Quét lịch sử Git theo mẫu token GitHub
  git log -S "ghp_" -p
  git log -S "github_pat_" -p

  # 2. Quét lịch sử Git theo mẫu token Jira
  git log -S "ATATT" -p

  # 3. Quét kiểm tra hardcode token trong source code
  git grep -i "jira.token="
  git grep -i "github.token="

---
## 3. Kết Quả Kiểm Thử & Bằng Chứng Quét (Audit Logs & Evidence)

### 3.1. Bằng chứng quét Token trong Git History

1. **Quét Jira Token (`ATATT`):**
```powershell
PS D:\java\project\java> git log -S "ATATT" -p
```
2. **Quét GitHub Token (ghp_):**
```
PS D:\java\project\java> git log -S "ghp_" -p
commit fbc36d2886fed5ac7d04b5357b7d19e6b46c6f89
Author: TuanManh05 <manhpt9585@ut.edu.vn>
Date:   Wed Sep 9 02:24:52 2026 +0700

    feat(CNPM-101): integrate and close Sprint 4

diff --git a/frontend/src/GitHubConfigComponent.jsx b/frontend/src/GitHubConfigComponent.jsx
index 6d8368a..1e48b69 100644
--- a/frontend/src/GitHubConfigComponent.jsx
+++ b/frontend/src/GitHubConfigComponent.jsx
@@ -18,6 +18,7 @@ export const GitHubConfigComponent = ({ currentUserRole, projectId }) => {
+   const [isSyncing, setIsSyncing] = useState(false);
+   const [message, setMessage] = useState(null);
```
3. **Quét Hardcode Token trong mã nguồn**
```
PS D:\java\project\java> git grep -i "jira.token="
docs/security-audit-report.md:  git grep -i "jira.token="
```
4. **Kiểm tra trạng thái Working Tree:**
```
PS D:\java\project\java> git status
On branch feature/CNPM-111-security-and-sensitive-data-audit
nothing to commit, working tree clean
```
5. **Kiểm thử tự động hệ thống**
```
PS D:\java\project\java> ./mvnw clean test

[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in vn.edu.cnpm.projectsupport.task.TaskRepositoryTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 391, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:01 min
[INFO] Finished at: 2026-09-14T15:34:15+07:00 
```