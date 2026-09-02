import React, { useState, useEffect } from "react";
import { GitHubConfigService } from "./GitHubConfigService";

export const GitHubConfigComponent = ({ currentUserRole }) => {
    const [owner, setOwner] = useState("");
    const [repository, setRepository] = useState("");
    const [token, setToken] = useState("");
    const [isTokenConfigured, setIsTokenConfigured] = useState(false);

    const [connectionStatus, setConnectionStatus] = useState("NOT_CHECKED");
    const [lastChecked, setLastChecked] = useState(null);
    const [failureReason, setFailureReason] = useState("");

    const [initialLoading, setInitialLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isTesting, setIsTesting] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        let isMounted = true;
        const fetchExistingConfig = async () => {
            try {
                const data = await GitHubConfigService.getConfig();
                if (data && isMounted) {
                    setOwner(data.owner || "");
                    setRepository(data.repository || "");
                    setIsTokenConfigured(Boolean(data.hasToken));
                    setConnectionStatus(data.status || "NOT_CHECKED");
                    setLastChecked(
                        data.lastCheckedAt
                            ? new Date(data.lastCheckedAt).toLocaleString(
                                  "vi-VN",
                              )
                            : null,
                    );
                }
            } catch (err) {
            } finally {
                if (isMounted) {
                    setInitialLoading(false);
                }
            }
        };

        if (currentUserRole === "ADMIN") {
            fetchExistingConfig();
        } else {
            setInitialLoading(false);
        }

        return () => {
            isMounted = false;
        };
    }, [currentUserRole]);

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

    const handleSave = async (e) => {
        e.preventDefault();
        if (!owner.trim() || !repository.trim()) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập đầy đủ Repository Owner và Repository Name.",
            });
            return;
        }
        if (!token && !isTokenConfigured) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập GitHub Personal Access Token (PAT).",
            });
            return;
        }

        try {
            setIsSaving(true);
            setMessage(null);
            await GitHubConfigService.saveConfig({
                owner: owner.trim(),
                repository: repository.trim(),
                ...(token ? { token: token.trim() } : {}),
            });
            setIsTokenConfigured(true);
            setToken("");
            setMessage({
                type: "success",
                text: "Lưu cấu hình GitHub thành công.",
            });
        } catch (err) {
            setMessage({
                type: "error",
                text:
                    err.response?.data?.message ||
                    "Lưu cấu hình thất bại. Vui lòng kiểm tra lại hệ thống.",
            });
        } finally {
            setIsSaving(false);
        }
    };

    const handleTestConnection = async () => {
        if (!owner.trim() || !repository.trim()) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập Owner và Repository trước khi kiểm tra kết nối.",
            });
            return;
        }
        if (!token && !isTokenConfigured) {
            setMessage({
                type: "error",
                text: "Vui lòng nhập Token để kiểm tra kết nối.",
            });
            return;
        }

        try {
            setIsTesting(true);
            setFailureReason("");
            setMessage(null);

            const result = await GitHubConfigService.testConnection({
                owner: owner.trim(),
                repository: repository.trim(),
                ...(token ? { token: token.trim() } : {}),
            });

            if (result && result.connected) {
                setConnectionStatus("CONNECTED");
                setFailureReason("");
            } else {
                setConnectionStatus("FAILED");
                setFailureReason(
                    result?.message ||
                        "Không thể kết nối tới GitHub Repository.",
                );
            }
            setLastChecked(new Date().toLocaleString("vi-VN"));
        } catch (err) {
            setConnectionStatus("FAILED");
            const errDetail =
                err.response?.data?.message ||
                "Kết nối thất bại. Vui lòng kiểm tra lại Token hoặc quyền hạn Repository.";
            setFailureReason(errDetail);
            setLastChecked(new Date().toLocaleString("vi-VN"));
        } finally {
            setIsTesting(false);
        }
    };

    const isBusy = initialLoading || isSaving || isTesting;

    const statusBadgeStyle = {
        CONNECTED: { bg: "#e3fcef", color: "#006644", text: "Connected" },
        FAILED: { bg: "#ffebe6", color: "#de350b", text: "Failed" },
        NOT_CHECKED: { bg: "#ebecf0", color: "#42526e", text: "Not Checked" },
    }[connectionStatus];

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
        backgroundColor: isBusy ? "#f4f5f7" : "#fafbfc",
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
                        value={owner}
                        onChange={(e) => setOwner(e.target.value)}
                        disabled={isBusy}
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
                        value={repository}
                        onChange={(e) => setRepository(e.target.value)}
                        disabled={isBusy}
                    />
                </div>

                <div style={{ marginBottom: "20px" }}>
                    <label htmlFor="token-input" style={labelStyle}>
                        Personal Access Token (PAT){" "}
                        <span style={{ color: "#de350b" }}>*</span>
                    </label>
                    <input
                        id="token-input"
                        type="password"
                        autoComplete="new-password"
                        style={inputStyle}
                        placeholder={
                            isTokenConfigured
                                ? "•••••••••••••••• (Đã cấu hình, nhập mới để đổi token khác)"
                                : "ghp_xxxxxxxxxxxxxxxxxxxx"
                        }
                        value={token}
                        onChange={(e) => setToken(e.target.value)}
                        disabled={isBusy}
                    />
                    <span
                        style={{
                            display: "block",
                            marginTop: "6px",
                            fontSize: "12px",
                            color: "#6b778c",
                        }}
                    >
                        Token được che và mã hóa an toàn, không hiển thị lại qua
                        API sau khi lưu.
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

                {connectionStatus === "FAILED" && (
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
                                "Không tìm thấy Repository hoặc Token không hợp lệ."}
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
                                    repo
                                </code>{" "}
                                hoặc{" "}
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
                        disabled={isBusy}
                        style={{
                            padding: "9px 16px",
                            fontSize: "14px",
                            fontWeight: 500,
                            backgroundColor: "#f4f5f7",
                            color: "#0052cc",
                            border: "1px solid #c1c7d0",
                            borderRadius: "4px",
                            cursor: isBusy ? "not-allowed" : "pointer",
                            opacity: isBusy ? 0.6 : 1,
                        }}
                    >
                        {isTesting ? "Đang kiểm tra..." : "Test Connection"}
                    </button>

                    <button
                        type="submit"
                        disabled={isBusy}
                        style={{
                            padding: "9px 18px",
                            fontSize: "14px",
                            fontWeight: 500,
                            backgroundColor: "#0052cc",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: isBusy ? "not-allowed" : "pointer",
                            opacity: isBusy ? 0.6 : 1,
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
