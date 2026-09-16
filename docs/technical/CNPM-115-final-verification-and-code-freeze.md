# CNPM-115 — Tổng kiểm thử phiên bản cuối và đóng băng code

## 1. Phạm vi phiên bản

- Nhánh chuẩn bị đóng băng: `feature/CNPM-115-118-final-verification`.
- Base đã rà soát: `origin/main` tại commit `467fa44dbd34017ff6a7c46d2ea36daa87dbcba5`.
- JDK: Eclipse Temurin 21.
- Frontend: Node.js 22 trở lên theo `frontend/package.json`.
- Cơ sở dữ liệu production: MySQL; bộ test tự động dùng H2 ở chế độ tương thích MySQL.

Commit cuối của nhánh sau khi toàn bộ test đạt là release candidate. Sau thời điểm đó chỉ
nhận bản sửa lỗi chặn hoặc nghiêm trọng, phải có người điều phối (MT) duyệt và phải chạy lại
toàn bộ checklist trong tài liệu này.

## 2. Kết quả kiểm thử bắt buộc

Kết quả chạy ngày 16/09/2026 trên nhánh release candidate:

- Backend: `417` test, `0` failure, `0` error, `0` skipped; `BUILD SUCCESS`.
- Frontend: `16/16` test suite và `113/113` test pass.
- Frontend production build: biên dịch thành công.
- Flyway: áp dụng đủ 15 migration lên schema test rỗng.
- Sprint API mới: 2 integration test pass, bao gồm dữ liệu và RBAC.

| Hạng mục | Cách kiểm tra | Kết quả mong đợi |
| --- | --- | --- |
| Backend | `./mvnw clean verify` | Build thành công, không có test fail/error |
| Frontend test | `npm test -- --runInBand` trong `frontend` | Tất cả test suite pass |
| Frontend build | `npm run build` trong `frontend` | Tạo production build thành công |
| Migration mới | `DemoDatabaseSeedIntegrationTest` và full backend suite | Flyway áp dụng đủ migration trên schema rỗng |
| Sprint API | `SprintApiIntegrationTest` | Leader, Lecturer và Member của project đọc được Sprint; người ngoài bị chặn |
| Jira–GitHub–Report | `JiraGitHubReportIntegrationTest` | Đồng bộ, liên kết, báo cáo, idempotency và partial failure pass |
| RBAC | Toàn bộ security/integration tests | Vai trò chỉ truy cập đúng project/phạm vi |
| Secret | Quét file được Git theo dõi và lịch sử trước release | Không có token, mật khẩu hoặc private key thật |

Lưu ý: kiểm thử H2/MySQL mode không thay thế bước smoke test migration trên một schema MySQL
mới của môi trường demo. Trước khi trình bày, N tạo schema demo mới, khởi động backend để
Flyway migrate và xác nhận ứng dụng đăng nhập được.

## 3. Lỗi chặn đã xử lý

- Giao diện tiến độ gọi `GET /api/v1/projects/{projectId}/sprints` nhưng backend trước đó
  chưa có endpoint tương ứng.
- Đã bổ sung Sprint API trả về dữ liệu theo đúng project và áp dụng RBAC giống báo cáo.
- Đã bổ sung integration test để ngăn lỗi endpoint 404 và rò rỉ Sprint giữa các project tái diễn.

## 4. Checklist đóng băng

- [x] Backend full suite pass trên release candidate (`417/417`).
- [x] Frontend test (`113/113`) và production build pass trên release candidate.
- [x] Migration tự động chạy được từ schema test rỗng.
- [x] Luồng Jira–GitHub–Report có integration test cho happy path, idempotency và partial failure.
- [x] Không phát hiện secret thật trong cây mã nguồn hiện tại.
- [x] Không còn lỗi chặn đã biết trong contract frontend–backend của màn hình tiến độ.
- [x] Các vấn đề còn lại được ghi tại mục 5.
- [ ] N xác nhận smoke test trên schema MySQL mới của máy demo.
- [ ] MT ghi commit SHA cuối cùng và xác nhận đóng băng trên Jira/PR.

## 5. Vấn đề còn lại sau đóng băng

Các mục dưới đây không làm test/build thất bại nhưng phải được theo dõi:

- `npm audit` còn các dependency mức high/moderate/low; không có mức critical tại thời điểm
  rà soát. Không chạy `npm audit fix --force` sát ngày demo vì có thể gây breaking change.
- Một số frontend test có cảnh báo React về state update chưa được bọc trong `act(...)`.
  Cảnh báo không làm test fail nhưng nên sửa ở sprint bảo trì tiếp theo.
- Kết nối Jira/GitHub thật phụ thuộc mạng và token của môi trường demo; phải chuẩn bị dữ liệu
  đã đồng bộ và kịch bản dự phòng trong tài liệu CNPM-118.

## 6. Quy tắc sau khi freeze

1. Mỗi bản sửa phải gắn Jira task và mô tả lỗi có thể tái hiện.
2. MT duyệt trước khi merge.
3. Không thêm chức năng mới hoặc thay đổi migration ngoài phạm vi lỗi chặn.
4. Chạy lại backend verify, frontend test/build và smoke test đúng luồng bị sửa.
5. Nếu bất kỳ test nào fail, hủy bản sửa khỏi release candidate và ghi vào Done/Not Done.
