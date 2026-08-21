import React, { useState, useEffect, useCallback } from "react";
import { TaskService } from "./TaskService";
import { currentUser } from "./authService";

const FORM_ISSUE_TYPES = ["TASK", "EPIC", "STORY", "BUG"];
const PRIORITIES = ["HIGHEST", "HIGH", "MEDIUM", "LOW", "LOWEST"];
const CLASSIFICATIONS = [
    "FEATURE_RELATED",
    "NEW_FEATURE",
    "AUTO_TEST",
    "AUTO_LOG",
    "OTHER",
];

export default function TaskComponent({ projectId }) {
    const user = currentUser() || {};
    const userRole = user.role
        ? String(user.role).replace("ROLE_", "").toUpperCase()
        : null;

    // Phân quyền chuẩn Sprint 2: ADMIN không có quyền trong Task module
    const isLeader = userRole === "TEAM_LEADER";
    const isLecturer = userRole === "LECTURER";
    const isMember = userRole === "TEAM_MEMBER" || userRole === "STUDENT";
    const isValidRole = isLeader || isLecturer || isMember;

    const [currentView, setCurrentView] = useState("list");
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const [pageInfo, setPageInfo] = useState({
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
        first: true,
        last: true,
    });

    const [selectedTask, setSelectedTask] = useState(null);
    const [detailError, setDetailError] = useState(null);

    const [pendingStatusChange, setPendingStatusChange] = useState(null);
    const [statusReason, setStatusReason] = useState("");

    const [formData, setFormData] = useState({
        title: "",
        description: "",
        acceptanceCriteria: "",
        issueType: "TASK",
        classification: "FEATURE_RELATED",
        priority: "MEDIUM",
        deadline: "",
        assigneeUserId: "",
        requirementId: "",
        sprintId: "",
        featureId: "",
    });
    const [formSubmitting, setFormSubmitting] = useState(false);
    const [formError, setFormError] = useState(null);

    const fetchTasks = useCallback(
        async (page = 0) => {
            if (!projectId || !isValidRole) {
                setTasks([]);
                setLoading(false);
                if (!isValidRole && projectId) {
                    setError(
                        "Bạn không có quyền truy cập module Task của dự án này.",
                    );
                } else {
                    setError(null);
                }
                return;
            }

            setLoading(true);
            setError(null);
            try {
                const res = await TaskService.getTasks(projectId, {
                    page,
                    size: 20,
                });
                if (Array.isArray(res)) {
                    setTasks(res);
                    setPageInfo({
                        page: 0,
                        size: res.length,
                        totalPages: 1,
                        totalElements: res.length,
                        first: true,
                        last: true,
                    });
                } else if (res && Array.isArray(res.content)) {
                    setTasks(res.content);
                    setPageInfo({
                        page: res.page ?? page,
                        size: res.size ?? 20,
                        totalPages: res.totalPages ?? 1,
                        totalElements: res.totalElements ?? res.content.length,
                        first: res.first ?? page === 0,
                        last:
                            res.last ??
                            (res.totalPages
                                ? page >= res.totalPages - 1
                                : true),
                    });
                } else {
                    setTasks([]);
                }
            } catch (err) {
                const status = err.response?.status;
                if (status === 401) {
                    setError(
                        "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.",
                    );
                } else if (status === 403) {
                    setError(
                        "Bạn không có quyền truy cập danh sách Task của dự án này.",
                    );
                } else if (status === 404) {
                    setError(
                        "Không tìm thấy dữ liệu dự án hoặc danh sách Task.",
                    );
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
        },
        [projectId, isValidRole],
    );

    useEffect(() => {
        fetchTasks(0);
    }, [projectId, fetchTasks]);

    const handleOpenDetail = async (task) => {
        setDetailError(null);
        setSelectedTask(task);
        try {
            const fullTask = await TaskService.getTaskById(projectId, task.id);
            setSelectedTask(fullTask || task);
        } catch (err) {
            const msg =
                err.response?.data?.message ||
                err.message ||
                "Không thể tải thông tin chi tiết của task từ máy chủ.";
            setDetailError(msg);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value }));
    };

    const handleCreateSubmit = async (e) => {
        e.preventDefault();

        if (formSubmitting) return;
        if (!isLeader) {
            setFormError(
                "Chỉ Trưởng nhóm (Team Leader) mới có quyền tạo Task.",
            );
            return;
        }
        if (!projectId) {
            setFormError("Chưa chọn dự án. Không thể tạo Task.");
            return;
        }

        const trimmedTitle = formData.title.trim();
        const trimmedAcceptance = formData.acceptanceCriteria.trim();

        if (
            !trimmedTitle ||
            !trimmedAcceptance ||
            !formData.issueType ||
            !formData.priority
        ) {
            setFormError(
                "Vui lòng điền đầy đủ: Tiêu đề, Tiêu chí nghiệm thu, Issue Type và Priority.",
            );
            return;
        }

        if (trimmedTitle.length > 255) {
            setFormError("Tiêu đề không được vượt quá 255 ký tự.");
            return;
        }

        if (formData.deadline) {
            const selectedDate = new Date(formData.deadline);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            if (selectedDate < today) {
                setFormError("Hạn chót (Deadline) không được ở trong quá khứ.");
                return;
            }
        }

        setFormSubmitting(true);
        setFormError(null);
        try {
            const payload = {
                title: trimmedTitle,
                description: formData.description?.trim() || null,
                acceptanceCriteria: trimmedAcceptance,
                issueType: formData.issueType,
                classification: formData.classification || "FEATURE_RELATED",
                priority: formData.priority,
                deadline: formData.deadline
                    ? new Date(
                          `${formData.deadline}T23:59:59.000Z`,
                      ).toISOString()
                    : null,
                assigneeUserId: formData.assigneeUserId
                    ? Number(formData.assigneeUserId)
                    : null,
                requirementId: formData.requirementId
                    ? Number(formData.requirementId)
                    : null,
                sprintId: formData.sprintId ? Number(formData.sprintId) : null,
                featureId: formData.featureId
                    ? Number(formData.featureId)
                    : null,
            };

            await TaskService.createTask(projectId, payload);
            setCurrentView("list");
            fetchTasks(0);
            setFormData({
                title: "",
                description: "",
                acceptanceCriteria: "",
                issueType: "TASK",
                classification: "FEATURE_RELATED",
                priority: "MEDIUM",
                deadline: "",
                assigneeUserId: "",
                requirementId: "",
                sprintId: "",
                featureId: "",
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

    const isTaskAssignedToCurrentMember = (task) => {
        if (!isMember) return false;
        if (!task) return false;
        if (task.assigneeUserId && user.id && task.assigneeUserId === user.id)
            return true;
        if (task.assignee && typeof task.assignee === "object") {
            if (task.assignee.id && user.id && task.assignee.id === user.id)
                return true;
            if (
                task.assignee.username &&
                user.username &&
                task.assignee.username === user.username
            )
                return true;
        }
        return false;
    };

    const getAllowedStatusesForRole = (currentStatus, task) => {
        if (!isValidRole || isLecturer) return [];
        if (isMember && !isTaskAssignedToCurrentMember(task)) return [];
        if (currentStatus === "DONE" || currentStatus === "CANCELLED") {
            return [currentStatus];
        }

        let transitions = [];
        switch (currentStatus) {
            case "TO_DO":
                transitions = ["TO_DO", "IN_PROGRESS", "BLOCKED"];
                if (isLeader) transitions.push("CANCELLED");
                break;
            case "IN_PROGRESS":
                transitions = ["IN_PROGRESS", "TO_DO", "IN_REVIEW", "BLOCKED"];
                if (isLeader) transitions.push("CANCELLED");
                break;
            case "IN_REVIEW":
                transitions = ["IN_REVIEW", "IN_PROGRESS", "DONE", "BLOCKED"];
                break;
            case "BLOCKED":
                transitions = ["BLOCKED", "TO_DO", "IN_PROGRESS"];
                if (isLeader) transitions.push("CANCELLED");
                break;
            default:
                transitions = [currentStatus];
        }
        return transitions;
    };

    const handleStatusSelectChange = (task, newStatus) => {
        const taskId = task.id;
        const currentStatus = task.status || "TO_DO";
        if (newStatus === currentStatus) return;

        if (currentStatus === "DONE" || currentStatus === "CANCELLED") {
            alert("Trạng thái này là trạng thái kết thúc, không thể thay đổi.");
            return;
        }

        const allowedStatuses = getAllowedStatusesForRole(currentStatus, task);
        if (!allowedStatuses.includes(newStatus)) {
            alert("Không được phép chuyển sang trạng thái này.");
            return;
        }

        if (isMember && newStatus === "CANCELLED") {
            alert("Thành viên nhóm không có quyền hủy (CANCELLED) task.");
            return;
        }

        if (newStatus === "BLOCKED" || newStatus === "CANCELLED") {
            setPendingStatusChange({ taskId, newStatus });
            setStatusReason("");
            return;
        }

        executeStatusChange(taskId, newStatus, "");
    };

    const executeStatusChange = async (taskId, newStatus, reason) => {
        try {
            await TaskService.updateTaskStatus(
                projectId,
                taskId,
                newStatus,
                reason,
            );
            setTasks((prev) =>
                prev.map((t) =>
                    t.id === taskId ? { ...t, status: newStatus } : t,
                ),
            );
            if (selectedTask && selectedTask.id === taskId) {
                setSelectedTask((prev) => ({ ...prev, status: newStatus }));
            }
            setPendingStatusChange(null);
            setStatusReason("");
        } catch (err) {
            alert(
                err.response?.data?.message || "Không thể cập nhật trạng thái.",
            );
        }
    };

    const renderSyncBadge = (status) => {
        const syncStatus = status || "NOT_SYNCED";
        switch (syncStatus) {
            case "SYNCED":
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
            case "SYNCING":
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
            case "PENDING":
                return (
                    <span
                        data-testid="sync-badge"
                        style={{
                            padding: "3px 8px",
                            fontSize: "11px",
                            fontWeight: "bold",
                            borderRadius: "4px",
                            background: "#e0e7ff",
                            color: "#4338ca",
                            border: "1px solid #c7d2fe",
                        }}
                    >
                        PENDING
                    </span>
                );
            case "FAILED":
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
                        FAILED
                    </span>
                );
            default:
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
        }
    };

    const getAssigneeName = (task) => {
        if (!task?.assignee) return "Chưa gán";
        if (typeof task.assignee === "object") {
            return (
                task.assignee.displayName ||
                task.assignee.fullName ||
                task.assignee.username ||
                "Chưa gán"
            );
        }
        return task.assignee;
    };

    const formatDeadlineDisplay = (deadline) => {
        if (!deadline) return "Không có";
        try {
            return new Date(deadline).toLocaleDateString("vi-VN");
        } catch {
            return deadline;
        }
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
                                placeholder="Nhập tiêu đề task (tối đa 255 ký tự)..."
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
                                    name="issueType"
                                    value={formData.issueType}
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
                                    {FORM_ISSUE_TYPES.map((t) => (
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
                                    htmlFor="task-classification"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Phân loại (Classification)
                                </label>
                                <select
                                    id="task-classification"
                                    name="classification"
                                    value={formData.classification}
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
                                    {CLASSIFICATIONS.map((c) => (
                                        <option key={c} value={c}>
                                            {c}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label
                                    htmlFor="task-assignee-user-id"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    ID Người thực hiện (Assignee User ID)
                                </label>
                                <input
                                    id="task-assignee-user-id"
                                    type="number"
                                    name="assigneeUserId"
                                    value={formData.assigneeUserId}
                                    onChange={handleInputChange}
                                    placeholder="Nhập ID thành viên..."
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
                                    htmlFor="task-deadline"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Hạn chót (Deadline - tùy chọn)
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
                            <div>
                                <label
                                    htmlFor="task-requirement-id"
                                    style={{
                                        display: "block",
                                        fontWeight: 600,
                                        fontSize: "13px",
                                        marginBottom: "4px",
                                        color: "#334155",
                                    }}
                                >
                                    Mã Requirement liên kết (Requirement ID)
                                </label>
                                <input
                                    id="task-requirement-id"
                                    type="number"
                                    name="requirementId"
                                    value={formData.requirementId}
                                    onChange={handleInputChange}
                                    placeholder="Ví dụ: 101"
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
                                name="acceptanceCriteria"
                                value={formData.acceptanceCriteria}
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
                            {isValidRole && (
                                <button
                                    type="button"
                                    onClick={() => fetchTasks(pageInfo.page)}
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
                            )}
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

                    {!loading &&
                        !error &&
                        tasks.length === 0 &&
                        isValidRole && (
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

                    {!loading && !error && tasks.length > 0 && isValidRole && (
                        <>
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
                                                borderBottom:
                                                    "1px solid #e2e8f0",
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
                                        {tasks.map((task) => {
                                            const currentStatus =
                                                task.status || "TO_DO";
                                            const allowedStatuses =
                                                getAllowedStatusesForRole(
                                                    currentStatus,
                                                    task,
                                                );
                                            const isTerminal =
                                                currentStatus === "DONE" ||
                                                currentStatus === "CANCELLED";
                                            const canEditStatus =
                                                allowedStatuses.length > 1 &&
                                                !isTerminal &&
                                                !isLecturer;

                                            return (
                                                <tr
                                                    key={task.id}
                                                    data-testid={`task-row-${task.id}`}
                                                    style={{
                                                        borderBottom:
                                                            "1px solid #f1f5f9",
                                                    }}
                                                >
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        <button
                                                            type="button"
                                                            onClick={() =>
                                                                handleOpenDetail(
                                                                    task,
                                                                )
                                                            }
                                                            style={{
                                                                background:
                                                                    "none",
                                                                border: "none",
                                                                color: "#1d4ed8",
                                                                fontWeight: 600,
                                                                cursor: "pointer",
                                                                padding: 0,
                                                                textAlign:
                                                                    "left",
                                                                fontSize:
                                                                    "13px",
                                                            }}
                                                        >
                                                            {task.title}
                                                        </button>
                                                        <div
                                                            style={{
                                                                fontSize:
                                                                    "11px",
                                                                color: "#94a3b8",
                                                                fontFamily:
                                                                    "monospace",
                                                            }}
                                                        >
                                                            {task.jiraIssueKey ||
                                                                "Chưa gắn Jira Key"}
                                                        </div>
                                                    </td>
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        <span
                                                            style={{
                                                                padding:
                                                                    "3px 8px",
                                                                background:
                                                                    "#f1f5f9",
                                                                borderRadius:
                                                                    "4px",
                                                                fontSize:
                                                                    "11px",
                                                                fontWeight: 600,
                                                            }}
                                                        >
                                                            {task.issueType}
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
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        {getAssigneeName(task)}
                                                    </td>
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        {formatDeadlineDisplay(
                                                            task.deadline,
                                                        )}
                                                    </td>
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        {renderSyncBadge(
                                                            task.syncStatus,
                                                        )}
                                                    </td>
                                                    <td
                                                        style={{
                                                            padding: "12px",
                                                        }}
                                                    >
                                                        {!canEditStatus ? (
                                                            <span
                                                                style={{
                                                                    padding:
                                                                        "4px 8px",
                                                                    fontSize:
                                                                        "12px",
                                                                    fontWeight: 600,
                                                                    color:
                                                                        currentStatus ===
                                                                        "DONE"
                                                                            ? "#15803d"
                                                                            : currentStatus ===
                                                                                "CANCELLED"
                                                                              ? "#b91c1c"
                                                                              : "#475569",
                                                                }}
                                                            >
                                                                {currentStatus}
                                                            </span>
                                                        ) : (
                                                            <select
                                                                aria-label={`Trạng thái task ${task.id}`}
                                                                value={
                                                                    currentStatus
                                                                }
                                                                onChange={(e) =>
                                                                    handleStatusSelectChange(
                                                                        task,
                                                                        e.target
                                                                            .value,
                                                                    )
                                                                }
                                                                style={{
                                                                    padding:
                                                                        "4px 8px",
                                                                    fontSize:
                                                                        "12px",
                                                                    borderRadius:
                                                                        "4px",
                                                                    border: "1px solid #cbd5e1",
                                                                    background:
                                                                        "#fff",
                                                                }}
                                                            >
                                                                {allowedStatuses.map(
                                                                    (st) => (
                                                                        <option
                                                                            key={
                                                                                st
                                                                            }
                                                                            value={
                                                                                st
                                                                            }
                                                                        >
                                                                            {st}
                                                                        </option>
                                                                    ),
                                                                )}
                                                            </select>
                                                        )}
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
                                                                handleOpenDetail(
                                                                    task,
                                                                )
                                                            }
                                                            style={{
                                                                padding:
                                                                    "5px 12px",
                                                                fontSize:
                                                                    "12px",
                                                                background:
                                                                    "#fff",
                                                                border: "1px solid #94a3b8",
                                                                borderRadius:
                                                                    "4px",
                                                                cursor: "pointer",
                                                                color: "#1e293b",
                                                                fontWeight: 500,
                                                            }}
                                                        >
                                                            Xem chi tiết
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>

                            <div
                                data-testid="pagination"
                                style={{
                                    display: "flex",
                                    justifyContent: "space-between",
                                    alignItems: "center",
                                    marginTop: "16px",
                                    paddingTop: "12px",
                                    borderTop: "1px solid #e2e8f0",
                                }}
                            >
                                <span
                                    data-testid="page-info"
                                    style={{
                                        fontSize: "13px",
                                        color: "#64748b",
                                    }}
                                >
                                    Trang {pageInfo.page + 1} /{" "}
                                    {pageInfo.totalPages || 1} (Tổng cộng{" "}
                                    {pageInfo.totalElements} công việc)
                                </span>
                                <div style={{ display: "flex", gap: "8px" }}>
                                    <button
                                        type="button"
                                        data-testid="prev-page-btn"
                                        disabled={pageInfo.page <= 0}
                                        onClick={() =>
                                            fetchTasks(pageInfo.page - 1)
                                        }
                                        style={{
                                            padding: "6px 14px",
                                            fontSize: "12px",
                                            background: "#fff",
                                            border: "1px solid #cbd5e1",
                                            borderRadius: "4px",
                                            cursor:
                                                pageInfo.page <= 0
                                                    ? "not-allowed"
                                                    : "pointer",
                                            opacity:
                                                pageInfo.page <= 0 ? 0.5 : 1,
                                            fontWeight: 500,
                                        }}
                                    >
                                        ← Trang trước
                                    </button>
                                    <button
                                        type="button"
                                        data-testid="next-page-btn"
                                        disabled={
                                            pageInfo.page >=
                                            pageInfo.totalPages - 1
                                        }
                                        onClick={() =>
                                            fetchTasks(pageInfo.page + 1)
                                        }
                                        style={{
                                            padding: "6px 14px",
                                            fontSize: "12px",
                                            background: "#fff",
                                            border: "1px solid #cbd5e1",
                                            borderRadius: "4px",
                                            cursor:
                                                pageInfo.page >=
                                                pageInfo.totalPages - 1
                                                    ? "not-allowed"
                                                    : "pointer",
                                            opacity:
                                                pageInfo.page >=
                                                pageInfo.totalPages - 1
                                                    ? 0.5
                                                    : 1,
                                            fontWeight: 500,
                                        }}
                                    >
                                        Trang sau →
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </div>
            )}

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

                        {detailError && (
                            <div
                                data-testid="detail-error"
                                style={{
                                    padding: "10px 14px",
                                    background: "#fee2e2",
                                    color: "#991b1b",
                                    border: "1px solid #f87171",
                                    borderRadius: "6px",
                                    marginBottom: "14px",
                                    fontSize: "13px",
                                }}
                            >
                                ⚠️ {detailError}
                            </div>
                        )}

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
                                {selectedTask.issueType}
                            </span>
                            {renderSyncBadge(selectedTask.syncStatus)}
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
                            {selectedTask.jiraIssueKey || "Chưa liên kết"}
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
                                {getAssigneeName(selectedTask)}
                            </div>
                            <div>
                                <strong>Hạn chót:</strong>{" "}
                                {formatDeadlineDisplay(selectedTask.deadline)}
                            </div>
                            <div>
                                <strong>Requirement ID:</strong>{" "}
                                {selectedTask.requirementId || "None"}
                            </div>
                            <div>
                                <strong>Sprint ID:</strong>{" "}
                                {selectedTask.sprintId || "None"}
                            </div>
                            <div>
                                <strong>Feature ID:</strong>{" "}
                                {selectedTask.featureId || "None"}
                            </div>
                            <div>
                                <strong>Phân loại:</strong>{" "}
                                {selectedTask.classification ||
                                    "FEATURE_RELATED"}
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
                                {selectedTask.acceptanceCriteria ||
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

            {/* REASON MODAL CHO BLOCKED HOẶC CANCELLED */}
            {pendingStatusChange && (
                <div
                    data-testid="reason-modal"
                    style={{
                        position: "fixed",
                        inset: 0,
                        background: "rgba(0,0,0,0.5)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        zIndex: 1100,
                        padding: "16px",
                    }}
                >
                    <div
                        style={{
                            background: "#fff",
                            borderRadius: "8px",
                            maxWidth: "450px",
                            width: "100%",
                            padding: "20px",
                        }}
                    >
                        <h4
                            style={{
                                margin: "0 0 10px 0",
                                fontSize: "16px",
                                color: "#b91c1c",
                            }}
                        >
                            Nhập lý do chuyển sang{" "}
                            {pendingStatusChange.newStatus}
                        </h4>
                        <textarea
                            value={statusReason}
                            onChange={(e) => setStatusReason(e.target.value)}
                            placeholder="Vui lòng nhập lý do (bắt buộc)..."
                            rows="3"
                            style={{
                                width: "100%",
                                padding: "8px",
                                borderRadius: "4px",
                                border: "1px solid #cbd5e1",
                                boxSizing: "border-box",
                                marginBottom: "12px",
                            }}
                        />
                        <div
                            style={{
                                display: "flex",
                                justifyContent: "flex-end",
                                gap: "8px",
                            }}
                        >
                            <button
                                type="button"
                                onClick={() => setPendingStatusChange(null)}
                                style={{
                                    padding: "6px 14px",
                                    background: "#f1f5f9",
                                    border: "1px solid #94a3b8",
                                    borderRadius: "4px",
                                    cursor: "pointer",
                                }}
                            >
                                Hủy
                            </button>
                            <button
                                type="button"
                                onClick={() => {
                                    if (!statusReason.trim()) {
                                        alert("Vui lòng nhập lý do!");
                                        return;
                                    }
                                    executeStatusChange(
                                        pendingStatusChange.taskId,
                                        pendingStatusChange.newStatus,
                                        statusReason.trim(),
                                    );
                                }}
                                style={{
                                    padding: "6px 16px",
                                    background: "#dc2626",
                                    color: "#fff",
                                    border: "none",
                                    borderRadius: "4px",
                                    fontWeight: "bold",
                                    cursor: "pointer",
                                }}
                            >
                                Xác nhận
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
