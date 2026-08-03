# Jira Cloud API Integration - Issue Creation PoC

Repository này chứa mã nguồn, tài liệu hướng dẫn và báo cáo thử nghiệm (Proof of Concept - PoC) việc tạo Issue tự động trên project **CNPM** sử dụng Jira Cloud REST API v3.

## 📌 Thông tin Task
* **Task Key:** `CNPM-24`
* **Mục tiêu:** Nghiên cứu và thực thi API tạo Issue qua Postman/Script, đảm bảo tính an toàn bảo mật cho API Token.

---

## 🛠 Cấu trúc Repository

```text
.
├── images/                  # Chứa ảnh chụp minh chứng (Postman response, Jira Web)
│   ├── postman_response.png
│   └── jira_issue.png
├── .gitignore               # Cấu hình chặn commit file nhạy cảm (.env, credentials)
├── REPORT.md                # Báo cáo chi tiết Kết quả bàn giao & Acceptance Criteria
└── README.md                # Trang tổng quan dự án
