import React, { useState, useEffect, useCallback } from "react";
import { JiraIntegrationService } from "./JiraIntegrationService";

export default function JiraConfigComponent({ projectId, role: propRole }) {
    const rawRole =
        propRole !== undefined && propRole !== null && propRole !== ""
            ? propRole
            : localStorage.getItem("role") ||
              localStorage.getItem("userRole") ||
              "";
    const userRole = String(rawRole)
        .replace(/^ROLE_/, "")
        .toUpperCase();

    const isAdmin = userRole === "ADMIN";
    const isTeamLeader = userRole === "TEAM_LEADER";
    const canView = isAdmin || isTeamLeader;
    const canEdit = isAdmin;
    const canTest = isAdmin;

    const [submitting, setSubmitting] = useState(false);
    const [testing, setTesting] = useState(false);
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);

    const [config, setConfig] = useState({
        configured: false,
        lastTestSucceeded: null,
        lastTestedAt: null,
        syncStatus: null,
    });

    const [formData, setFormData] = useState({
        siteUrl: "",
        projectKey: "",
        email: "",
        apiToken: "",
        authType: "API_TOKEN",
    });

    const fetchConfig = useCallback(async () => {
        if (!projectId || !canView) return;

        try {
            const res = await JiraIntegrationService.getConnection(projectId);
            const data =
                res?.data !== undefined &&
                typeof res.data === "object" &&
                res.data !== null &&
                !Array.isArray(res.data)
                    ? res.data
                    : res;

            if (data) {
                const url =
                    data.siteUrl || data.jiraBaseUrl || data.baseUrl || "";

                const isConfigured =
                    data.configured !== undefined
                        ? Boolean(data.configured)
                        : Boolean(url);

                setConfig({
                    configured: isConfigured,
                    lastTestSucceeded:
                        data.lastTestSucceeded !== undefined
                            ? data.lastTestSucceeded
                            : null,
                    lastTestedAt: data.lastTestedAt || null,
                    syncStatus: data.syncStatus || null,
                });

                setFormData({
                    siteUrl: url,
                    projectKey: data.projectKey || "",
                    authType: data.authType || "API_TOKEN",
                    email: isConfigured && !canEdit ? "••••••••••••••••" : "",
                    apiToken:
                        isConfigured && !canEdit ? "••••••••••••••••" : "",
                });
                setError(null);
            }
        } catch (err) {
            setConfig({
                configured: false,
                lastTestSucceeded: null,
                lastTestedAt: null,
                syncStatus: null,
            });
            setError(null);
        }
    }, [projectId, canView, canEdit]);

    useEffect(() => {
        fetchConfig();
    }, [fetchConfig]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleSaveConfig = async (e) => {
        e.preventDefault();
        if (!canEdit || submitting) return;

        setSubmitting(true);
        setMessage(null);
        setError(null);

        const payload = {
            siteUrl: formData.siteUrl.trim(),
            projectKey: formData.projectKey.trim(),
            email: formData.email.trim(),
            authType: formData.authType || "API_TOKEN",
            apiToken: formData.apiToken.trim(),
        };

        try {
            await JiraIntegrationService.configureConnection(
                projectId,
                payload,
            );
            setMessage("Lưu cấu hình Jira thành công.");
            await fetchConfig();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                    err.message ||
                    "Lưu cấu hình thất bại.",
            );
        } finally {
            setSubmitting(false);
        }
    };

    const handleTestConnection = async () => {
        if (!canTest || testing) return;

        setTesting(true);
        setMessage(null);
        setError(null);

        try {
            const res = await JiraIntegrationService.testConnection(projectId);
            const data =
                res?.data !== undefined &&
                typeof res.data === "object" &&
                res.data !== null &&
                !Array.isArray(res.data)
                    ? res.data
                    : res;

            const isSuccess = data?.connected === true;
            const testedAt =
                data?.testedAt ||
                data?.lastTestedAt ||
                new Date().toISOString();

            setConfig((prev) => ({
                ...prev,
                lastTestSucceeded: isSuccess,
                lastTestedAt: testedAt,
            }));

            if (!isSuccess) {
                setError(
                    data?.message ||
                        (data?.errorCode
                            ? `Lỗi kết nối Jira: ${data.errorCode}`
                            : "Kiểm tra kết nối Jira thất bại."),
                );
            } else {
                setMessage("Kết nối tới Jira thành công!");
            }
        } catch (err) {
            const testedAt = new Date().toISOString();
            setConfig((prev) => ({
                ...prev,
                lastTestSucceeded: false,
                lastTestedAt: testedAt,
            }));
            setError(
                err.response?.data?.message ||
                    err.response?.data?.code ||
                    err.message ||
                    "Kiểm tra kết nối Jira thất bại.",
            );
        } finally {
            setTesting(false);
        }
    };

    const renderConnectionBadge = () => {
        if (!config.configured) {
            return (
                <span
                    data-testid="connection-status-badge"
                    style={{
                        padding: "4px 10px",
                        borderRadius: "4px",
                        fontSize: "12px",
                        fontWeight: 700,
                        backgroundColor: "#ebecf0",
                        color: "#42526e",
                    }}
                >
                    NOT_CONFIGURED
                </span>
            );
        }

        if (
            config.lastTestSucceeded === null ||
            config.lastTestSucceeded === undefined
        ) {
            return (
                <span
                    data-testid="connection-status-badge"
                    style={{
                        padding: "4px 10px",
                        borderRadius: "4px",
                        fontSize: "12px",
                        fontWeight: 700,
                        backgroundColor: "#fff0b3",
                        color: "#172b4d",
                    }}
                >
                    NOT_CHECKED
                </span>
            );
        }

        if (config.lastTestSucceeded === true) {
            return (
                <span
                    data-testid="connection-status-badge"
                    style={{
                        padding: "4px 10px",
                        borderRadius: "4px",
                        fontSize: "12px",
                        fontWeight: 700,
                        backgroundColor: "#e3fcef",
                        color: "#006644",
                    }}
                >
                    CONNECTED
                </span>
            );
        }

        return (
            <span
                data-testid="connection-status-badge"
                style={{
                    padding: "4px 10px",
                    borderRadius: "4px",
                    fontSize: "12px",
                    fontWeight: 700,
                    backgroundColor: "#ffebe6",
                    color: "#de350b",
                }}
            >
                CONNECTION_FAILED
            </span>
        );
    };

    if (!canView) {
        return (
            <div
                data-testid="unauthorized-message"
                style={{
                    padding: "24px",
                    color: "#de350b",
                    fontSize: "14px",
                    fontWeight: 500,
                }}
            >
                Bạn không có quyền truy cập cấu hình này.
            </div>
        );
    }

    return (
        <div style={{ padding: "24px", maxWidth: "700px" }}>
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    marginBottom: "20px",
                }}
            >
                <div>
                    <h2
                        style={{
                            fontSize: "18px",
                            color: "#172b4d",
                            margin: "0 0 4px 0",
                        }}
                    >
                        Cấu hình tích hợp Jira
                    </h2>
                    <p
                        style={{
                            margin: 0,
                            fontSize: "13px",
                            color: "#6b778c",
                        }}
                    >
                        Thiết lập đồng bộ Task và Requirement trực tiếp từ Jira
                        Cloud.
                    </p>
                </div>
                <div style={{ textAlign: "right" }}>
                    <div>{renderConnectionBadge()}</div>
                    {config.lastTestedAt && (
                        <div
                            data-testid="last-tested-at"
                            style={{
                                fontSize: "12px",
                                color: "#6b778c",
                                marginTop: "4px",
                            }}
                        >
                            Kiểm tra lần cuối:{" "}
                            {new Date(config.lastTestedAt).toLocaleString(
                                "vi-VN",
                            )}
                        </div>
                    )}
                </div>
            </div>

            {message && (
                <div
                    data-testid="jira-success-message"
                    style={{
                        padding: "10px 14px",
                        backgroundColor: "#e3fcef",
                        color: "#006644",
                        borderRadius: "4px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    {message}
                </div>
            )}

            {error && (
                <div
                    data-testid="jira-error-message"
                    style={{
                        padding: "10px 14px",
                        backgroundColor: "#ffebe6",
                        color: "#de350b",
                        borderRadius: "4px",
                        marginBottom: "16px",
                        fontSize: "13px",
                    }}
                >
                    {error}
                </div>
            )}

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
                        htmlFor="jira-site-url"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Jira Base URL *
                    </label>
                    <input
                        id="jira-site-url"
                        type="url"
                        name="siteUrl"
                        value={formData.siteUrl}
                        onChange={handleInputChange}
                        disabled={!canEdit}
                        placeholder="https://your-domain.atlassian.net"
                        required
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            boxSizing: "border-box",
                            backgroundColor: !canEdit ? "#f8fafc" : "#fff",
                            cursor: !canEdit ? "not-allowed" : "text",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="jira-project-key"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Project Key *
                    </label>
                    <input
                        id="jira-project-key"
                        type="text"
                        name="projectKey"
                        value={formData.projectKey}
                        onChange={handleInputChange}
                        disabled={!canEdit}
                        placeholder="VD: CNPM, PROJ"
                        required
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            boxSizing: "border-box",
                            backgroundColor: !canEdit ? "#f8fafc" : "#fff",
                            cursor: !canEdit ? "not-allowed" : "text",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="jira-email"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        Atlassian Account Email {canEdit && "*"}
                    </label>
                    <input
                        id="jira-email"
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleInputChange}
                        disabled={!canEdit}
                        required={canEdit}
                        placeholder={
                            !canEdit
                                ? "•••••••••••••••• (Đã ẩn vì lý do bảo mật)"
                                : "VD: admin@company.com"
                        }
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            boxSizing: "border-box",
                            backgroundColor: !canEdit ? "#f8fafc" : "#fff",
                            cursor: !canEdit ? "not-allowed" : "text",
                        }}
                    />
                </div>

                <div>
                    <label
                        htmlFor="jira-api-token"
                        style={{
                            display: "block",
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                            marginBottom: "4px",
                        }}
                    >
                        API Token (Write-only) {canEdit && "*"}
                    </label>
                    <input
                        id="jira-api-token"
                        type="password"
                        name="apiToken"
                        value={formData.apiToken}
                        onChange={handleInputChange}
                        disabled={!canEdit}
                        required={canEdit}
                        placeholder={
                            !canEdit
                                ? "•••••••••••••••• (Đã ẩn vì lý do bảo mật)"
                                : "Nhập Jira API Token..."
                        }
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            boxSizing: "border-box",
                            backgroundColor: !canEdit ? "#f8fafc" : "#fff",
                            cursor: !canEdit ? "not-allowed" : "text",
                        }}
                    />
                </div>

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "12px",
                        marginTop: "12px",
                    }}
                >
                    {canTest && (
                        <button
                            type="button"
                            data-testid="test-connection-btn"
                            onClick={handleTestConnection}
                            disabled={testing || !config.configured}
                            style={{
                                padding: "9px 18px",
                                backgroundColor: "#f1f5f9",
                                border: "1px solid #94a3b8",
                                borderRadius: "6px",
                                fontWeight: 600,
                                fontSize: "13px",
                                color: "#1e293b",
                                cursor:
                                    testing || !config.configured
                                        ? "not-allowed"
                                        : "pointer",
                                opacity:
                                    testing || !config.configured ? 0.6 : 1,
                            }}
                        >
                            {testing ? "Đang kiểm tra..." : "Test Connection"}
                        </button>
                    )}

                    {canEdit && (
                        <button
                            type="submit"
                            data-testid="save-config-btn"
                            disabled={submitting}
                            style={{
                                padding: "9px 22px",
                                backgroundColor: "#1d4ed8",
                                border: "none",
                                borderRadius: "6px",
                                fontWeight: 600,
                                fontSize: "13px",
                                color: "#ffffff",
                                cursor: submitting ? "not-allowed" : "pointer",
                                opacity: submitting ? 0.7 : 1,
                            }}
                        >
                            {submitting ? "Đang lưu..." : "Lưu cấu hình"}
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
}
