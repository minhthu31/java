import React, { useState, useEffect, useCallback } from "react";
import { TaskService } from "./TaskService";
import { currentUser } from "./authService";

const ISSUE_TYPES = [
    "Feature",
    "Bug",
    "Test",
    "Docs",
    "Refactor",
    "Logging",
    "Deployment",
    "Research",
    "UI-UX",
];
const PRIORITIES = ["HIGHEST", "HIGH", "MEDIUM", "LOW", "LOWEST"];
const TASK_STATUSES = ["TODO", "IN_PROGRESS", "IN_REVIEW", "BLOCKED", "DONE"];

export default function TaskComponent({ projectId }) {
    const user = currentUser() || {};
    const userRole = user.role
        ? String(user.role).replace("ROLE_", "").toUpperCase()
        : null;

    // Phân quyền chuẩn: Chỉ Trưởng nhóm (Leader) được tạo Task
    const isLeader = userRole === "TEAM_LEADER";

    const [currentView, setCurrentView] = useState("list");
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [metadata, setMetadata] = useState({
        assignees: [],
        sprints: [],
        features: [],
    });
    const [selectedTask, setSelectedTask] = useState(null);

    const [formData, setFormData] = useState({
        title: "",
        description: "",
        acceptance_criteria: "",
        issue_type: "Feature",
        priority: "MEDIUM",
        deadline: "",
        assignee_user_id: "",
        sprint_id: "",
        feature_id: "",
    });
    const [formSubmitting, setFormSubmitting] = useState(false);
    const [formError, setFormError] = useState(null);

    const fetchTasks = useCallback(async () => {
        if (!projectId) {
            setTasks([]);
            setMetadata({ assignees: [], sprints: [], features: [] });
            setLoading(false);
            setError(null);
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const data = await TaskService.getTasks(projectId);
            setTasks(Array.isArray(data) ? data : []);
            try {
                const meta = await TaskService.getTaskMetadata(projectId);
                setMetadata(
                    meta || { assignees: [], sprints: [], features: [] },
                );
            } catch {
                setMetadata({ assignees: [], sprints: [], features: [] });
            }
        } catch (err) {
            const status = err.response?.status;
            if (status === 401) {
                setError("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
            } else if (status === 403) {
                setError(
                    "Bạn không có quyền truy cập danh sách Task của dự án này.",
                );
            } else if (status === 404) {
                setError("Không tìm thấy dữ liệu dự án hoặc danh sách Task.");
            } else {
                setError(
                    err.response?.data?.message ||
                        err.message ||
                        "Hệ thống gặp lỗi ngoài dự kiến",
                );
            }
        } finally {
            setLoading(false);
        }
    }, [projectId]);

    useEffect(() => {
        if (!projectId) {
            setTasks([]);
            setMetadata({ assignees: [], sprints: [], features: [] });
            setLoading(false);
            setError(null);
            return;
        }

        fetchTasks();
    }, [projectId, fetchTasks]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleCreateSubmit = async (e) => {
        e.preventDefault();

        if (formSubmitting) {
            return;
        }

        if (!projectId) {
            setFormError("Chưa chọn dự án. Không thể tạo Task.");
            return;
        }

        if (
            !formData.title.trim() ||
            !formData.acceptance_criteria.trim() ||
            !formData.deadline
        ) {
            setFormError(
                "Vui lòng điền đầy đủ: Tiêu đề, Acceptance Criteria và Deadline.",
            );
            return;
        }

        setFormSubmitting(true);
        setFormError(null);
        try {
            const payload = {
                ...formData,
                assignee_user_id: formData.assignee_user_id
                    ? Number(formData.assignee_user_id)
                    : null,
                sprint_id: formData.sprint_id
                    ? Number(formData.sprint_id)
                    : null,
                feature_id: formData.feature_id
                    ? Number(formData.feature_id)
                    : null,
            };

            const created = await TaskService.createTask(projectId, payload);
            setTasks((prev) => [created, ...prev]);
            setCurrentView("list");
            setFormData({
                title: "",
                description: "",
                acceptance_criteria: "",
                issue_type: "Feature",
                priority: "MEDIUM",
                deadline: "",
                assignee_user_id: "",
                sprint_id: "",
                feature_id: "",
            });
        } catch (err) {
            const status = err.response?.status;
            if (status === 401) {
                setFormError("Phiên làm việc hết hạn khi tạo Task.");
            } else if (status === 403) {
                setFormError("Chỉ Trưởng nhóm (Leader) mới có quyền tạo Task.");
            } else if (status === 404) {
                setFormError("Không tìm thấy dự án tương ứng.");
            } else {
                setFormError(
                    err.response?.data?.message ||
                        err.message ||
                        "Có lỗi xảy ra khi tạo Task.",
                );
            }
        } finally {
            setFormSubmitting(false);
        }
    };

    const handleStatusChange = async (taskId, newStatus) => {
        try {
            await TaskService.updateTaskStatus(projectId, taskId, newStatus);
            setTasks((prev) =>
                prev.map((t) =>
                    t.id === taskId ? { ...t, status: newStatus } : t,
                ),
            );
            if (selectedTask && selectedTask.id === taskId) {
                setSelectedTask((prev) => ({ ...prev, status: newStatus }));
            }
        } catch (err) {
            alert(
                err.response?.data?.message || "Không thể cập nhật trạng thái.",
            );
        }
    };

    const renderSyncBadge = (status) => {
        const syncStatus = status || "NOT_SYNCED";
        if (syncStatus === "SYNCED") {
            return (
                <span
                    data-testid="sync-badge"
                    style={{
                        padding: "3px 8px",
                        fontSize: "11px",
                        fontWeight: "bold",
                        borderRadius: "4px",
                        background: "#dcfce7",
                        color: "#15803d",
                        border: "1px solid #86efac",
                    }}
                >
                    SYNCED
                </span>
            );
        }
        if (syncStatus === "SYNCING") {
            return (
                <span
                    data-testid="sync-badge"
                    style={{
                        padding: "3px 8px",
                        fontSize: "11px",
                        fontWeight: "bold",
                        borderRadius: "4px",
                        background: "#dbeafe",
                        color: "#1d4ed8",
                        border: "1px solid #93c5fd",
                    }}
                >
                    SYNCING
                </span>
            );
        }
        if (syncStatus === "SYNC_FAILED") {
            return (
                <span
                    data-testid="sync-badge"
                    style={{
                        padding: "3px 8px",
                        fontSize: "11px",
                        fontWeight: "bold",
                        borderRadius: "4px",
                        background: "#fee2e2",
                        color: "#b91c1c",
                        border: "1px solid #fca5a5",
                    }}
                >
                    SYNC_FAILED
                </span>
            );
        }
        return (
            <span
                data-testid="sync-badge"
                style={{
                    padding: "3px 8px",
                    fontSize: "11px",
                    fontWeight: "bold",
                    borderRadius: "4px",
                    background: "#fef3c7",
                    color: "#92400e",
                    border: "1px solid #fcd34d",
                }}
            >
                NOT_SYNCED
            </span>
        );
    };

    return (
        <div
            style={{
                background: "#fff",
                borderRadius: "8px",
                padding: "24px",
                border: "1px solid #e2e8f0",
            }}
        >
            {currentView === "create" ? (
                <div>
                    <div
                        style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            marginBottom: "20px",
                            paddingBottom: "12px",
                            borderBottom: "1px solid #e2e8f0",
                        }}
                    >
                        <h2
                            style={{
                                fontSize: "18px",
                                fontWeight: "bold",
                                margin: 0,
                                color: "#1e293b",
                            }}
                        >
                            Tạo mới Task
                        </h2>
                        <button
                            type="button"
                            onClick={() => setCurrentView("list")}
                            style={{
                                padding: "8px 18px",
                                background: "#f1f5f9",
                                border: "1px solid #94a3b8",
                                borderRadius: "6px",
                                color: "#1e293b",
                                fontWeight: 600,
                                fontSize: "13px",
                                cursor: "pointer",
                            }}
                        >
                            ← Quay lại danh sách
                        </button>
                    </div>

                    {formError && (
                        <div
                            data-testid="form-error"
                            style={{
                                padding: "12px",
                                background: "#fee2e2",
                                color: "#991b1b",
                                borderRadius: "6px",
                                marginBottom: "16px",
                                border: "1px solid #f87171",
                            }}
                        >
                            {formError}
                        </div>
                    )}

                    <form
                        onSubmit={handleCreateSubmit}
                        style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "14px",
                        }}
                    >
                        <div>
                            <label
                                htmlFor="task-title"
                                style={{
                                    display: "block",
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    marginBottom: "4px",
                                    color: "#334155",
                                }}
                            >
                                Tiêu đề (Title) *
                            </label>
                            <input
                                id="task-title"
                                type="text"
                                name="title"
                                value={formData.title}
                                onChange={handleInputChange}
                                placeholder="Nhập tiêu đề task..."
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
                                    borderRadius: "6px",
                                    border: "1px solid #cbd5e1",
                                    boxSizing: "border-box",
                                }}
                            />
                        </div>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr 1fr",
                                gap: "14px",
                            }}
                        >
                            <div>
                                <label
                                    htmlFor="task-issue-type"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Loại công việc (Issue Type) *
                                </label>
                                <select
                                    id="task-issue-type"
                                    name="issue_type"
                                    value={formData.issue_type}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        background: "#fff",
                                        boxSizing: "border-box",
                                    }}
                                >
                                    {ISSUE_TYPES.map((t) => (
                                        <option key={t} value={t}>
                                            {t}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label
                                    htmlFor="task-priority"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Độ ưu tiên (Priority) *
                                </label>
                                <select
                                    id="task-priority"
                                    name="priority"
                                    value={formData.priority}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        background: "#fff",
                                        boxSizing: "border-box",
                                    }}
                                >
                                    {PRIORITIES.map((p) => (
                                        <option key={p} value={p}>
                                            {p}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr 1fr",
                                gap: "14px",
                            }}
                        >
                            <div>
                                <label
                                    htmlFor="task-assignee"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Người thực hiện (Assignee)
                                </label>
                                <select
                                    id="task-assignee"
                                    name="assignee_user_id"
                                    value={formData.assignee_user_id}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        background: "#fff",
                                        boxSizing: "border-box",
                                    }}
                                >
                                    <option value="">
                                        -- Chọn thành viên --
                                    </option>
                                    {metadata.assignees?.map((u) => (
                                        <option key={u.id} value={u.id}>
                                            {u.full_name || u.username}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label
                                    htmlFor="task-deadline"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Hạn chót (Deadline) *
                                </label>
                                <input
                                    id="task-deadline"
                                    type="date"
                                    name="deadline"
                                    value={formData.deadline}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        boxSizing: "border-box",
                                    }}
                                />
                            </div>
                        </div>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr 1fr",
                                gap: "14px",
                            }}
                        >
                            <div>
                                <label
                                    htmlFor="task-sprint"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Sprint
                                </label>
                                <select
                                    id="task-sprint"
                                    name="sprint_id"
                                    value={formData.sprint_id}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        background: "#fff",
                                        boxSizing: "border-box",
                                    }}
                                >
                                    <option value="">-- Chọn Sprint --</option>
                                    {metadata.sprints?.map((s) => (
                                        <option key={s.id} value={s.id}>
                                            {s.name || `Sprint ${s.id}`}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label
                                    htmlFor="task-feature"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Feature
                                </label>
                                <select
                                    id="task-feature"
                                    name="feature_id"
                                    value={formData.feature_id}
                                    onChange={handleInputChange}
                                    style={{
                                        width: "100%",
                                        padding: "8px 12px",
                                        borderRadius: "6px",
                                        border: "1px solid #cbd5e1",
                                        background: "#fff",
                                        boxSizing: "border-box",
                                    }}
                                >
                                    <option value="">-- Chọn Feature --</option>
                                    {metadata.features?.map((f) => (
                                        <option key={f.id} value={f.id}>
                                            {f.title || `Feature #${f.id}`}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div>
                            <label
                                htmlFor="task-acceptance"
                                style={{
                                    display: "block",
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    marginBottom: "4px",
                                    color: "#334155",
                                }}
                            >
                                Tiêu chí nghiệm thu (Acceptance Criteria) *
                            </label>
                            <textarea
                                id="task-acceptance"
                                name="acceptance_criteria"
                                value={formData.acceptance_criteria}
                                onChange={handleInputChange}
                                rows="3"
                                placeholder="Nhập tiêu chí nghiệm thu..."
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
                                    borderRadius: "6px",
                                    border: "1px solid #cbd5e1",
                                    boxSizing: "border-box",
                                }}
                            />
                        </div>

                        <div>
                            <label
                                htmlFor="task-description"
                                style={{
                                    display: "block",
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    marginBottom: "4px",
                                    color: "#334155",
                                }}
                            >
                                Mô tả (Description)
                            </label>
                            <textarea
                                id="task-description"
                                name="description"
                                value={formData.description}
                                onChange={handleInputChange}
                                rows="2"
                                placeholder="Mô tả kỹ thuật chi tiết..."
                                style={{
                                    width: "100%",
                                    padding: "8px 12px",
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
                                marginTop: "12px",
                            }}
                        >
                            <button
                                type="button"
                                onClick={() => setCurrentView("list")}
                                style={{
                                    padding: "9px 20px",
                                    background: "#f1f5f9",
                                    border: "1px solid #94a3b8",
                                    borderRadius: "6px",
                                    color: "#1e293b",
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    cursor: "pointer",
                                }}
                            >
                                Hủy
                            </button>
                            <button
                                type="submit"
                                disabled={formSubmitting}
                                style={{
                                    padding: "9px 24px",
                                    background: "#1d4ed8",
                                    color: "#ffffff",
                                    border: "none",
                                    borderRadius: "6px",
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    cursor: formSubmitting
                                        ? "not-allowed"
                                        : "pointer",
                                    opacity: formSubmitting ? 0.7 : 1,
                                }}
                            >
                                {formSubmitting ? "Đang lưu..." : "Lưu Task"}
                            </button>
                        </div>
                    </form>
                </div>
            ) : (
                <div>
                    <div
                        style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            marginBottom: "20px",
                        }}
                    >
                        <div>
                            <h2
                                style={{
                                    fontSize: "20px",
                                    fontWeight: "bold",
                                    margin: 0,
                                    color: "#0f172a",
                                }}
                            >
                                Danh sách công việc (Tasks)
                            </h2>
                            <p
                                style={{
                                    fontSize: "13px",
                                    color: "#64748b",
                                    margin: "4px 0 0 0",
                                }}
                            >
                                Theo dõi tiến độ, phân công và trạng thái đồng
                                bộ Jira
                            </p>
                        </div>
                        {isLeader && (
                            <button
                                type="button"
                                onClick={() => setCurrentView("create")}
                                data-testid="create-task-btn"
                                style={{
                                    padding: "10px 20px",
                                    background: "#1d4ed8",
                                    color: "#fff",
                                    border: "none",
                                    borderRadius: "6px",
                                    fontWeight: 600,
                                    cursor: "pointer",
                                    fontSize: "13px",
                                }}
                            >
                                + Tạo Task mới
                            </button>
                        )}
                    </div>

                    {error && !loading && (
                        <div
                            data-testid="error-message"
                            style={{
                                padding: "14px 18px",
                                background: "#fee2e2",
                                border: "1px solid #f87171",
                                borderRadius: "6px",
                                color: "#991b1b",
                                marginBottom: "16px",
                                display: "flex",
                                justifyContent: "space-between",
                                alignItems: "center",
                            }}
                        >
                            <span style={{ fontSize: "13px" }}>{error}</span>
                            <button
                                type="button"
                                onClick={fetchTasks}
                                data-testid="retry-btn"
                                style={{
                                    padding: "6px 14px",
                                    background: "#dc2626",
                                    color: "#fff",
                                    border: "none",
                                    borderRadius: "4px",
                                    cursor: "pointer",
                                    fontSize: "12px",
                                    fontWeight: "bold",
                                }}
                            >
                                Thử lại
                            </button>
                        </div>
                    )}

                    {loading && (
                        <div
                            data-testid="loading-state"
                            style={{
                                padding: "40px",
                                textAlign: "center",
                                color: "#64748b",
                                fontSize: "14px",
                            }}
                        >
                            Đang tải danh sách công việc...
                        </div>
                    )}

                    {!loading && !error && tasks.length === 0 && (
                        <div
                            data-testid="empty-state"
                            style={{
                                padding: "40px",
                                textAlign: "center",
                                background: "#f8fafc",
                                border: "2px dashed #cbd5e1",
                                borderRadius: "8px",
                            }}
                        >
                            <p
                                style={{
                                    color: "#64748b",
                                    fontSize: "14px",
                                    margin: 0,
                                }}
                            >
                                Chưa có công việc nào trong dự án này.
                            </p>
                        </div>
                    )}

                    {!loading && !error && tasks.length > 0 && (
                        <div
                            style={{
                                overflowX: "auto",
                                border: "1px solid #e2e8f0",
                                borderRadius: "8px",
                            }}
                        >
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
                                            background: "#f8fafc",
                                            borderBottom: "1px solid #e2e8f0",
                                            color: "#475569",
                                            fontSize: "12px",
                                            textTransform: "uppercase",
                                        }}
                                    >
                                        <th style={{ padding: "12px" }}>
                                            Tiêu đề / Jira Key
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Type
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Priority
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Assignee
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Deadline
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Sync Jira
                                        </th>
                                        <th style={{ padding: "12px" }}>
                                            Trạng thái
                                        </th>
                                        <th
                                            style={{
                                                padding: "12px",
                                                textAlign: "right",
                                            }}
                                        >
                                            Hành động
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {tasks.map((task) => (
                                        <tr
                                            key={task.id}
                                            data-testid={`task-row-${task.id}`}
                                            style={{
                                                borderBottom:
                                                    "1px solid #f1f5f9",
                                            }}
                                        >
                                            <td style={{ padding: "12px" }}>
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        setSelectedTask(task)
                                                    }
                                                    style={{
                                                        background: "none",
                                                        border: "none",
                                                        color: "#1d4ed8",
                                                        fontWeight: 600,
                                                        cursor: "pointer",
                                                        padding: 0,
                                                        textAlign: "left",
                                                        fontSize: "13px",
                                                    }}
                                                >
                                                    {task.title}
                                                </button>
                                                <div
                                                    style={{
                                                        fontSize: "11px",
                                                        color: "#94a3b8",
                                                        fontFamily: "monospace",
                                                    }}
                                                >
                                                    {task.jira_issue_key ||
                                                        "Chưa gắn Jira Key"}
                                                </div>
                                            </td>
                                            <td style={{ padding: "12px" }}>
                                                <span
                                                    style={{
                                                        padding: "3px 8px",
                                                        background: "#f1f5f9",
                                                        borderRadius: "4px",
                                                        fontSize: "11px",
                                                        fontWeight: 600,
                                                    }}
                                                >
                                                    {task.issue_type}
                                                </span>
                                            </td>
                                            <td
                                                style={{
                                                    padding: "12px",
                                                    fontWeight: 600,
                                                }}
                                            >
                                                {task.priority}
                                            </td>
                                            <td style={{ padding: "12px" }}>
                                                {task.assignee_name ||
                                                    task.assignee_user_id ||
                                                    "Chưa gán"}
                                            </td>
                                            <td style={{ padding: "12px" }}>
                                                {task.deadline}
                                            </td>
                                            <td style={{ padding: "12px" }}>
                                                {renderSyncBadge(
                                                    task.sync_status,
                                                )}
                                            </td>
                                            <td style={{ padding: "12px" }}>
                                                <select
                                                    aria-label={`Trạng thái task ${task.id}`}
                                                    value={
                                                        task.status || "TODO"
                                                    }
                                                    onChange={(e) =>
                                                        handleStatusChange(
                                                            task.id,
                                                            e.target.value,
                                                        )
                                                    }
                                                    style={{
                                                        padding: "4px 8px",
                                                        fontSize: "12px",
                                                        borderRadius: "4px",
                                                        border: "1px solid #cbd5e1",
                                                        background: "#fff",
                                                    }}
                                                >
                                                    {TASK_STATUSES.map((st) => (
                                                        <option
                                                            key={st}
                                                            value={st}
                                                        >
                                                            {st}
                                                        </option>
                                                    ))}
                                                </select>
                                            </td>
                                            <td
                                                style={{
                                                    padding: "12px",
                                                    textAlign: "right",
                                                }}
                                            >
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        setSelectedTask(task)
                                                    }
                                                    style={{
                                                        padding: "5px 12px",
                                                        fontSize: "12px",
                                                        background: "#fff",
                                                        border: "1px solid #94a3b8",
                                                        borderRadius: "4px",
                                                        cursor: "pointer",
                                                        color: "#1e293b",
                                                        fontWeight: 500,
                                                    }}
                                                >
                                                    Xem chi tiết
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            )}

            {/* DETAIL MODAL */}
            {selectedTask && (
                <div
                    data-testid="detail-modal"
                    style={{
                        position: "fixed",
                        inset: 0,
                        background: "rgba(0,0,0,0.5)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        zIndex: 1000,
                        padding: "16px",
                    }}
                >
                    <div
                        style={{
                            background: "#fff",
                            borderRadius: "8px",
                            maxWidth: "600px",
                            width: "100%",
                            padding: "24px",
                            position: "relative",
                            maxHeight: "90vh",
                            overflowY: "auto",
                        }}
                    >
                        <button
                            type="button"
                            data-testid="close-modal-btn"
                            onClick={() => setSelectedTask(null)}
                            style={{
                                position: "absolute",
                                top: "16px",
                                right: "16px",
                                background: "none",
                                border: "none",
                                fontSize: "18px",
                                fontWeight: "bold",
                                cursor: "pointer",
                            }}
                        >
                            ✕
                        </button>
                        <div
                            style={{
                                display: "flex",
                                gap: "8px",
                                alignItems: "center",
                                marginBottom: "8px",
                            }}
                        >
                            <span
                                style={{
                                    padding: "3px 8px",
                                    background: "#dbeafe",
                                    color: "#1e40af",
                                    borderRadius: "4px",
                                    fontSize: "11px",
                                    fontWeight: "bold",
                                }}
                            >
                                {selectedTask.issue_type}
                            </span>
                            {renderSyncBadge(selectedTask.sync_status)}
                        </div>
                        <h3
                            style={{
                                margin: "0 0 4px 0",
                                fontSize: "18px",
                                fontWeight: "bold",
                            }}
                        >
                            {selectedTask.title}
                        </h3>
                        <p
                            style={{
                                margin: "0 0 16px 0",
                                fontSize: "12px",
                                color: "#94a3b8",
                                fontFamily: "monospace",
                            }}
                        >
                            Jira Key:{" "}
                            {selectedTask.jira_issue_key || "Chưa liên kết"}
                        </p>

                        <div
                            style={{
                                display: "grid",
                                gridTemplateColumns: "1fr 1fr",
                                gap: "10px",
                                background: "#f8fafc",
                                padding: "12px",
                                borderRadius: "6px",
                                fontSize: "12px",
                                marginBottom: "16px",
                            }}
                        >
                            <div>
                                <strong>Độ ưu tiên:</strong>{" "}
                                {selectedTask.priority}
                            </div>
                            <div>
                                <strong>Trạng thái:</strong>{" "}
                                {selectedTask.status}
                            </div>
                            <div>
                                <strong>Người thực hiện:</strong>{" "}
                                {selectedTask.assignee_name ||
                                    selectedTask.assignee_user_id ||
                                    "Chưa gán"}
                            </div>
                            <div>
                                <strong>Hạn chót:</strong>{" "}
                                {selectedTask.deadline}
                            </div>
                            <div>
                                <strong>Sprint ID:</strong>{" "}
                                {selectedTask.sprint_id || "None"}
                            </div>
                            <div>
                                <strong>Feature ID:</strong>{" "}
                                {selectedTask.feature_id || "None"}
                            </div>
                        </div>

                        <div style={{ marginBottom: "12px" }}>
                            <div
                                style={{
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    marginBottom: "4px",
                                }}
                            >
                                Mô tả:
                            </div>
                            <div
                                style={{
                                    background: "#f8fafc",
                                    padding: "10px",
                                    borderRadius: "6px",
                                    fontSize: "12px",
                                    whiteSpace: "pre-wrap",
                                }}
                            >
                                {selectedTask.description || "Không có mô tả."}
                            </div>
                        </div>

                        <div style={{ marginBottom: "20px" }}>
                            <div
                                style={{
                                    fontWeight: 600,
                                    fontSize: "13px",
                                    marginBottom: "4px",
                                }}
                            >
                                Tiêu chí nghiệm thu (Acceptance Criteria):
                            </div>
                            <div
                                style={{
                                    background: "#fffbeb",
                                    border: "1px solid #fef3c7",
                                    padding: "10px",
                                    borderRadius: "6px",
                                    fontSize: "12px",
                                    whiteSpace: "pre-wrap",
                                    color: "#78350f",
                                }}
                            >
                                {selectedTask.acceptance_criteria ||
                                    "Không có tiêu chí nghiệm thu."}
                            </div>
                        </div>

                        <div
                            style={{
                                display: "flex",
                                justifyContent: "flex-end",
                            }}
                        >
                            <button
                                type="button"
                                onClick={() => setSelectedTask(null)}
                                style={{
                                    padding: "8px 20px",
                                    background: "#f1f5f9",
                                    border: "1px solid #94a3b8",
                                    borderRadius: "6px",
                                    cursor: "pointer",
                                    fontSize: "13px",
                                    fontWeight: 600,
                                    color: "#1e293b",
                                }}
                            >
                                Đóng
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
