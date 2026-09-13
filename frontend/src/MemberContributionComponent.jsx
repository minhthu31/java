import React, { useState, useEffect, useRef } from "react";
import {
    getMemberContributions,
    getProjectSprints,
} from "./memberContributionService";

function MemberContributionComponent({ projectId, sprints }) {
    const [sprintList, setSprintList] = useState([]);
    const [selectedSprintId, setSelectedSprintId] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");
    const [dateError, setDateError] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [reportData, setReportData] = useState(null);

    const lastFetchedSprintProjectIdRef = useRef(null);
    const lastFetchedReportProjectIdRef = useRef(null);

    // 1. Tải danh sách Sprint khi projectId thay đổi (hoặc khi không truyền prop sprints)
    const loadSprints = (targetProjectId) => {
        if (!targetProjectId) return;
        lastFetchedSprintProjectIdRef.current = targetProjectId;
        getProjectSprints(targetProjectId)
            .then((data) => {
                if (data) setSprintList(data);
            })
            .catch((err) => {
                // Reset ref nếu tải lỗi để người dùng có thể retry
                lastFetchedSprintProjectIdRef.current = null;
                setError(err.message || "Không thể tải danh sách Sprint.");
            });
    };

    useEffect(() => {
        if (Array.isArray(sprints) && sprints.length > 0) {
            setSprintList(sprints);
            return;
        }

        if (!projectId || lastFetchedSprintProjectIdRef.current === projectId) {
            return;
        }

        loadSprints(projectId);
    }, [projectId, sprints]);

    // 2. Hàm gọi API lấy báo cáo tổng hợp
    const fetchData = async (filters = {}) => {
        if (!projectId) return;
        setLoading(true);
        setError(null);

        const sprintIdToUse =
            filters.sprintId !== undefined
                ? filters.sprintId
                : selectedSprintId;
        const fromDateToUse =
            filters.fromDate !== undefined ? filters.fromDate : fromDate;
        const toDateToUse =
            filters.toDate !== undefined ? filters.toDate : toDate;

        try {
            const data = await getMemberContributions(projectId, {
                sprintId: sprintIdToUse || undefined,
                fromDate: fromDateToUse || undefined,
                toDate: toDateToUse || undefined,
            });
            setReportData(data);
        } catch (err) {
            setError(err.message || "Máy chủ báo lỗi khi tải dữ liệu.");
            setReportData(null);
        } finally {
            setLoading(false);
        }
    };

    // 3. Khởi tạo dữ liệu khi mở trang hoặc khi đổi sang projectId khác
    useEffect(() => {
        if (!projectId || lastFetchedReportProjectIdRef.current === projectId) {
            return;
        }
        lastFetchedReportProjectIdRef.current = projectId;
        fetchData();
    }, [projectId]);

    // 4. Xử lý nút Áp dụng (Client-side validation chặn fromDate >= toDate)
    const handleApply = (e) => {
        e.preventDefault();
        if (fromDate && toDate && fromDate >= toDate) {
            setDateError("Ngày bắt đầu phải nhỏ hơn ngày kết thúc.");
            return;
        }
        setDateError("");
        fetchData({ sprintId: selectedSprintId, fromDate, toDate });
    };

    // 5. Xử lý nút Đặt lại
    const handleReset = () => {
        setSelectedSprintId("");
        setFromDate("");
        setToDate("");
        setDateError("");
        fetchData({ sprintId: "", fromDate: "", toDate: "" });
    };

    // 6. Xử lý nút Thử lại: retry cả Sprint nếu trước đó fail, sau đó retry Report
    const handleRetry = () => {
        if (sprintList.length === 0 && !Array.isArray(sprints)) {
            loadSprints(projectId);
        }
        fetchData();
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
                        Thống kê công việc liên kết, commit và pull request
                        tương ứng theo Sprint hoặc mốc thời gian
                    </p>
                </div>

                <form
                    onSubmit={handleApply}
                    style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "flex-start",
                        gap: "12px",
                        flexWrap: "nowrap",
                        overflowX: "auto",
                        width: "100%",
                        paddingBottom: "4px",
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                            flexShrink: 0,
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
                                minWidth: "120px",
                                padding: "6px 10px",
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
                            flexShrink: 0,
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
                            onChange={(e) => {
                                setFromDate(e.target.value);
                                setDateError("");
                            }}
                            style={{
                                padding: "5px 8px",
                                fontSize: "13px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                outline: "none",
                                width: "130px",
                            }}
                        />
                    </div>

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "6px",
                            flexShrink: 0,
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
                            Đến:
                        </span>
                        <input
                            aria-label="Đến ngày"
                            type="date"
                            value={toDate}
                            onChange={(e) => {
                                setToDate(e.target.value);
                                setDateError("");
                            }}
                            style={{
                                padding: "5px 8px",
                                fontSize: "13px",
                                borderRadius: "6px",
                                border: "1px solid #cbd5e1",
                                outline: "none",
                                width: "130px",
                            }}
                        />
                    </div>

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "8px",
                            flexShrink: 0,
                            marginLeft: "4px",
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
                                backgroundColor: "#ffffff",
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

                {dateError && (
                    <div
                        role="alert"
                        style={{
                            marginTop: "8px",
                            color: "#dc2626",
                            fontSize: "12px",
                            fontWeight: 500,
                        }}
                    >
                        {dateError}
                    </div>
                )}
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
                        onClick={handleRetry}
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
                                    Tasks liên kết
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

                                    const isLinked = Boolean(
                                        m.githubLinked ||
                                        m.githubUsername ||
                                        m.externalAccountId,
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
                                                {isLinked ? (
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
                                                        {m.githubUsername
                                                            ? `@${m.githubUsername}`
                                                            : "Đã liên kết"}
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
