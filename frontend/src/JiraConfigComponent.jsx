import React, { useState, useEffect } from "react";
import { JiraIntegrationService } from "./JiraIntegrationService";

const JiraConfigComponent = ({ projectId, role: propRole }) => {
    const getStoredRole = () => {
        try {
            const user = JSON.parse(
                localStorage.getItem("user") ||
                    sessionStorage.getItem("user") ||
                    "{}",
            );
            return user.role;
        } catch {
            return null;
        }
    };

    const rawRole =
        propRole ||
        localStorage.getItem("role") ||
        localStorage.getItem("userRole") ||
        getStoredRole();
    const normalizedRole = rawRole
        ? String(rawRole).replace("ROLE_", "").toUpperCase()
        : "";
    const isAdmin = normalizedRole === "ADMIN";
    const canView = isAdmin || normalizedRole === "TEAM_LEADER";

    const [formData, setFormData] = useState({
        siteUrl: "",
        email: "",
        projectKey: "",
        apiToken: "",
        authType: "API_TOKEN",
    });
    const [status, setStatus] = useState("NOT_CONFIGURED");
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState("");

    useEffect(() => {
        if (!canView || !projectId) return;

        let isMounted = true;
        const fetchConfig = async () => {
            setLoading(true);
            try {
                const res =
                    await JiraIntegrationService.getConnection(projectId);
                const data = res?.data !== undefined ? res.data : res;
                if (data && isMounted) {
                    setFormData((prev) => ({
                        ...prev,
                        siteUrl: data.siteUrl || "",
                        email: data.email || "",
                        projectKey: data.projectKey || "",
                        apiToken: data.apiToken || "",
                        authType: data.authType || "API_TOKEN",
                    }));

                    if (
                        data.lastTestSucceeded === true ||
                        data.connected === true
                    ) {
                        setStatus("CONNECTED");
                    } else if (
                        data.lastTestSucceeded === false ||
                        data.connected === false
                    ) {
                        setStatus("FAILED");
                    } else {
                        setStatus("NOT_CONFIGURED");
                    }
                }
            } catch (err) {
                if (isMounted) setStatus("NOT_CONFIGURED");
            } finally {
                if (isMounted) setLoading(false);
            }
        };

        fetchConfig();
        return () => {
            isMounted = false;
        };
    }, [projectId, canView]);

    if (!canView) {
        return (
            <div
                data-testid="unauthorized-message"
                style={{
                    padding: "32px",
                    textAlign: "center",
                    color: "#de350b",
                }}
            >
                Bạn không có quyền truy cập cấu hình này.
            </div>
        );
    }

    if (!projectId) {
        return (
            <div
                style={{
                    padding: "32px",
                    textAlign: "center",
                    color: "#6b778c",
                }}
            >
                Vui lòng chọn dự án để cấu hình Jira.
            </div>
        );
    }

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSave = async (e) => {
        e.preventDefault();
        if (!isAdmin) return;
        setLoading(true);
        setMessage("");
        try {
            const payload = {
                siteUrl: formData.siteUrl,
                email: formData.email,
                projectKey: formData.projectKey,
                apiToken: formData.apiToken,
                authType: "API_TOKEN",
            };
            await JiraIntegrationService.configureConnection(
                projectId,
                payload,
            );
            setMessage("Lưu cấu hình Jira thành công!");
        } catch (err) {
            setMessage("Lưu cấu hình thất bại!");
        } finally {
            setLoading(false);
        }
    };

    const handleTestConnection = async () => {
        setLoading(true);
        setMessage("");
        try {
            const res = await JiraIntegrationService.testConnection(projectId);
            const data = res?.data !== undefined ? res.data : res;
            if (data?.lastTestSucceeded === true || data?.connected === true) {
                setStatus("CONNECTED");
                setMessage("Kết nối Jira thành công!");
            } else {
                setStatus("FAILED");
                setMessage("Kết nối Jira thất bại!");
            }
        } catch (err) {
            setStatus("FAILED");
            setMessage("Không thể kiểm tra kết nối!");
        } finally {
            setLoading(false);
        }
    };

    const badgeStyle = {
        display: "inline-block",
        padding: "3px 8px",
        borderRadius: "4px",
        fontSize: "12px",
        fontWeight: 700,
        backgroundColor:
            status === "CONNECTED"
                ? "#e3fcef"
                : status === "FAILED"
                  ? "#ffebe6"
                  : "#ebecf0",
        color:
            status === "CONNECTED"
                ? "#006644"
                : status === "FAILED"
                  ? "#de350b"
                  : "#42526e",
    };

    const inputStyle = {
        width: "100%",
        padding: "10px 12px",
        borderRadius: "4px",
        border: "1px solid #dfe1e6",
        fontSize: "14px",
        marginTop: "6px",
        boxSizing: "border-box",
        outline: "none",
        backgroundColor: isAdmin ? "#fafbfc" : "#f4f5f7",
    };

    return (
        <div
            style={{
                maxWidth: "600px",
                margin: "0 auto",
                padding: "36px 24px",
            }}
        >
            <h2
                style={{
                    fontSize: "20px",
                    fontWeight: 700,
                    color: "#172b4d",
                    margin: "0 0 16px",
                }}
            >
                Cấu hình Jira Integration
            </h2>

            <div
                style={{
                    marginBottom: "20px",
                    fontSize: "14px",
                    color: "#5e6c84",
                }}
            >
                Trạng thái kết nối:{" "}
                <span data-testid="connection-status-badge" style={badgeStyle}>
                    {status}
                </span>
            </div>

            {message && (
                <div
                    style={{
                        padding: "12px 16px",
                        borderRadius: "4px",
                        marginBottom: "20px",
                        fontSize: "14px",
                        backgroundColor:
                            message.includes("thất bại") ||
                            message.includes("Lỗi") ||
                            message.includes("Không thể")
                                ? "#ffebe6"
                                : "#deebff",
                        color:
                            message.includes("thất bại") ||
                            message.includes("Lỗi") ||
                            message.includes("Không thể")
                                ? "#de350b"
                                : "#0747a6",
                    }}
                >
                    {message}
                </div>
            )}

            <form
                onSubmit={handleSave}
                style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "18px",
                }}
            >
                <div>
                    <label
                        htmlFor="siteUrl"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#344563",
                        }}
                    >
                        Jira Base URL{" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="siteUrl"
                        name="siteUrl"
                        type="url"
                        placeholder="https://your-domain.atlassian.net"
                        value={formData.siteUrl}
                        onChange={handleChange}
                        disabled={!isAdmin}
                        required
                        style={inputStyle}
                    />
                </div>

                <div>
                    <label
                        htmlFor="email"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#344563",
                        }}
                    >
                        Email tài khoản{" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="email"
                        name="email"
                        type="email"
                        placeholder="name@company.com"
                        value={formData.email}
                        onChange={handleChange}
                        disabled={!isAdmin}
                        required
                        style={inputStyle}
                    />
                </div>

                <div>
                    <label
                        htmlFor="projectKey"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#344563",
                        }}
                    >
                        Project Key <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="projectKey"
                        name="projectKey"
                        type="text"
                        placeholder="VD: PROJ, CNPM"
                        value={formData.projectKey}
                        onChange={handleChange}
                        disabled={!isAdmin}
                        required
                        style={inputStyle}
                    />
                </div>

                <div>
                    <label
                        htmlFor="apiToken"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#344563",
                        }}
                    >
                        API Token <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="apiToken"
                        name="apiToken"
                        type="password"
                        placeholder="Nhập Atlassian API Token..."
                        value={formData.apiToken}
                        onChange={handleChange}
                        disabled={!isAdmin}
                        required
                        style={inputStyle}
                    />
                </div>

                <div
                    style={{ display: "flex", gap: "12px", marginTop: "12px" }}
                >
                    {isAdmin && (
                        <button
                            type="submit"
                            data-testid="save-config-btn"
                            disabled={loading}
                            style={{
                                backgroundColor: "#0052cc",
                                color: "#fff",
                                padding: "10px 20px",
                                borderRadius: "4px",
                                border: "none",
                                fontWeight: 600,
                                fontSize: "14px",
                                cursor: loading ? "not-allowed" : "pointer",
                                opacity: loading ? 0.6 : 1,
                            }}
                        >
                            Lưu cấu hình
                        </button>
                    )}
                    <button
                        type="button"
                        data-testid="test-connection-btn"
                        onClick={handleTestConnection}
                        disabled={loading}
                        style={{
                            backgroundColor: "#f4f5f7",
                            color: "#42526e",
                            padding: "10px 20px",
                            borderRadius: "4px",
                            border: "1px solid #dfe1e6",
                            fontWeight: 600,
                            fontSize: "14px",
                            cursor: loading ? "not-allowed" : "pointer",
                            opacity: loading ? 0.6 : 1,
                        }}
                    >
                        Test Connection
                    </button>
                </div>
            </form>
        </div>
    );
};

export default JiraConfigComponent;
