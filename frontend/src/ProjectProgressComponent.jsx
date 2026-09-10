import React, { useState, useEffect, useCallback } from "react";
import { progressService } from "./progressService";

export function ProjectProgressComponent({ projectId, currentUserRole }) {
    const isAuthorized =
        currentUserRole === "TEAM_LEADER" || currentUserRole === "LECTURER";

    const [sprints, setSprints] = useState([]);
    const [selectedSprint, setSelectedSprint] = useState("");
    const [fromDate, setFromDate] = useState("");
    const [toDate, setToDate] = useState("");

    const [appliedFilters, setAppliedFilters] = useState({
        sprintId: "",
        from: "",
        to: "",
    });

    const [reportData, setReportData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!isAuthorized || !projectId) return;

        let isCurrent = true;
        progressService
            .getSprints(projectId)
            .then((data) => {
                if (isCurrent) {
                    setSprints(Array.isArray(data) ? data : []);
                }
            })
            .catch(() => {
                if (isCurrent) setSprints([]);
            });

        return () => {
            isCurrent = false;
        };
    }, [projectId, isAuthorized]);

    const fetchProgress = useCallback(async () => {
        if (!projectId) return;

        try {
            setLoading(true);
            setError(null);

            const params = {};
            if (appliedFilters.sprintId) {
                params.sprintId = appliedFilters.sprintId;
            }
            if (appliedFilters.from) {
                params.from = `${appliedFilters.from}T00:00:00Z`;
            }
            if (appliedFilters.to) {
                params.to = `${appliedFilters.to}T23:59:59Z`;
            }

            const data = await progressService.getProjectSummary(
                projectId,
                params,
            );
            setReportData(data);
        } catch (err) {
            setError("Không thể tải thông tin tiến độ dự án từ hệ thống.");
            setReportData(null);
        } finally {
            setLoading(false);
        }
    }, [projectId, appliedFilters]);

    useEffect(() => {
        if (isAuthorized && projectId) {
            fetchProgress();
        }
    }, [fetchProgress, isAuthorized, projectId]);

    const handleFilterSubmit = (e) => {
        e.preventDefault();
        if (fromDate && toDate && new Date(fromDate) >= new Date(toDate)) {
            setError("Thời gian 'Từ' phải nhỏ hơn thời gian 'Đến'.");
            return;
        }
        setAppliedFilters({
            sprintId: selectedSprint,
            from: fromDate,
            to: toDate,
        });
    };

    const handleResetFilter = () => {
        setSelectedSprint("");
        setFromDate("");
        setToDate("");
        setAppliedFilters({
            sprintId: "",
            from: "",
            to: "",
        });
    };

    if (!isAuthorized) {
        return (
            <div
                data-testid="unauthorized-progress-message"
                style={{
                    padding: "50px 24px",
                    textAlign: "center",
                    maxWidth: "460px",
                    margin: "40px auto",
                    backgroundColor: "#ffffff",
                    borderRadius: "14px",
                    border: "1px solid #fee2e2",
                }}
            >
                <h3
                    style={{
                        color: "#dc2626",
                        margin: "0 0 8px",
                        fontSize: "17px",
                        fontWeight: 700,
                    }}
                >
                    Không có quyền truy cập
                </h3>
                <p
                    style={{
                        margin: 0,
                        fontSize: "14px",
                        color: "#64748b",
                        lineHeight: 1.6,
                    }}
                >
                    Chỉ có <strong>Trưởng nhóm (TEAM_LEADER)</strong> và{" "}
                    <strong>Giảng viên (LECTURER)</strong> mới được phép theo
                    dõi tiến độ dự án.
                </p>
            </div>
        );
    }

    const taskMetrics = reportData?.taskMetrics;
    const totalTasks = taskMetrics?.totalTasks || 0;
    const completedTasks = taskMetrics?.completedTasks || 0;
    const overdueTasks = taskMetrics?.overdueTasks || 0;
    const statusMap = taskMetrics?.tasksByStatus || {};

    const unassignedTasks = taskMetrics?.unassignedTasks ?? 0;

    return (
        <div
            style={{ padding: "28px 32px" }}
            data-testid="project-progress-container"
        >
            <div style={{ marginBottom: "22px" }}>
                <h2
                    style={{
                        margin: "0 0 4px",
                        fontSize: "18px",
                        fontWeight: 700,
                        color: "#0f172a",
                    }}
                >
                    Tiến độ & Tình trạng Công việc
                </h2>
                <p style={{ margin: 0, fontSize: "13px", color: "#64748b" }}>
                    Theo dõi tổng quan các chỉ số Sprint, Task tồn đọng và trạng
                    thái thực hiện
                </p>
            </div>

            {error && (
                <div
                    data-testid="progress-error-banner"
                    style={{
                        padding: "12px 18px",
                        marginBottom: "20px",
                        backgroundColor: "#fef2f2",
                        border: "1px solid #fecaca",
                        borderRadius: "10px",
                        color: "#b91c1c",
                        fontSize: "13px",
                        fontWeight: 500,
                    }}
                >
                    {error}
                </div>
            )}

            <form
                onSubmit={handleFilterSubmit}
                style={{
                    display: "flex",
                    gap: "14px",
                    alignItems: "center",
                    flexWrap: "wrap",
                    marginBottom: "28px",
                    padding: "14px 20px",
                    backgroundColor: "#f8fafc",
                    borderRadius: "12px",
                    border: "1px solid #e2e8f0",
                }}
            >
                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "6px",
                    }}
                >
                    <label
                        htmlFor="filter-sprint"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                        }}
                    >
                        Sprint:
                    </label>
                    <select
                        id="filter-sprint"
                        value={selectedSprint}
                        onChange={(e) => setSelectedSprint(e.target.value)}
                        style={{
                            padding: "7px 12px",
                            borderRadius: "8px",
                            border: "1px solid #cbd5e1",
                            backgroundColor: "#ffffff",
                            fontSize: "13px",
                            color: "#1e293b",
                            outline: "none",
                            cursor: "pointer",
                        }}
                    >
                        <option value="">Tất cả Sprint</option>
                        {sprints.map((sprint) => (
                            <option key={sprint.id} value={sprint.id}>
                                {sprint.name || `Sprint ${sprint.id}`}
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
                    <label
                        htmlFor="filter-from-date"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                        }}
                    >
                        Từ:
                    </label>
                    <input
                        id="filter-from-date"
                        type="date"
                        value={fromDate}
                        onChange={(e) => setFromDate(e.target.value)}
                        style={{
                            padding: "6px 10px",
                            borderRadius: "8px",
                            border: "1px solid #cbd5e1",
                            backgroundColor: "#ffffff",
                            fontSize: "13px",
                            color: "#1e293b",
                            outline: "none",
                        }}
                    />
                </div>

                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "6px",
                    }}
                >
                    <label
                        htmlFor="filter-to-date"
                        style={{
                            fontSize: "13px",
                            fontWeight: 600,
                            color: "#334155",
                        }}
                    >
                        Đến:
                    </label>
                    <input
                        id="filter-to-date"
                        type="date"
                        value={toDate}
                        onChange={(e) => setToDate(e.target.value)}
                        style={{
                            padding: "6px 10px",
                            borderRadius: "8px",
                            border: "1px solid #cbd5e1",
                            backgroundColor: "#ffffff",
                            fontSize: "13px",
                            color: "#1e293b",
                            outline: "none",
                        }}
                    />
                </div>

                <button
                    type="submit"
                    style={{
                        padding: "7px 18px",
                        backgroundColor: "#2563eb",
                        color: "#ffffff",
                        border: "none",
                        borderRadius: "8px",
                        cursor: "pointer",
                        fontWeight: 600,
                        fontSize: "13px",
                    }}
                >
                    Áp dụng
                </button>

                <button
                    type="button"
                    onClick={handleResetFilter}
                    style={{
                        padding: "7px 14px",
                        backgroundColor: "#ffffff",
                        color: "#64748b",
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

            {loading ? (
                <div
                    data-testid="progress-loading-state"
                    style={{
                        padding: "70px 20px",
                        textAlign: "center",
                        color: "#64748b",
                        fontSize: "14px",
                        fontWeight: 500,
                    }}
                >
                    Đang tải dữ liệu tiến độ dự án...
                </div>
            ) : totalTasks === 0 ? (
                <div
                    data-testid="progress-empty-state"
                    style={{
                        padding: "50px 20px",
                        textAlign: "center",
                        backgroundColor: "#f8fafc",
                        border: "2px dashed #cbd5e1",
                        borderRadius: "14px",
                        color: "#64748b",
                    }}
                >
                    <div
                        style={{
                            fontSize: "15px",
                            fontWeight: 600,
                            color: "#1e293b",
                        }}
                    >
                        Chưa có công việc nào
                    </div>
                    <p style={{ margin: "4px 0 0", fontSize: "13px" }}>
                        Chưa có công việc nào được ghi nhận cho khoảng thời gian
                        hoặc Sprint đã chọn.
                    </p>
                </div>
            ) : (
                <div>
                    <div
                        style={{
                            display: "grid",
                            gridTemplateColumns:
                                "repeat(auto-fit, minmax(190px, 1fr))",
                            gap: "16px",
                            marginBottom: "28px",
                        }}
                    >
                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #2563eb",
                            }}
                        >
                            <span
                                style={{
                                    fontSize: "12px",
                                    fontWeight: 700,
                                    color: "#64748b",
                                    textTransform: "uppercase",
                                }}
                            >
                                Tổng số Task
                            </span>
                            <h2
                                data-testid="metric-total-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "28px",
                                    fontWeight: 800,
                                    color: "#0f172a",
                                }}
                            >
                                {totalTasks}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #16a34a",
                            }}
                        >
                            <span
                                style={{
                                    fontSize: "12px",
                                    fontWeight: 700,
                                    color: "#16a34a",
                                    textTransform: "uppercase",
                                }}
                            >
                                Đã hoàn thành
                            </span>
                            <h2
                                data-testid="metric-completed-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "28px",
                                    fontWeight: 800,
                                    color: "#15803d",
                                }}
                            >
                                {completedTasks}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #dc2626",
                            }}
                        >
                            <span
                                style={{
                                    fontSize: "12px",
                                    fontWeight: 700,
                                    color: "#dc2626",
                                    textTransform: "uppercase",
                                }}
                            >
                                Quá hạn (Overdue)
                            </span>
                            <h2
                                data-testid="metric-overdue-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "28px",
                                    fontWeight: 800,
                                    color: "#b91c1c",
                                }}
                            >
                                {overdueTasks}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #f59e0b",
                            }}
                        >
                            <span
                                style={{
                                    fontSize: "12px",
                                    fontWeight: 700,
                                    color: "#d97706",
                                    textTransform: "uppercase",
                                }}
                            >
                                Chưa phân công
                            </span>
                            <h2
                                data-testid="metric-unassigned-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "28px",
                                    fontWeight: 800,
                                    color: "#b45309",
                                }}
                            >
                                {unassignedTasks}
                            </h2>
                        </div>
                    </div>

                    <div
                        style={{
                            padding: "22px 24px",
                            backgroundColor: "#f8fafc",
                            border: "1px solid #e2e8f0",
                            borderRadius: "14px",
                        }}
                    >
                        <h3
                            style={{
                                margin: "0 0 16px",
                                fontSize: "14px",
                                fontWeight: 700,
                                color: "#1e293b",
                            }}
                        >
                            Số lượng công việc theo trạng thái
                        </h3>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns:
                                    "repeat(auto-fit, minmax(140px, 1fr))",
                                gap: "12px",
                            }}
                        >
                            {Object.entries(statusMap).map(
                                ([status, count]) => (
                                    <div
                                        key={status}
                                        style={{
                                            padding: "14px 18px",
                                            border: "1px solid #e2e8f0",
                                            borderRadius: "10px",
                                            backgroundColor: "#ffffff",
                                        }}
                                    >
                                        <div
                                            style={{
                                                fontSize: "11px",
                                                color: "#64748b",
                                                fontWeight: 700,
                                                letterSpacing: "0.5px",
                                            }}
                                        >
                                            {status}
                                        </div>
                                        <div
                                            data-testid={`status-count-${status}`}
                                            style={{
                                                fontSize: "24px",
                                                fontWeight: 800,
                                                color: "#0f172a",
                                                marginTop: "6px",
                                            }}
                                        >
                                            {count}
                                        </div>
                                    </div>
                                ),
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default ProjectProgressComponent;
