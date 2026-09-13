# CNPM-112 - Báo cáo tích hợp và hoàn thiện giao diện Sprint 5

## 1. Mục tiêu

Hoàn tất việc tích hợp toàn bộ các màn hình frontend với API thật, chuẩn hóa contract theo đúng các phương thức HTTP (GET, POST, PUT), loại bỏ dữ liệu giả lập (mock data), che giấu thông tin nhạy cảm và bảo đảm chất lượng hệ thống qua bộ kiểm thử tự động cùng bản build production.

## 2. Phạm vi tích hợp

| Phân hệ / Màn hình         | Contract & Endpoint thực tế                                                                                                     |       Phương thức       | Kết quả tích hợp                                                                                                                          |
| :------------------------- | :------------------------------------------------------------------------------------------------------------------------------ | :---------------------: | :---------------------------------------------------------------------------------------------------------------------------------------- |
| **Cấu hình Jira**          | `/projects/{projectId}/integrations/jira/config`                                                                                |        GET, POST        | Dùng API thật; lấy và lưu cấu hình; ẩn `apiToken` qua `type="password"`; xử lý thông báo thành công/lỗi                                   |
| **Kiểm tra kết nối Jira**  | `/projects/{projectId}/integrations/jira/test-connection`                                                                       |          POST           | Gửi request test kết nối Jira, xử lý hiển thị `errorCode`                                                                                 |
| **Thông tin Jira Issue**   | `/projects/{projectId}/integrations/jira/issues/{issueKey}`                                                                     |           GET           | Lấy thông tin issue liên kết từ Jira                                                                                                      |
| **Đồng bộ Jira Task**      | `/projects/{projectId}/integrations/jira/tasks/{taskId}/sync`<br>`/projects/{projectId}/integrations/jira/tasks/{taskId}/retry` |          POST           | Dùng API thật; tự động cập nhật liên kết Jira Issue Key và hỗ trợ retry khi lỗi                                                           |
| **Cấu hình GitHub**        | `/projects/{projectId}/integrations/github/config`                                                                              |        GET, POST        | Dùng API thật; lấy và lưu cấu hình; ẩn `Personal Access Token (PAT)`                                                                      |
| **Kiểm tra & Sync GitHub** | `/projects/{projectId}/integrations/github/test-connection`<br>`/projects/{projectId}/integrations/github/sync`                 |          POST           | Dùng API thật; test kết nối token và kích hoạt đồng bộ commit/PR                                                                          |
| **Hoạt động GitHub**       | `/projects/{projectId}/integrations/github/activities`<br>`/projects/{projectId}/integrations/github/tasks/{taskId}/activities` |           GET           | Dùng API thật; hiển thị commit/PR liên kết mã Jira; có xử lý danh sách rỗng                                                               |
| **Quản lý Task**           | `/projects/{projectId}/tasks`<br>`/projects/{projectId}/tasks/{taskId}`<br>`/projects/{projectId}/tasks/{taskId}/status`        | GET, POST<br>GET<br>PUT | Dùng API thật; lấy danh sách, tạo mới, xem chi tiết và cập nhật trạng thái; có loading/error/empty state; sanitize chuỗi nhạy cảm trên UI |
| **Báo cáo đóng góp**       | `/projects/{projectId}/reports/progress`<br>`/projects/{projectId}/reports/summary`                                             |           GET           | Dùng API thật; có banner cảnh báo trạng thái đồng bộ; chống xung đột dữ liệu (race condition) khi lọc ngày                                |

Base tích hợp là commit `e4abc5b` của nhánh `origin/main`.

## 3. Bằng chứng kiểm thử kỹ thuật

### a. Rà soát Mock Data & Endpoint tĩnh

- **Lệnh kiểm tra:** `git grep -nEi 'mockData|dummyData|sampleData|fakeData|demoData' src/ ':!*.test.*'`
- **Kết quả:** Không tìm thấy dữ liệu giả lập trong luồng mã nguồn chính thức.
- **Base URL:** Toàn bộ service (`RequirementService`, `api.js`, `memberContributionService`, `progressService`) đều trỏ về biến môi trường `REACT_APP_API_BASE_URL` hoặc fallback chuẩn `http://localhost:8080/api/v1`.

### b. Kiểm tra bảo mật và thông tin nhạy cảm

- **Lệnh kiểm tra:** `git grep -i "password\|token\|secret" src/ ':!*.test.*'`
- **Kết quả:**
    - Trường PAT của GitHub (`#token-input`) và Token của Jira (`#jira-api-token`) đều được thiết lập `type="password"`, `autoComplete="new-password"`.
    - Không truyền token qua màn hình hiển thị; regex che chuỗi bí mật `Bearer`, `token=`, `secret_` tại chi tiết Task.
    - Quét `git grep -i "console.log" src/ ':!*.test.*'` trả về 0 kết quả (không rò rỉ token ra trình duyệt).

### c. Kiểm thử tự động (Unit & Integration Tests)

- **Lệnh thực thi:** `npm test -- --watchAll=false`
- **Kết quả:**
    - **Test Suites:** `15 passed, 15 total`
    - **Tests:** `111 passed, 111 total`
    - **Thời gian chạy:** `7.386 s`
    - Toàn bộ các suite: `RequirementForm`, `RequirementList`, `GitHubConfigComponent`, `GitHubActivityComponent`, `SrsPreview`, `ProjectProgressComponent`, `GitHubTaskActivityPanel`, `JiraConfigComponent`, `memberContributionService`, `JiraIntegrationService`, `GitHubActivityService`, `TaskService`, `MemberContributionComponent`, `Dashboard`, `TaskComponent` đều đạt trạng thái PASS.

### d. Kiểm thử đóng gói (Production Build)

- **Lệnh thực thi:** `npm run build`
- **Kết quả:** `Compiled successfully.`
    - Bundle JS: `95.87 kB` (sau gzip)
    - Bundle CSS: `1.21 kB` (sau gzip)
    - Thư mục `build/` đã sẵn sàng phục vụ triển khai server.

## 4. Checklist nghiệm thu (Acceptance Criteria)

| Tiêu chí nghiệm thu                                        | Trạng thái | Ghi chú                                                                 |
| :--------------------------------------------------------- | :--------: | :---------------------------------------------------------------------- |
| Các màn hình Jira, GitHub, Task và Report sử dụng API thật |  **Đạt**   | Kết nối trực tiếp qua các service REST API `/api/v1` chuẩn GET/POST/PUT |
| Không còn URL hoặc dữ liệu mock dùng trong luồng demo      |  **Đạt**   | Quét sạch mock data trong toàn bộ thư mục `src/`                        |
| Trạng thái loading, lỗi và không có dữ liệu được xử lý     |  **Đạt**   | Đầy đủ spinner, nút retry khi lỗi, empty placeholder và banner đồng bộ  |
| Không hiển thị token hoặc thông tin nhạy cảm               |  **Đạt**   | Input PAT/Token dùng `type="password"`, regex lọc chuỗi nhạy cảm        |
| Không có lỗi console nghiêm trọng trong luồng demo         |  **Đạt**   | `console.log` đã dọn sạch, không rò rỉ dữ liệu ra log                   |
| Frontend build thành công                                  |  **Đạt**   | `npm run build` tạo artifact production thành công                      |
| Frontend test hiện có đều pass                             |  **Đạt**   | 15/15 test suites pass (111/111 unit & integration tests)               |

## 5. Kết luận

Mã nguồn frontend đã hoàn thiện toàn diện các tiêu chí tích hợp và kiểm thử của Sprint 5. Nhánh `feature/CNPM-112-frontend-integration-and-polish` sẵn sàng tạo Pull Request để hợp nhất và chuyển trạng thái task sang **Done**.
