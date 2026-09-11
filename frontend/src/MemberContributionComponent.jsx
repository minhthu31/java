import React, { useState, useEffect } from "react";
import { getMemberContributions } from "./memberContributionService";

export default function MemberContributionComponent({
    projectId,
    sprints = [],
}) {
    const [selectedSprintId, setSelectedSprintId] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [reportData, setReportData] = useState(null);

    const loadData = async () => {
        if (!projectId) return;
        setLoading(true);
        setError(null);
        try {
            const data = await getMemberContributions(projectId, {
                sprintId: selectedSprintId || undefined,
                fromDate: fromDate || undefined,
                toDate: toDate || undefined,
            });
            setReportData(data);
        } catch (err) {
            setError(err.message || "Lỗi khi tải dữ liệu đóng góp thành viên");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [projectId]);

    const handleApplyFilter = (e) => {
        e.preventDefault();
        loadData();
    };

    const handleResetFilter = () => {
        setSelectedSprintId("");
        setFromDate("");
        setToDate("");
        setTimeout(() => loadData(), 0);
    };

    const members =
        reportData?.memberContributions ||
        reportData?.members ||
        (Array.isArray(reportData) ? reportData : []);

    return (
        <div
            style={{
                marginTop: "20px",
                backgroundColor: "#ffffff",
                borderRadius: "14px",
                border: "1px solid #dbeafe",
                boxShadow: "0 2px 10px rgba(37, 99, 235, 0.04)",
                overflow: "hidden",
                width: "100%",
                boxSizing: "border-box",
                fontFamily:
                    "'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif",
            }}
        >
            <div
                style={{
                    padding: "18px 24px",
                    borderBottom: "1px solid #f1f5f9",
                    backgroundColor: "#fcfdfe",
                }}
            >
                <div style={{ marginBottom: "14px" }}>
                    <h3
                        style={{
                            margin: 0,
                            fontSize: "16px",
                            fontWeight: 700,
                            color: "#0f172a",
                        }}
                    >
                        Mức độ đóng góp của từng thành viên
                    </h3>
                    <p
                        style={{
                            margin: "4px 0 0",
                            fontSize: "13px",
                            color: "#64748b",
                        }}
                    >
                        Thống kê công việc hoàn thành, commit và pull request
                        tương ứng theo Sprint hoặc mốc thời gian
                    </p>
                </div>

                <form
                    onSubmit={handleApplyFilter}
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                        flexWrap: "wrap",
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                        }}
                    >
                        <span
                            style={{
                                fontSize: "12px",
                                color: "#475569",
                                fontWeight: 500,
                            }}
                        >
                            Sprint:
                        </span>
                        <select
                            value={selectedSprintId}
                            onChange={(e) =>
                                setSelectedSprintId(e.target.value)
                            }
                            style={{
                                padding: "6px 12px",
                                fontSize: "13px",
                                borderRadius: "8px",
                                border: "1px solid #cbd5e1",
                                backgroundColor: "#ffffff",
                                outline: "none",
                                cursor: "pointer",
                            }}
                        >
                            <option value="">Tất cả Sprint</option>
                            {sprints.map((sp) => (
                                <option
                                    key={sp.id || sp.sprintId}
                                    value={sp.id || sp.sprintId}
                                >
                                    {sp.name ||
                                        sp.sprintName ||
                                        `Sprint #${sp.id || sp.sprintId}`}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                        }}
                    >
                        <span
                            style={{
                                fontSize: "12px",
                                color: "#475569",
                                fontWeight: 500,
                            }}
                        >
                            Từ:
                        </span>
                        <input
                            type="date"
                            value={fromDate}
                            onChange={(e) => setFromDate(e.target.value)}
                            style={{
                                padding: "5px 8px",
                                fontSize: "13px",
                                borderRadius: "8px",
                                border: "1px solid #cbd5e1",
                                outline: "none",
                            }}
                        />
                        <span
                            style={{
                                fontSize: "12px",
                                color: "#475569",
                                fontWeight: 500,
                            }}
                        >
                            Đến:
                        </span>
                        <input
                            type="date"
                            value={toDate}
                            onChange={(e) => setToDate(e.target.value)}
                            style={{
                                padding: "5px 8px",
                                fontSize: "13px",
                                borderRadius: "8px",
                                border: "1px solid #cbd5e1",
                                outline: "none",
                            }}
                        />
                    </div>

                    <button
                        type="submit"
                        style={{
                            padding: "6px 16px",
                            backgroundColor: "#2563eb",
                            color: "#ffffff",
                            border: "none",
                            borderRadius: "8px",
                            cursor: "pointer",
                            fontSize: "13px",
                            fontWeight: 600,
                        }}
                    >
                        Áp dụng
                    </button>

                    <button
                        type="button"
                        onClick={handleResetFilter}
                        style={{
                            padding: "6px 14px",
                            backgroundColor: "#f8fafc",
                            color: "#475569",
                            border: "1px solid #cbd5e1",
                            borderRadius: "8px",
                            cursor: "pointer",
                            fontSize: "13px",
                            fontWeight: 500,
                        }}
                    >
                        Đặt lại
                    </button>
                </form>
            </div>

            {loading && (
                <div
                    style={{
                        padding: "40px 24px",
                        textAlign: "center",
                        color: "#64748b",
                        fontSize: "13px",
                    }}
                >
                    Đang tải dữ liệu đóng góp...
                </div>
            )}

            {!loading && error && (
                <div
                    style={{
                        margin: "20px 24px",
                        padding: "14px 18px",
                        backgroundColor: "#fef2f2",
                        borderRadius: "10px",
                        border: "1px solid #fecaca",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        color: "#b91c1c",
                        fontSize: "13px",
                    }}
                >
                    <span>{error}</span>
                    <button
                        onClick={loadData}
                        type="button"
                        style={{
                            padding: "6px 14px",
                            backgroundColor: "#dc2626",
                            color: "#ffffff",
                            border: "none",
                            borderRadius: "6px",
                            cursor: "pointer",
                            fontWeight: 600,
                            fontSize: "12px",
                        }}
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {!loading && !error && (
                <div style={{ overflowX: "auto", width: "100%" }}>
                    <table
                        style={{
                            width: "100%",
                            borderCollapse: "collapse",
                            textAlign: "left",
                            fontSize: "13px",
                        }}
                    >
                        <thead>
                            <tr
                                style={{
                                    backgroundColor: "#f8fafc",
                                    borderBottom: "1px solid #e2e8f0",
                                }}
                            >
                                <th
                                    style={{
                                        padding: "12px 24px",
                                        fontWeight: 600,
                                        color: "#475569",
                                    }}
                                >
                                    Thành viên
                                </th>
                                <th
                                    style={{
                                        padding: "12px 24px",
                                        fontWeight: 600,
                                        color: "#475569",
                                    }}
                                >
                                    Tài khoản GitHub
                                </th>
                                <th
                                    style={{
                                        padding: "12px 24px",
                                        fontWeight: 600,
                                        color: "#475569",
                                        textAlign: "center",
                                    }}
                                >
                                    Tasks hoàn thành
                                </th>
                                <th
                                    style={{
                                        padding: "12px 24px",
                                        fontWeight: 600,
                                        color: "#475569",
                                        textAlign: "center",
                                    }}
                                >
                                    Commits
                                </th>
                                <th
                                    style={{
                                        padding: "12px 24px",
                                        fontWeight: 600,
                                        color: "#475569",
                                        textAlign: "center",
                                    }}
                                >
                                    Pull Requests
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {members.length === 0 ? (
                                <tr>
                                    <td
                                        colSpan={5}
                                        style={{
                                            padding: "48px 24px",
                                            textAlign: "center",
                                            color: "#94a3b8",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Chưa ghi nhận thông tin đóng góp nào
                                        trong kỳ báo cáo này.
                                    </td>
                                </tr>
                            ) : (
                                members.map((m, idx) => {
                                    const memberId =
                                        m.memberId || m.userId || m.id || idx;
                                    const displayName =
                                        m.fullName ||
                                        m.username ||
                                        "Thành viên";
                                    const username = m.username
                                        ? `@${m.username}`
                                        : "";
                                    const githubUser = m.githubUsername;
                                    const completedTasks =
                                        m.linkedTasks ?? m.completedTasks ?? 0;
                                    const commits =
                                        m.commits ?? m.commitCount ?? 0;
                                    const pullRequests =
                                        m.pullRequests ??
                                        m.pullRequestCount ??
                                        0;

                                    return (
                                        <tr
                                            key={memberId}
                                            style={{
                                                borderBottom:
                                                    "1px solid #f1f5f9",
                                                backgroundColor:
                                                    idx % 2 === 0
                                                        ? "#ffffff"
                                                        : "#fafcff",
                                            }}
                                        >
                                            <td
                                                style={{ padding: "14px 24px" }}
                                            >
                                                <div
                                                    style={{
                                                        fontWeight: 600,
                                                        color: "#1e293b",
                                                    }}
                                                >
                                                    {displayName}
                                                </div>
                                                {username && (
                                                    <div
                                                        style={{
                                                            fontSize: "11px",
                                                            color: "#64748b",
                                                            marginTop: "2px",
                                                        }}
                                                    >
                                                        {username}
                                                    </div>
                                                )}
                                            </td>

                                            <td
                                                style={{ padding: "14px 24px" }}
                                            >
                                                {githubUser ? (
                                                    <span
                                                        style={{
                                                            display:
                                                                "inline-block",
                                                            padding: "3px 8px",
                                                            backgroundColor:
                                                                "#f1f5f9",
                                                            color: "#0f172a",
                                                            borderRadius: "6px",
                                                            fontWeight: 600,
                                                            fontSize: "12px",
                                                        }}
                                                    >
                                                        @{githubUser}
                                                    </span>
                                                ) : (
                                                    <span
                                                        style={{
                                                            display:
                                                                "inline-block",
                                                            padding: "3px 8px",
                                                            backgroundColor:
                                                                "#fffbeb",
                                                            color: "#b45309",
                                                            border: "1px solid #fef3c7",
                                                            borderRadius: "6px",
                                                            fontSize: "11px",
                                                            fontWeight: 500,
                                                        }}
                                                    >
                                                        Chưa liên kết
                                                    </span>
                                                )}
                                            </td>

                                            <td
                                                style={{
                                                    padding: "14px 24px",
                                                    textAlign: "center",
                                                    fontWeight: 700,
                                                    color: "#0f172a",
                                                }}
                                            >
                                                {completedTasks}
                                            </td>

                                            <td
                                                style={{
                                                    padding: "14px 24px",
                                                    textAlign: "center",
                                                }}
                                            >
                                                {m.commitUrl ? (
                                                    <a
                                                        href={m.commitUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        style={{
                                                            color: "#2563eb",
                                                            fontWeight: 700,
                                                            textDecoration:
                                                                "none",
                                                        }}
                                                    >
                                                        {commits} ↗
                                                    </a>
                                                ) : (
                                                    <span
                                                        style={{
                                                            fontWeight: 700,
                                                            color: "#334155",
                                                        }}
                                                    >
                                                        {commits}
                                                    </span>
                                                )}
                                            </td>

                                            <td
                                                style={{
                                                    padding: "14px 24px",
                                                    textAlign: "center",
                                                }}
                                            >
                                                {m.pullRequestUrl ? (
                                                    <a
                                                        href={m.pullRequestUrl}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        style={{
                                                            color: "#2563eb",
                                                            fontWeight: 700,
                                                            textDecoration:
                                                                "none",
                                                        }}
                                                    >
                                                        {pullRequests} ↗
                                                    </a>
                                                ) : (
                                                    <span
                                                        style={{
                                                            fontWeight: 700,
                                                            color: "#334155",
                                                        }}
                                                    >
                                                        {pullRequests}
                                                    </span>
                                                )}
                                            </td>
                                        </tr>
                                    );
                                })
                            )}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
