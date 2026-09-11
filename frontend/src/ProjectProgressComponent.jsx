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

    const [progressData, setProgressData] = useState(null);
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
                const nextDay = new Date(`${appliedFilters.to}T00:00:00Z`);
                nextDay.setUTCDate(nextDay.getUTCDate() + 1);
                params.to = nextDay.toISOString().replace(".000Z", "Z");
            }

            const data = await progressService.getProjectProgress(
                projectId,
                params,
            );
            setProgressData(data);
        } catch (err) {
            setError("Không thể tải thông tin tiến độ dự án từ hệ thống.");
            setProgressData(null);
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
        if (fromDate && toDate && new Date(fromDate) > new Date(toDate)) {
            setError("Thời gian 'Từ' phải nhỏ hơn hoặc bằng thời gian 'Đến'.");
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

    const totalRequirements = progressData?.totalRequirements || 0;
    const totalFeatures = progressData?.totalFeatures || 0;
    const totalSprints = progressData?.totalSprints || 0;
    const totalTasks = progressData?.totalTasks || 0;
    const completedTasks = progressData?.completedTasks || 0;
    const overdueTasks = progressData?.overdueTasks || 0;
    const progressPercent = progressData?.progressPercent || 0;
    const sprintList = progressData?.sprints || [];

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
                    Theo dõi tổng quan tiến độ dự án, chỉ số Sprint và trạng
                    thái Task
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
            ) : totalTasks === 0 && totalRequirements === 0 ? (
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
                        Chưa có dữ liệu tiến độ
                    </div>
                    <p style={{ margin: "4px 0 0", fontSize: "13px" }}>
                        Chưa ghi nhận Requirement hoặc Task nào trong kỳ báo cáo
                        này.
                    </p>
                </div>
            ) : (
                <div>
                    {/* Progress Percent Bar */}
                    <div
                        style={{
                            padding: "20px 24px",
                            backgroundColor: "#ffffff",
                            borderRadius: "14px",
                            border: "1px solid #e2e8f0",
                            marginBottom: "24px",
                        }}
                    >
                        <div
                            style={{
                                display: "flex",
                                justifyContent: "space-between",
                                marginBottom: "8px",
                            }}
                        >
                            <span
                                style={{
                                    fontSize: "14px",
                                    fontWeight: 700,
                                    color: "#0f172a",
                                }}
                            >
                                Tiến độ hoàn thành dự án
                            </span>
                            <span
                                data-testid="metric-progress-percent"
                                style={{
                                    fontSize: "16px",
                                    fontWeight: 800,
                                    color: "#2563eb",
                                }}
                            >
                                {progressPercent}%
                            </span>
                        </div>
                        <div
                            style={{
                                width: "100%",
                                height: "10px",
                                backgroundColor: "#e2e8f0",
                                borderRadius: "999px",
                                overflow: "hidden",
                            }}
                        >
                            <div
                                data-testid="metric-progress-bar-fill"
                                style={{
                                    width: `${Math.min(Math.max(progressPercent, 0), 100)}%`,
                                    height: "100%",
                                    backgroundColor: "#2563eb",
                                    borderRadius: "999px",
                                }}
                            />
                        </div>
                    </div>

                    <div
                        style={{
                            display: "grid",
                            gridTemplateColumns:
                                "repeat(auto-fit, minmax(180px, 1fr))",
                            gap: "16px",
                            marginBottom: "28px",
                        }}
                    >
                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "18px 20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #6366f1",
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
                                Yêu cầu (Requirements)
                            </span>
                            <h2
                                data-testid="metric-total-requirements"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
                                    fontWeight: 800,
                                    color: "#0f172a",
                                }}
                            >
                                {totalRequirements}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "18px 20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #8b5cf6",
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
                                Tính năng (Features)
                            </span>
                            <h2
                                data-testid="metric-total-features"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
                                    fontWeight: 800,
                                    color: "#0f172a",
                                }}
                            >
                                {totalFeatures}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "18px 20px",
                                borderRadius: "14px",
                                border: "1px solid #e2e8f0",
                                borderLeft: "4px solid #0ea5e9",
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
                                Tổng Sprint
                            </span>
                            <h2
                                data-testid="metric-total-sprints"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
                                    fontWeight: 800,
                                    color: "#0f172a",
                                }}
                            >
                                {totalSprints}
                            </h2>
                        </div>

                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                padding: "18px 20px",
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
                                Tổng Task
                            </span>
                            <h2
                                data-testid="metric-total-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
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
                                padding: "18px 20px",
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
                                Task hoàn thành
                            </span>
                            <h2
                                data-testid="metric-completed-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
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
                                padding: "18px 20px",
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
                                Task quá hạn
                            </span>
                            <h2
                                data-testid="metric-overdue-tasks"
                                style={{
                                    margin: "8px 0 0",
                                    fontSize: "26px",
                                    fontWeight: 800,
                                    color: "#b91c1c",
                                }}
                            >
                                {overdueTasks}
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
                            Tiến độ từng Sprint
                        </h3>

                        {sprintList.length === 0 ? (
                            <div style={{ fontSize: "13px", color: "#64748b" }}>
                                Chưa có dữ liệu chi tiết cho Sprint.
                            </div>
                        ) : (
                            <div
                                style={{
                                    display: "flex",
                                    flexDirection: "column",
                                    gap: "12px",
                                }}
                            >
                                {sprintList.map((s) => (
                                    <div
                                        key={s.sprintId}
                                        data-testid={`sprint-progress-row-${s.sprintId}`}
                                        style={{
                                            padding: "14px 18px",
                                            border: "1px solid #e2e8f0",
                                            borderRadius: "10px",
                                            backgroundColor: "#ffffff",
                                        }}
                                    >
                                        <div
                                            style={{
                                                display: "flex",
                                                justifyContent: "space-between",
                                                marginBottom: "8px",
                                            }}
                                        >
                                            <span
                                                style={{
                                                    fontWeight: 600,
                                                    color: "#0f172a",
                                                    fontSize: "14px",
                                                }}
                                            >
                                                {s.sprintName ||
                                                    `Sprint ${s.sprintId}`}
                                            </span>
                                            <span
                                                data-testid={`sprint-percent-${s.sprintId}`}
                                                style={{
                                                    fontSize: "13px",
                                                    fontWeight: 700,
                                                    color: "#2563eb",
                                                }}
                                            >
                                                {s.progressPercent}%
                                            </span>
                                        </div>

                                        <div
                                            style={{
                                                width: "100%",
                                                height: "6px",
                                                backgroundColor: "#f1f5f9",
                                                borderRadius: "999px",
                                                overflow: "hidden",
                                                marginBottom: "10px",
                                            }}
                                        >
                                            <div
                                                style={{
                                                    width: `${Math.min(Math.max(s.progressPercent, 0), 100)}%`,
                                                    height: "100%",
                                                    backgroundColor: "#16a34a",
                                                    borderRadius: "999px",
                                                }}
                                            />
                                        </div>

                                        <div
                                            style={{
                                                display: "flex",
                                                gap: "16px",
                                                fontSize: "12px",
                                                color: "#64748b",
                                            }}
                                        >
                                            <span>
                                                Tổng:{" "}
                                                <strong
                                                    style={{ color: "#0f172a" }}
                                                >
                                                    {s.totalTasks}
                                                </strong>
                                            </span>
                                            <span>
                                                Hoàn thành:{" "}
                                                <strong
                                                    style={{ color: "#16a34a" }}
                                                >
                                                    {s.completedTasks}
                                                </strong>
                                            </span>
                                            <span>
                                                Quá hạn:{" "}
                                                <strong
                                                    style={{ color: "#dc2626" }}
                                                >
                                                    {s.overdueTasks}
                                                </strong>
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default ProjectProgressComponent;
