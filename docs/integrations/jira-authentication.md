# Nghiên cứu Cơ chế Xác thực Jira Cloud REST API

## 1. Mục tiêu
Nghiên cứu cơ chế xác thực của Jira Cloud REST API để làm cơ sở cho việc tích hợp Jira vào hệ thống.

## 2. Thông tin cần thiết để xác thực
Để thực hiện xác thực với Jira Cloud REST API, bạn cần chuẩn bị 3 thông tin cơ bản sau:
* **Jira Site URL:** Đường dẫn gốc (Base URL) tới không gian làm việc Jira của tổ chức.
  * *Ví dụ:* `https://<ten-to-chuc>.atlassian.net`
* **Tài khoản email:** Địa chỉ email dùng để truy cập vào Jira Cloud.
  * *Ví dụ:* `nguyenvana@example.edu.vn`
* **API Token:** Kể từ năm 2019, Atlassian yêu cầu sử dụng API Token thay cho mật khẩu tài khoản thông thường khi gọi các dịch vụ REST API nhằm tăng cường bảo mật.

## 3. Cơ chế Basic Authentication và Các Header cần thiết
Jira Cloud REST API sử dụng cơ chế **Basic Authentication**. Chuỗi xác thực được tạo ra bằng cách kết hợp địa chỉ email và API token theo định dạng `email:api_token`, sau đó chuỗi này được mã hóa Base64.

**Các header bắt buộc cần thiết khi gửi request:**
* `Authorization`: `Basic <chuỗi_base64_của_email:api_token>` (Các công cụ như Postman thường tự động sinh ra chuỗi này khi chọn Basic Auth).
* `Accept`: `application/json` (Cho hệ thống biết request muốn nhận dữ liệu trả về theo định dạng JSON).
* `Content-Type`: `application/json` (Bắt buộc đối với các request POST/PUT chứa body data).

## 4. Hướng dẫn tạo và sử dụng API token
**Cách tạo API Token:**
1. Đăng nhập vào tài khoản Atlassian trên trình duyệt web.
2. Truy cập vào trang quản trị bảo mật của người dùng: [https://id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens).
3. Chọn nút **Create API token**.
4. Nhập nhãn (Label) cho token để dễ quản lý (VD: `Jira-Integration-App`) và nhấn **Create**.
5. Nhấn **Copy** để lưu lại mã token. *(Lưu ý quan trọng: Mã token này chỉ được hiển thị một lần duy nhất, do đó bạn cần lưu trữ ngay vào nơi an toàn)*.

**Cách sử dụng:** Truyền API Token này vào vị trí của **Password** khi cấu hình xác thực Basic Auth.

## 5. Ví dụ Request và Response xác thực (PoC)
Dưới đây là ví dụ sử dụng endpoint lấy thông tin người dùng (`GET /rest/api/3/myself`) để kiểm tra trạng thái xác thực. *(Lưu ý: Mọi thông tin nhạy cảm đã được che hoặc thay thế).*

**Ví dụ gửi request bằng cURL:**
```bash
curl --request GET   --url 'https://your-domain.atlassian.net/rest/api/3/myself'   --user 'email@example.com:<API_TOKEN_HERE>'   --header 'Accept: application/json'
```

**Ví dụ cấu hình kiểm tra trên Postman:**
* **URL:** `https://your-domain.atlassian.net/rest/api/3/myself`
* **Method:** `GET`
* **Authorization Tab:** 
  * **Auth Type:** `Basic Auth`
  * **Username:** `email@example.com`
  * **Password:** `<API_TOKEN_HERE>`

**Kết quả response mẫu (HTTP 200 OK) chứng minh xác thực thành công:**
```json
{
  "self": "https://your-domain.atlassian.net/rest/api/3/user?accountId=123...",
  "accountId": "1234567890abcdef12345678",
  "emailAddress": "email@example.com",
  "displayName": "Nguyen Van A",
  "active": true,
  "timeZone": "Asia/Ho_Chi_Minh",
  "locale": "en_US"
}
```

## 6. Các lỗi xác thực thường gặp và cách xử lý
* **Lỗi 401 Unauthorized:** 
  * *Nguyên nhân:* Nhập sai email (ví dụ: thiếu domain tổ chức), dùng mật khẩu đăng nhập thông thường thay vì API token, hoặc cấu hình sai Auth Type trên Postman (để dạng Inherit thay vì Basic Auth).
  * *Cách xử lý:* Kiểm tra lại kỹ cấu trúc email, tạo và sử dụng API Token mới nhất, và đảm bảo đã chọn chính xác Basic Auth.
* **Lỗi 403 Forbidden:**
  * *Nguyên nhân:* Xác thực thành công (API token hợp lệ) nhưng tài khoản email không được cấp quyền truy cập vào Resource/Project cụ thể trong request.
  * *Cách xử lý:* Liên hệ quản trị viên (Admin) của Jira site để kiểm tra và cấp quyền (Permissions/Roles).
* **Lỗi 404 Not Found:**
  * *Nguyên nhân:* URL Jira site không tồn tại, hoặc nhập sai đường dẫn endpoint API.
  * *Cách xử lý:* Kiểm tra lại cấu trúc Base URL và API Endpoint trong tài liệu của Jira.

## 7. Cách lưu trữ và bảo vệ API token
API token có quyền truy cập tương đương với tài khoản thật của bạn, vì vậy cần áp dụng các biện pháp bảo mật nghiêm ngặt:
1. **Tuyệt đối không Hard-code:** KHÔNG BAO GIỜ đưa trực tiếp API token hoặc mật khẩu vào source code, tài liệu Markdown (.md) hay đẩy lên repository (ví dụ: GitHub, GitLab).
2. **Sử dụng Biến môi trường (Environment Variables):** Lưu trữ token trong file cấu hình `.env` ở local (như `JIRA_API_TOKEN=your_token`). Cần đảm bảo file `.env` đã được liệt kê trong `.gitignore`.
3. **Sử dụng Secret Management Tool:** Đối với môi trường triển khai CI/CD, nên lưu trữ token vào các hệ thống quản lý bí mật chuyên dụng như GitHub Secrets, AWS Secrets Manager, hoặc HashiCorp Vault.
4. **Xử lý khi lộ token (Revoke):** Nếu bạn nghi ngờ token bị rò rỉ, hãy đăng nhập ngay vào tài khoản Atlassian, tìm token đó và nhấn **Revoke** để hủy hiệu lực, sau đó khởi tạo một token mới thay thế.
