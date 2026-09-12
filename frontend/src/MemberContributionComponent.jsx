import React, { useState, useEffect, useCallback } from "react";
import {
    getMemberContributions,
    getProjectSprints,
} from "./memberContributionService";

function MemberContributionComponent({
    projectId,
    sprints: initialSprints = [],
}) {
    const [sprintList, setSprintList] = useState(initialSprints);
    const [selectedSprintId, setSelectedSprintId] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [reportData, setReportData] = useState(null);

    useEffect(() => {
        if (initialSprints && initialSprints.length > 0) {
            setSprintList(initialSprints);
        } else if (projectId) {
            getProjectSprints(projectId).then((data) => {
                if (data && data.length > 0) {
                    setSprintList(data);
                }
            });
        }
    }, [projectId, initialSprints]);

    const loadData = useCallback(
        async (overrideFilters = null) => {
            if (!projectId) return;
            setLoading(true);
            setError(null);

            const sprintIdToUse =
                overrideFilters && overrideFilters.sprintId !== undefined
                    ? overrideFilters.sprintId
                    : selectedSprintId;
            const fromDateToUse =
                overrideFilters && overrideFilters.fromDate !== undefined
                    ? overrideFilters.fromDate
                    : fromDate;
            const toDateToUse =
                overrideFilters && overrideFilters.toDate !== undefined
                    ? overrideFilters.toDate
                    : toDate;

            try {
                const data = await getMemberContributions(projectId, {
                    sprintId: sprintIdToUse || undefined,
                    fromDate: fromDateToUse || undefined,
                    toDate: toDateToUse || undefined,
                });
                setReportData(data);
            } catch (err) {
                setError(
                    err.message || "Lỗi khi tải dữ liệu đóng góp thành viên.",
                );
                setReportData(null);
            } finally {
                setLoading(false);
            }
        },
        [projectId, selectedSprintId, fromDate, toDate],
    );

    useEffect(() => {
        loadData();
    }, [projectId]);

    const handleApply = (e) => {
        e.preventDefault();
        loadData();
    };

    const handleReset = () => {
        setSelectedSprintId("");
        setFromDate("");
        setToDate("");
        loadData({ sprintId: "", fromDate: "", toDate: "" });
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
                    onSubmit={handleApply}
                    style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        gap: "12px",
                        flexWrap: "wrap",
                        width: "100%",
                    }}
                >
                    <div
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
                                    fontSize: "13px",
                                    color: "#475569",
                                    fontWeight: 500,
                                    whiteSpace: "nowrap",
                                }}
                            >
                                Sprint:
                            </span>
                            <select
                                aria-label="Sprint"
                                value={selectedSprintId}
                                onChange={(e) =>
                                    setSelectedSprintId(e.target.value)
                                }
                                style={{
                                    minWidth: "130px",
                                    maxWidth: "160px",
                                    padding: "5px 8px",
                                    fontSize: "13px",
                                    borderRadius: "6px",
                                    border: "1px solid #cbd5e1",
                                    backgroundColor: "#ffffff",
                                    outline: "none",
                                    cursor: "pointer",
                                }}
                            >
                                <option value="">Tất cả Sprint</option>
                                {sprintList.map((sp) => (
                                    <option
                                        key={sp.sprintId || sp.id}
                                        value={sp.sprintId || sp.id}
                                    >
                                        {sp.sprintName ||
                                            sp.name ||
                                            `Sprint #${sp.sprintId || sp.id}`}
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
                                    fontSize: "13px",
                                    color: "#475569",
                                    fontWeight: 500,
                                    whiteSpace: "nowrap",
                                }}
                            >
                                Từ:
                            </span>
                            <input
                                aria-label="Từ ngày"
                                type="date"
                                value={fromDate}
                                onChange={(e) => setFromDate(e.target.value)}
                                style={{
                                    padding: "4px 6px",
                                    fontSize: "12px",
                                    borderRadius: "6px",
                                    border: "1px solid #cbd5e1",
                                    outline: "none",
                                    width: "125px",
                                }}
                            />
                            <span
                                style={{
                                    fontSize: "13px",
                                    color: "#475569",
                                    fontWeight: 500,
                                    whiteSpace: "nowrap",
                                }}
                            >
                                Đến:
                            </span>
                            <input
                                aria-label="Đến ngày"
                                type="date"
                                value={toDate}
                                onChange={(e) => setToDate(e.target.value)}
                                style={{
                                    padding: "4px 6px",
                                    fontSize: "12px",
                                    borderRadius: "6px",
                                    border: "1px solid #cbd5e1",
                                    outline: "none",
                                    width: "125px",
                                }}
                            />
                        </div>
                    </div>

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "8px",
                            flexShrink: 0,
                        }}
                    >
                        <button
                            type="submit"
                            style={{
                                padding: "6px 14px",
                                backgroundColor: "#2563eb",
                                color: "#ffffff",
                                border: "none",
                                borderRadius: "6px",
                                cursor: "pointer",
                                fontSize: "13px",
                                fontWeight: 600,
                                whiteSpace: "nowrap",
                            }}
                        >
                            Áp dụng
                        </button>

                        <button
                            type="button"
                            onClick={handleReset}
                            style={{
                                padding: "6px 14px",
                                backgroundColor: "#f8fafc",
                                color: "#475569",
                                border: "1px solid #cbd5e1",
                                borderRadius: "6px",
                                cursor: "pointer",
                                fontSize: "13px",
                                fontWeight: 500,
                                whiteSpace: "nowrap",
                            }}
                        >
                            Đặt lại
                        </button>
                    </div>
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
                    role="alert"
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
                        onClick={() => loadData()}
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
                                        m.memberId || m.userId || idx;
                                    const displayName =
                                        m.fullName ||
                                        m.username ||
                                        "Thành viên";
                                    const username = m.username
                                        ? `@${m.username}`
                                        : "";
                                    const isGithubLinked = Boolean(
                                        m.githubLinked,
                                    );
                                    const linkedTasks = m.linkedTasks ?? 0;
                                    const commits = m.commits ?? 0;
                                    const pullRequests = m.pullRequests ?? 0;

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
                                                {isGithubLinked ? (
                                                    <span
                                                        style={{
                                                            display:
                                                                "inline-block",
                                                            padding: "3px 8px",
                                                            backgroundColor:
                                                                "#ecfdf5",
                                                            color: "#065f46",
                                                            border: "1px solid #a7f3d0",
                                                            borderRadius: "6px",
                                                            fontSize: "12px",
                                                            fontWeight: 600,
                                                        }}
                                                    >
                                                        Đã liên kết
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
                                                            fontSize: "12px",
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
                                                {linkedTasks}
                                            </td>

                                            <td
                                                style={{
                                                    padding: "14px 24px",
                                                    textAlign: "center",
                                                    fontWeight: 700,
                                                    color: "#334155",
                                                }}
                                            >
                                                {commits}
                                            </td>

                                            <td
                                                style={{
                                                    padding: "14px 24px",
                                                    textAlign: "center",
                                                    fontWeight: 700,
                                                    color: "#334155",
                                                }}
                                            >
                                                {pullRequests}
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

export default MemberContributionComponent;
export { MemberContributionComponent };
