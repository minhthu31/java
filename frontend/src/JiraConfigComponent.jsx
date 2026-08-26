import React, { useState, useEffect } from "react";
import { JiraService } from "./JiraService";
import { currentUser } from "./authService";

export default function JiraConfigComponent() {
    const user = currentUser() || {};
    const userRole = user.role
        ? String(user.role).replace("ROLE_", "").toUpperCase()
        : null;

    const isAdmin = userRole === "ADMIN";

    const [formData, setFormData] = useState({
        baseUrl: "",
        accountIdentifier: "",
        apiToken: "",
        projectKey: "",
    });

    const [connectionStatus, setConnectionStatus] = useState("NOT_CHECKED"); // NOT_CHECKED | CONNECTED | FAILED
    const [lastTestedAt, setLastTestedAt] = useState(null);
    const [testing, setTesting] = useState(false);
    const [saving, setSaving] = useState(false);
    const [pageLoading, setPageLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);
    const [troubleshootingTip, setTroubleshootingTip] = useState(null);

    useEffect(() => {
        if (!isAdmin) return;

        const loadConfig = async () => {
            setPageLoading(true);
            try {
                const res = await JiraService.getConfig();
                if (res) {
                    setFormData({
                        baseUrl: res.baseUrl || "",
                        accountIdentifier: res.accountIdentifier || "",
                        apiToken: "", // Token bảo mật: không tự động tải lại từ server
                        projectKey: res.projectKey || "",
                    });
                    if (res.connectionStatus) {
                        setConnectionStatus(res.connectionStatus);
                    }
                    if (res.lastTestedAt) {
                        setLastTestedAt(res.lastTestedAt);
                    }
                }
            } catch (err) {
                setErrorMessage(
                    err.response?.data?.message ||
                        "Không thể tải cấu hình Jira từ hệ thống.",
                );
            } finally {
                setPageLoading(false);
            }
        };

        loadConfig();
    }, [isAdmin]);

    if (!isAdmin) {
        return (
            <div
                data-testid="unauthorized-message"
                style={{
                    padding: "24px",
                    background: "#fee2e2",
                    color: "#991b1b",
                    borderRadius: "8px",
                    border: "1px solid #f87171",
                    margin: "20px",
                }}
            >
                <h3 style={{ margin: "0 0 8px 0" }}>Từ chối truy cập</h3>
                <p style={{ margin: 0 }}>
                    Bạn không có quyền truy cập màn hình cấu hình Jira. Màn hình
                    này chỉ dành cho Quản trị viên (ADMIN).
                </p>
            </div>
        );
    }

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const validateForm = () => {
        if (!formData.baseUrl.trim()) return "Vui lòng nhập Jira Base URL.";
        if (!formData.accountIdentifier.trim())
            return "Vui lòng nhập Account Identifier (Email/Username).";
        if (!formData.projectKey.trim()) return "Vui lòng nhập Project Key.";
        return null;
    };

    const handleTestConnection = async () => {
        setErrorMessage(null);
        setSuccessMessage(null);
        setTroubleshootingTip(null);

        const valErr = validateForm();
        if (valErr) {
            setErrorMessage(valErr);
            return;
        }

        setTesting(true);
        try {
            const res = await JiraService.testConnection(formData);
            setConnectionStatus("CONNECTED");
            const now = new Date().toISOString();
            setLastTestedAt(now);
            setSuccessMessage(
                res?.message || "Kết nối đến máy chủ Jira thành công!",
            );
        } catch (err) {
            setConnectionStatus("FAILED");
            setLastTestedAt(new Date().toISOString());
            const status = err.response?.status;
            const serverMsg =
                err.response?.data?.message ||
                err.message ||
                "Kết nối Jira thất bại.";
            setErrorMessage(serverMsg);

            if (status === 401) {
                setTroubleshootingTip(
                    "Hướng xử lý: Kiểm tra lại Account Identifier và API Token Jira. Hãy chắc chắn API Token vẫn còn hiệu lực.",
                );
            } else if (status === 404) {
                setTroubleshootingTip(
                    "Hướng xử lý: Kiểm tra lại Jira Base URL hoặc Project Key có tồn tại trên hệ thống Jira hay không.",
                );
            } else {
                setTroubleshootingTip(
                    "Hướng xử lý: Kiểm tra kết nối mạng, firewall hoặc kiểm tra quyền cấp API trong cài đặt Atlassian.",
                );
            }
        } finally {
            setTesting(false);
        }
    };

    const handleSaveConfig = async (e) => {
        e.preventDefault();
        setErrorMessage(null);
        setSuccessMessage(null);
        setTroubleshootingTip(null);

        const valErr = validateForm();
        if (valErr) {
            setErrorMessage(valErr);
            return;
        }

        setSaving(true);
        try {
            await JiraService.saveConfig(formData);
            setSuccessMessage("Lưu cấu hình Jira thành công!");
        } catch (err) {
            setErrorMessage(
                err.response?.data?.message ||
                    err.message ||
                    "Có lỗi xảy ra khi lưu cấu hình Jira.",
            );
        } finally {
            setSaving(false);
        }
    };

    const renderStatusBadge = () => {
        switch (connectionStatus) {
            case "CONNECTED":
                return (
                    <span
                        data-testid="status-badge"
                        style={{
                            padding: "6px 12px",
                            borderRadius: "6px",
                            background: "#dcfce7",
                            color: "#15803d",
                            fontWeight: "bold",
                            border: "1px solid #86efac",
                            fontSize: "13px",
                        }}
                    >
                        ● Connected
                    </span>
                );
            case "FAILED":
                return (
                    <span
                        data-testid="status-badge"
                        style={{
                            padding: "6px 12px",
                            borderRadius: "6px",
                            background: "#fee2e2",
                            color: "#b91c1c",
                            fontWeight: "bold",
                            border: "1px solid #fca5a5",
                            fontSize: "13px",
                        }}
                    >
                        ✕ Failed
                    </span>
                );
            default:
                return (
                    <span
                        data-testid="status-badge"
                        style={{
                            padding: "6px 12px",
                            borderRadius: "6px",
                            background: "#f1f5f9",
                            color: "#64748b",
                            fontWeight: "bold",
                            border: "1px solid #cbd5e1",
                            fontSize: "13px",
                        }}
                    >
                        ○ Not Checked
                    </span>
                );
        }
    };

    const formatTimestamp = (ts) => {
        if (!ts) return "Chưa từng kiểm tra";
        try {
            return new Date(ts).toLocaleString("vi-VN");
        } catch {
            return ts;
        }
    };

    return (
        <div
            style={{
                maxWidth: "750px",
                margin: "24px auto",
                padding: "28px",
                background: "#ffffff",
                borderRadius: "8px",
                border: "1px solid #e2e8f0",
                boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
            }}
        >
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "24px",
                    paddingBottom: "16px",
                    borderBottom: "1px solid #e2e8f0",
                }}
            >
                <div>
                    <h2
                        style={{
                            margin: "0 0 4px 0",
                            fontSize: "20px",
                            fontWeight: "bold",
                            color: "#0f172a",
                        }}
                    >
                        Cấu hình tích hợp Jira
                    </h2>
                    <p
                        style={{
                            margin: 0,
                            fontSize: "13px",
                            color: "#64748b",
                        }}
                    >
                        Quản lý thông tin kết nối và kiểm tra tính sẵn sàng của
                        hệ thống Jira
                    </p>
                </div>
                <div>{renderStatusBadge()}</div>
            </div>

            <div
                data-testid="last-tested-info"
                style={{
                    fontSize: "12px",
                    color: "#64748b",
                    marginBottom: "16px",
                    display: "flex",
                    justifyContent: "flex-end",
                }}
            >
                Thời điểm kiểm tra gần nhất: {formatTimestamp(lastTestedAt)}
            </div>

            {successMessage && (
                <div
                    data-testid="success-message"
                    style={{
                        padding: "12px 16px",
                        background: "#dcfce7",
                        color: "#15803d",
                        border: "1px solid #86efac",
                        borderRadius: "6px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    ✓ {successMessage}
                </div>
            )}

            {errorMessage && (
                <div
                    data-testid="error-message"
                    style={{
                        padding: "12px 16px",
                        background: "#fee2e2",
                        color: "#991b1b",
                        border: "1px solid #f87171",
                        borderRadius: "6px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    ⚠️ {errorMessage}
                </div>
            )}

            {troubleshootingTip && (
                <div
                    data-testid="troubleshooting-tip"
                    style={{
                        padding: "12px 16px",
                        background: "#fffbeb",
                        color: "#92400e",
                        border: "1px solid #fcd34d",
                        borderRadius: "6px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    💡 {troubleshootingTip}
                </div>
            )}

            {pageLoading ? (
                <div
                    data-testid="page-loading"
                    style={{
                        textAlign: "center",
                        padding: "40px",
                        color: "#64748b",
                    }}
                >
                    Đang tải cấu hình Jira...
                </div>
            ) : (
                <form
                    onSubmit={handleSaveConfig}
                    style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "16px",
                    }}
                >
                    <div>
                        <label
                            htmlFor="jira-base-url"
                            style={{
                                display: "block",
                                fontWeight: 600,
                                fontSize: "13px",
                                marginBottom: "6px",
                                color: "#334155",
                            }}
                        >
                            Jira Base URL *
                        </label>
                        <input
                            id="jira-base-url"
                            type="url"
                            name="baseUrl"
                            value={formData.baseUrl}
                            onChange={handleInputChange}
                            placeholder="https://your-domain.atlassian.net"
                            style={{
                                width: "100%",
                                padding: "10px 12px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="jira-account-id"
                            style={{
                                display: "block",
                                fontWeight: 600,
                                fontSize: "13px",
                                marginBottom: "6px",
                                color: "#334155",
                            }}
                        >
                            Account Identifier (Email / Username) *
                        </label>
                        <input
                            id="jira-account-id"
                            type="text"
                            name="accountIdentifier"
                            value={formData.accountIdentifier}
                            onChange={handleInputChange}
                            placeholder="admin@example.com"
                            style={{
                                width: "100%",
                                padding: "10px 12px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>

                    <div>
                        <label
                            htmlFor="jira-api-token"
                            style={{
                                display: "block",
                                fontWeight: 600,
                                fontSize: "13px",
                                marginBottom: "6px",
                                color: "#334155",
                            }}
                        >
                            Jira API Token (Secret)
                        </label>
                        <input
                            id="jira-api-token"
                            type="password"
                            name="apiToken"
                            value={formData.apiToken}
                            onChange={handleInputChange}
                            placeholder="Nhập API token mới nếu muốn thay đổi..."
                            autoComplete="new-password"
                            style={{
                                width: "100%",
                                padding: "10px 12px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                        <span
                            style={{
                                fontSize: "11px",
                                color: "#64748b",
                                marginTop: "4px",
                                display: "block",
                            }}
                        >
                            * Token luôn được che dưới dạng mật khẩu và không tự
                            động tải lại từ server vì lý do bảo mật.
                        </span>
                    </div>

                    <div>
                        <label
                            htmlFor="jira-project-key"
                            style={{
                                display: "block",
                                fontWeight: 600,
                                fontSize: "13px",
                                marginBottom: "6px",
                                color: "#334155",
                            }}
                        >
                            Jira Project Key *
                        </label>
                        <input
                            id="jira-project-key"
                            type="text"
                            name="projectKey"
                            value={formData.projectKey}
                            onChange={handleInputChange}
                            placeholder="Ví dụ: CNPM, PROJ"
                            style={{
                                width: "100%",
                                padding: "10px 12px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                            }}
                        />
                    </div>

                    <div
                        style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            gap: "12px",
                            marginTop: "16px",
                            paddingTop: "16px",
                            borderTop: "1px solid #f1f5f9",
                        }}
                    >
                        <button
                            type="button"
                            data-testid="test-connection-btn"
                            disabled={testing || saving}
                            onClick={handleTestConnection}
                            style={{
                                padding: "10px 20px",
                                background: "#0284c7",
                                color: "#ffffff",
                                border: "none",
                                borderRadius: "6px",
                                fontWeight: 600,
                                fontSize: "13px",
                                cursor:
                                    testing || saving
                                        ? "not-allowed"
                                        : "pointer",
                                opacity: testing || saving ? 0.7 : 1,
                            }}
                        >
                            {testing
                                ? "Đang kiểm tra kết nối..."
                                : "Test Connection"}
                        </button>

                        <button
                            type="submit"
                            data-testid="save-config-btn"
                            disabled={testing || saving}
                            style={{
                                padding: "10px 24px",
                                background: "#15803d",
                                color: "#ffffff",
                                border: "none",
                                borderRadius: "6px",
                                fontWeight: 600,
                                fontSize: "13px",
                                cursor:
                                    testing || saving
                                        ? "not-allowed"
                                        : "pointer",
                                opacity: testing || saving ? 0.7 : 1,
                            }}
                        >
                            {saving ? "Đang lưu..." : "Save"}
                        </button>
                    </div>
                </form>
            )}
        </div>
    );
}
