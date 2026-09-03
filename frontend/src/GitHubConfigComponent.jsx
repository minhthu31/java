import React, { useState, useEffect } from "react";
import { GitHubConfigService } from "./GitHubConfigService";

export const GitHubConfigComponent = ({ currentUserRole, projectId }) => {
    const [repositoryOwner, setRepositoryOwner] = useState("");
    const [repositoryName, setRepositoryName] = useState("");
    const [accessToken, setAccessToken] = useState("");
    const [apiVersion, setApiVersion] = useState("2026-03-10");

    const [isConfigured, setIsConfigured] = useState(false);
    const [isDirty, setIsDirty] = useState(false);

    const [connectionStatus, setConnectionStatus] = useState("NOT_CHECKED");
    const [lastChecked, setLastChecked] = useState(null);
    const [failureReason, setFailureReason] = useState("");

    const [initialLoading, setInitialLoading] = useState(true);
    const [loadError, setLoadError] = useState(null);
    const [isSaving, setIsSaving] = useState(false);
    const [isTesting, setIsTesting] = useState(false);
    const [message, setMessage] = useState(null);

    const fetchExistingConfig = async () => {
        if (!projectId) {
            setInitialLoading(false);
            return;
        }
        setInitialLoading(true);
        setLoadError(null);
        try {
            const data = await GitHubConfigService.getConfig(projectId);
            if (data) {
                if (data.repositoryFullName) {
                    const parts = data.repositoryFullName.split("/");
                    setRepositoryOwner(parts[0] || "");
                    setRepositoryName(parts[1] || "");
                } else {
                    if (data.repositoryOwner)
                        setRepositoryOwner(data.repositoryOwner);
                    if (data.repositoryName)
                        setRepositoryName(data.repositoryName);
                }
                setIsConfigured(Boolean(data.configured));
                setConnectionStatus(data.status || "NOT_CHECKED");
                setLastChecked(
                    data.lastTestedAt
                        ? new Date(data.lastTestedAt).toLocaleString("vi-VN")
                        : null,
                );
                setIsDirty(false);
            }
        } catch (err) {
            setLoadError(
                err.response?.data?.message ||
                    "Không thể tải cấu hình tích hợp GitHub hiện tại. Vui lòng thử lại.",
            );
        } finally {
            setInitialLoading(false);
        }
    };

    useEffect(() => {
        if (currentUserRole === "ADMIN") {
            fetchExistingConfig();
        } else {
            setInitialLoading(false);
        }
    }, [currentUserRole, projectId]);

    if (currentUserRole !== "ADMIN") {
        return (
            <div
                style={{
                    padding: "20px",
                    backgroundColor: "#ffebe6",
                    color: "#de350b",
                    borderRadius: "6px",
                    border: "1px solid #ffbdad",
                }}
            >
                <h3
                    style={{
                        margin: "0 0 8px",
                        fontSize: "16px",
                        fontWeight: 600,
                    }}
                >
                    403 - Truy cập bị từ chối
                </h3>
                <p style={{ margin: 0, fontSize: "14px" }}>
                    Bạn không có quyền Admin để cấu hình GitHub.
                </p>
            </div>
        );
    }

    if (!projectId) {
        return (
            <div
                data-testid="no-project-message"
                style={{
                    padding: "40px 24px",
                    textAlign: "center",
                    color: "#6b778c",
                }}
            >
                <h3 style={{ color: "#172b4d", marginBottom: "8px" }}>
                    Chưa chọn dự án
                </h3>
                <p style={{ margin: 0, fontSize: "14px" }}>
                    Vui lòng chọn một project trước khi cấu hình tích hợp
                    GitHub.
                </p>
            </div>
        );
    }

    const handleSave = async (e) => {
        e.preventDefault();
        if (loadError || isBusy) return;

        if (!repositoryOwner.trim() || !repositoryName.trim()) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập đầy đủ Repository Owner và Repository Name.",
            });
            return;
        }

        if (!accessToken.trim() && !isConfigured) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập Personal Access Token (PAT) cho lần cấu hình đầu tiên.",
            });
            return;
        }

        try {
            setIsSaving(true);
            setMessage(null);

            const payload = {
                repositoryOwner: repositoryOwner.trim(),
                repositoryName: repositoryName.trim(),
                accessToken: accessToken.trim() ? accessToken.trim() : null,
                apiVersion: apiVersion || "2026-03-10",
            };

            const result = await GitHubConfigService.saveConfig(
                projectId,
                payload,
            );
            setIsConfigured(true);
            setIsDirty(false);
            setAccessToken("");
            if (result?.status) setConnectionStatus(result.status);
            setMessage({
                type: "success",
                text: "Lưu cấu hình GitHub thành công.",
            });
        } catch (err) {
            setMessage({
                type: "error",
                text:
                    err.response?.data?.message ||
                    "Lưu cấu hình thất bại. Vui lòng kiểm tra lại thông số.",
            });
        } finally {
            setIsSaving(false);
        }
    };

    const handleTestConnection = async () => {
        if (!isConfigured || isDirty || loadError || isBusy) {
            setMessage({
                type: "error",
                text: "Vui lòng lưu cấu hình trước khi kiểm tra kết nối.",
            });
            return;
        }

        try {
            setIsTesting(true);
            setFailureReason("");
            setMessage(null);

            const result = await GitHubConfigService.testConnection(projectId);

            if (result && result.connected) {
                setConnectionStatus("CONNECTED");
                setFailureReason("");
            } else {
                setConnectionStatus("CONNECTION_FAILED");
                setFailureReason("Không thể thiết lập kết nối tới GitHub.");
            }
            setLastChecked(
                result?.testedAt
                    ? new Date(result.testedAt).toLocaleString("vi-VN")
                    : new Date().toLocaleString("vi-VN"),
            );
        } catch (err) {
            setConnectionStatus("CONNECTION_FAILED");
            const errResponse = err.response?.data;
            const errDetail =
                errResponse?.message ||
                "Kết nối thất bại. Token không hợp lệ hoặc không có quyền truy cập repository.";
            setFailureReason(errDetail);
            setLastChecked(new Date().toLocaleString("vi-VN"));
        } finally {
            setIsTesting(false);
        }
    };

    const isBusy = initialLoading || isSaving || isTesting;

    const canTest = isConfigured && !isDirty && !loadError && !isBusy;

    const canSave = !loadError && !isBusy;

    const statusBadgeStyle = {
        CONNECTED: { bg: "#e3fcef", color: "#006644", text: "Connected" },
        CONNECTION_FAILED: { bg: "#ffebe6", color: "#de350b", text: "Failed" },
        NOT_CONFIGURED: {
            bg: "#ebecf0",
            color: "#42526e",
            text: "Not Checked",
        },
        NOT_CHECKED: { bg: "#ebecf0", color: "#42526e", text: "Not Checked" },
    }[connectionStatus] || {
        bg: "#ebecf0",
        color: "#42526e",
        text: "Not Checked",
    };

    const labelStyle = {
        display: "block",
        marginBottom: "6px",
        fontSize: "13px",
        fontWeight: 600,
        color: "#172b4d",
    };

    const inputStyle = {
        width: "100%",
        padding: "10px 12px",
        fontSize: "14px",
        border: "1px solid #dfe1e6",
        borderRadius: "4px",
        backgroundColor: isBusy || Boolean(loadError) ? "#f4f5f7" : "#fafbfc",
        color: "#172b4d",
        boxSizing: "border-box",
        outline: "none",
    };

    return (
        <div
            style={{ maxWidth: "680px", margin: "0 auto", padding: "8px 4px" }}
        >
            <div style={{ marginBottom: "20px" }}>
                <h2
                    style={{
                        margin: "0 0 4px",
                        fontSize: "20px",
                        fontWeight: 700,
                        color: "#172b4d",
                    }}
                >
                    Cấu hình tích hợp GitHub
                </h2>
                <p style={{ margin: 0, fontSize: "13px", color: "#6b778c" }}>
                    Thiết lập thông tin xác thực để đồng bộ nhánh, commit và kéo
                    dữ liệu Pull Request.
                </p>
            </div>

            {loadError && (
                <div
                    data-testid="initial-load-error"
                    style={{
                        padding: "12px 16px",
                        marginBottom: "20px",
                        borderRadius: "6px",
                        fontSize: "13px",
                        backgroundColor: "#ffebe6",
                        color: "#de350b",
                        border: "1px solid #ffbdad",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                    }}
                >
                    <span>{loadError}</span>
                    <button
                        type="button"
                        onClick={fetchExistingConfig}
                        style={{
                            padding: "4px 10px",
                            backgroundColor: "#de350b",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            fontSize: "12px",
                            cursor: "pointer",
                        }}
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {message && (
                <div
                    role="alert"
                    style={{
                        padding: "12px 16px",
                        marginBottom: "20px",
                        borderRadius: "6px",
                        fontSize: "13px",
                        backgroundColor:
                            message.type === "success" ? "#e3fcef" : "#ffebe6",
                        color:
                            message.type === "success" ? "#006644" : "#de350b",
                        border: `1px solid ${message.type === "success" ? "#abf5d1" : "#ffbdad"}`,
                    }}
                >
                    {message.text}
                </div>
            )}

            <form onSubmit={handleSave}>
                <div style={{ marginBottom: "18px" }}>
                    <label htmlFor="owner-input" style={labelStyle}>
                        Repository Owner / Organization{" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="owner-input"
                        type="text"
                        style={inputStyle}
                        placeholder="e.g. github-username hoặc organization-name"
                        value={repositoryOwner}
                        onChange={(e) => {
                            setRepositoryOwner(e.target.value);
                            setIsDirty(true);
                        }}
                        disabled={isBusy || Boolean(loadError)}
                    />
                </div>

                <div style={{ marginBottom: "18px" }}>
                    <label htmlFor="repo-input" style={labelStyle}>
                        Repository Name{" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="repo-input"
                        type="text"
                        style={inputStyle}
                        placeholder="e.g. cnpm-project-support"
                        value={repositoryName}
                        onChange={(e) => {
                            setRepositoryName(e.target.value);
                            setIsDirty(true);
                        }}
                        disabled={isBusy || Boolean(loadError)}
                    />
                </div>

                <div style={{ marginBottom: "20px" }}>
                    <label htmlFor="token-input" style={labelStyle}>
                        Personal Access Token (PAT){" "}
                        {!isConfigured && (
                            <span style={{ color: "#de350b" }}>*</span>
                        )}
                    </label>
                    <input
                        id="token-input"
                        type="password"
                        autoComplete="new-password"
                        style={inputStyle}
                        placeholder={
                            isConfigured
                                ? "•••••••••••••••• (Đã cấu hình, để trống nếu không muốn đổi)"
                                : "ghp_xxxxxxxxxxxxxxxxxxxx"
                        }
                        value={accessToken}
                        onChange={(e) => {
                            setAccessToken(e.target.value);
                            setIsDirty(true);
                        }}
                        disabled={isBusy || Boolean(loadError)}
                    />
                    <span
                        style={{
                            display: "block",
                            marginTop: "6px",
                            fontSize: "12px",
                            color: "#6b778c",
                        }}
                    >
                        Token được mã hóa an toàn, không được trả về phía client
                        qua API sau khi lưu.
                    </span>
                </div>

                <div
                    style={{
                        padding: "14px 16px",
                        backgroundColor: "#f4f5f7",
                        borderRadius: "6px",
                        marginBottom: "20px",
                        display: "flex",
                        flexDirection: "column",
                        gap: "8px",
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "10px",
                        }}
                    >
                        <span
                            style={{
                                fontSize: "13px",
                                fontWeight: 600,
                                color: "#172b4d",
                            }}
                        >
                            Trạng thái kết nối:
                        </span>
                        <span
                            data-testid="connection-status-badge"
                            style={{
                                padding: "3px 10px",
                                borderRadius: "12px",
                                fontSize: "12px",
                                fontWeight: 600,
                                backgroundColor: statusBadgeStyle.bg,
                                color: statusBadgeStyle.color,
                            }}
                        >
                            {statusBadgeStyle.text}
                        </span>
                    </div>

                    {lastChecked && (
                        <div
                            data-testid="last-checked-time"
                            style={{ fontSize: "12px", color: "#6b778c" }}
                        >
                            Thời điểm kiểm tra gần nhất: {lastChecked}
                        </div>
                    )}
                </div>

                {connectionStatus === "CONNECTION_FAILED" && (
                    <div
                        style={{
                            padding: "14px 16px",
                            backgroundColor: "#fff0b3",
                            border: "1px solid #ffe380",
                            borderRadius: "6px",
                            marginBottom: "20px",
                            color: "#172b4d",
                            fontSize: "13px",
                        }}
                    >
                        <div
                            style={{
                                fontWeight: 600,
                                color: "#bf2600",
                                marginBottom: "6px",
                            }}
                        >
                            Lý do thất bại:{" "}
                            {failureReason ||
                                "Lỗi xác thực hoặc không tìm thấy Repository."}
                        </div>
                        <div style={{ fontWeight: 600, marginBottom: "4px" }}>
                            Hướng xử lý gợi ý:
                        </div>
                        <ul
                            style={{
                                margin: "0",
                                paddingLeft: "20px",
                                color: "#42526e",
                                lineHeight: "1.5",
                            }}
                        >
                            <li>
                                Kiểm tra xem tên Owner và Repository có đúng
                                chính tả và có tồn tại hay không.
                            </li>
                            <li>
                                Đảm bảo token chưa hết hạn và đã cấp quyền tối
                                thiểu:{" "}
                                <code
                                    style={{
                                        backgroundColor: "#fffae6",
                                        padding: "1px 4px",
                                        borderRadius: "3px",
                                    }}
                                >
                                    Metadata: Read
                                </code>
                                ,{" "}
                                <code
                                    style={{
                                        backgroundColor: "#fffae6",
                                        padding: "1px 4px",
                                        borderRadius: "3px",
                                    }}
                                >
                                    Contents: Read
                                </code>
                                .
                            </li>
                            <li>
                                Kiểm tra giới hạn lượt gọi (Rate Limit) của
                                GitHub REST API.
                            </li>
                        </ul>
                    </div>
                )}

                <div
                    style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "12px",
                    }}
                >
                    <button
                        type="button"
                        onClick={handleTestConnection}
                        disabled={!canTest}
                        title={
                            !isConfigured || isDirty
                                ? "Vui lòng lưu cấu hình trước khi kiểm tra kết nối"
                                : ""
                        }
                        style={{
                            padding: "9px 16px",
                            fontSize: "14px",
                            fontWeight: 500,
                            backgroundColor: "#f4f5f7",
                            color: "#0052cc",
                            border: "1px solid #c1c7d0",
                            borderRadius: "4px",
                            cursor: !canTest ? "not-allowed" : "pointer",
                            opacity: !canTest ? 0.6 : 1,
                        }}
                    >
                        {isTesting ? "Đang kiểm tra..." : "Test Connection"}
                    </button>

                    <button
                        type="submit"
                        disabled={!canSave}
                        style={{
                            padding: "9px 18px",
                            fontSize: "14px",
                            fontWeight: 500,
                            backgroundColor: "#0052cc",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: !canSave ? "not-allowed" : "pointer",
                            opacity: !canSave ? 0.6 : 1,
                        }}
                    >
                        {isSaving ? "Đang lưu..." : "Save Configuration"}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default GitHubConfigComponent;
