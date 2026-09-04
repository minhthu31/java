import React, { useCallback, useEffect, useState } from "react";
import { RequirementService } from "./RequirementService";
import RequirementForm from "./RequirementForm";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ["", "DRAFT", "APPROVED", "SYNCED", "ARCHIVED"];
const PRIORITY_OPTIONS = ["", "HIGHEST", "HIGH", "MEDIUM", "LOW", "LOWEST"];

const RequirementList = ({ projectId, currentUserRole }) => {
    const [requirements, setRequirements] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);

    const [keywordInput, setKeywordInput] = useState("");
    const [jiraIssueKeyInput, setJiraIssueKeyInput] = useState("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [priority, setPriority] = useState("");
    const [jiraIssueKey, setJiraIssueKey] = useState("");
    const [editingRequirementId, setEditingRequirementId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const canManage = currentUserRole === "TEAM_LEADER";
    const canView =
        currentUserRole === "TEAM_LEADER" || currentUserRole === "LECTURER";

    const fetchRequirements = useCallback(async () => {
        if (!canView) {
            setRequirements([]);
            setError("Bạn không có quyền xem danh sách Requirement.");
            return;
        }

        if (!projectId || Number(projectId) <= 0) {
            setRequirements([]);
            setTotalPages(1);
            setTotalElements(0);
            setError("Không xác định được project hiện tại.");
            setLoading(false);
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const data = await RequirementService.getRequirements(projectId, {
                keyword,
                status,
                priority,
                jiraIssueKey,
                page,
                size: PAGE_SIZE,
                sort: "updatedAt,desc",
            });

            if (!data || !Array.isArray(data.content)) {
                throw new Error(
                    "Response API không đúng cấu trúc data.content.",
                );
            }

            setRequirements(data.content);
            setTotalPages(
                Number.isInteger(data.totalPages)
                    ? Math.max(data.totalPages, 1)
                    : 1,
            );
            setTotalElements(
                Number.isInteger(data.totalElements)
                    ? data.totalElements
                    : data.content.length,
            );
        } catch (err) {
            setRequirements([]);
            setTotalPages(1);
            setTotalElements(0);
            if (err.status === 401) {
                setError("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            } else if (err.status === 403) {
                setError("Bạn không có quyền xem danh sách Requirement.");
            } else if (err.status === 404) {
                setError("Không tìm thấy project hoặc Requirement.");
            } else {
                setError(err.message || "Không thể tải danh sách Requirement.");
            }
        } finally {
            setLoading(false);
        }
    }, [projectId, canView, keyword, status, priority, jiraIssueKey, page]);

    useEffect(() => {
        fetchRequirements();
    }, [fetchRequirements]);

    const handleSearchSubmit = (event) => {
        event.preventDefault();
        setPage(0);
        setKeyword(keywordInput.trim());
        setJiraIssueKey(jiraIssueKeyInput.trim());
    };

    const handleClearFilters = () => {
        setKeywordInput("");
        setJiraIssueKeyInput("");
        setKeyword("");
        setJiraIssueKey("");
        setStatus("");
        setPriority("");
        setPage(0);
    };

    const handleStatusChange = (event) => {
        setStatus(event.target.value);
        setPage(0);
    };

    const handlePriorityChange = (event) => {
        setPriority(event.target.value);
        setPage(0);
    };

    const handlePreviousPage = () => {
        setPage((currentPage) => Math.max(currentPage - 1, 0));
    };

    const handleNextPage = () => {
        setPage((currentPage) => Math.min(currentPage + 1, totalPages - 1));
    };

    if (!canView) {
        return (
            <div
                style={{
                    padding: "32px",
                    textAlign: "center",
                    color: "#bf2600",
                }}
            >
                Bạn không có quyền truy cập vào danh sách Requirement.
            </div>
        );
    }

    if (canManage && showForm) {
        return (
            <RequirementForm
                projectId={projectId}
                requirementId={editingRequirementId}
                onSuccess={() => {
                    setShowForm(false);
                    setEditingRequirementId(null);
                    fetchRequirements();
                }}
                onCancel={() => {
                    setShowForm(false);
                    setEditingRequirementId(null);
                }}
            />
        );
    }

    return (
        <div style={{ padding: "24px", maxWidth: "1400px", margin: "0 auto" }}>
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
                            margin: 0,
                            fontSize: "24px",
                            color: "#172b4d",
                        }}
                    >
                        Requirements
                    </h2>
                    <p
                        style={{
                            margin: "4px 0 0",
                            color: "#6b778c",
                            fontSize: "14px",
                        }}
                    >
                        Danh sách yêu cầu kỹ thuật của project
                    </p>
                </div>
                {canManage && (
                    <button
                        type="button"
                        onClick={() => {
                            setEditingRequirementId(null);
                            setShowForm(true);
                        }}
                        style={{
                            padding: "10px 16px",
                            backgroundColor: "#0052cc",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer",
                            fontWeight: 600,
                            fontSize: "14px",
                        }}
                    >
                        + Tạo Requirement
                    </button>
                )}
            </div>

            <form
                onSubmit={handleSearchSubmit}
                style={{
                    backgroundColor: "#fafbfc",
                    padding: "16px",
                    borderRadius: "6px",
                    marginBottom: "20px",
                    border: "1px solid #ebecf0",
                }}
            >
                <div
                    style={{
                        display: "grid",
                        gridTemplateColumns: "2fr 1fr 1fr 1.5fr auto auto",
                        gap: "12px",
                        alignItems: "end",
                    }}
                >
                    <div>
                        <label
                            style={{
                                display: "block",
                                marginBottom: "6px",
                                fontSize: "13px",
                                fontWeight: 600,
                            }}
                        >
                            Từ khóa
                        </label>
                        <input
                            type="text"
                            placeholder="Tìm từ khóa"
                            value={keywordInput}
                            onChange={(event) =>
                                setKeywordInput(event.target.value)
                            }
                            style={{
                                width: "100%",
                                boxSizing: "border-box",
                                padding: "8px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                            }}
                        />
                    </div>
                    <div>
                        <label
                            style={{
                                display: "block",
                                marginBottom: "6px",
                                fontSize: "13px",
                                fontWeight: 600,
                            }}
                        >
                            Status
                        </label>
                        <select
                            aria-label="Status filter"
                            value={status}
                            onChange={handleStatusChange}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                                backgroundColor: "#fff",
                            }}
                        >
                            {STATUS_OPTIONS.map((option) => (
                                <option
                                    key={option || "ALL_STATUS"}
                                    value={option}
                                >
                                    {option || "Tất cả"}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label
                            style={{
                                display: "block",
                                marginBottom: "6px",
                                fontSize: "13px",
                                fontWeight: 600,
                            }}
                        >
                            Priority
                        </label>
                        <select
                            aria-label="Priority filter"
                            value={priority}
                            onChange={handlePriorityChange}
                            style={{
                                width: "100%",
                                padding: "8px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                                backgroundColor: "#fff",
                            }}
                        >
                            {PRIORITY_OPTIONS.map((option) => (
                                <option
                                    key={option || "ALL_PRIORITY"}
                                    value={option}
                                >
                                    {option || "Tất cả"}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label
                            style={{
                                display: "block",
                                marginBottom: "6px",
                                fontSize: "13px",
                                fontWeight: 600,
                            }}
                        >
                            Jira Issue Key
                        </label>
                        <input
                            type="text"
                            placeholder="Ví dụ: CNPM-63"
                            value={jiraIssueKeyInput}
                            onChange={(event) =>
                                setJiraIssueKeyInput(event.target.value)
                            }
                            style={{
                                width: "100%",
                                boxSizing: "border-box",
                                padding: "8px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                            }}
                        />
                    </div>
                    <button
                        type="submit"
                        style={{
                            padding: "9px 16px",
                            backgroundColor: "#0052cc",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer",
                            fontWeight: 600,
                        }}
                    >
                        Tìm kiếm
                    </button>
                    <button
                        type="button"
                        onClick={handleClearFilters}
                        style={{
                            padding: "9px 16px",
                            backgroundColor: "#fff",
                            color: "#172b4d",
                            border: "1px solid #dfe1e6",
                            borderRadius: "4px",
                            cursor: "pointer",
                        }}
                    >
                        Xóa lọc
                    </button>
                </div>
            </form>

            {error && (
                <div
                    role="alert"
                    style={{
                        padding: "14px 16px",
                        backgroundColor: "#ffebe6",
                        border: "1px solid #ffbdad",
                        borderRadius: "6px",
                        marginBottom: "20px",
                        color: "#bf2600",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                    }}
                >
                    <span>{error}</span>
                    <button
                        type="button"
                        onClick={fetchRequirements}
                        style={{
                            padding: "6px 12px",
                            backgroundColor: "#de350b",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer",
                        }}
                    >
                        Thử lại
                    </button>
                </div>
            )}

            {loading && (
                <div
                    style={{
                        padding: "40px",
                        textAlign: "center",
                        color: "#6b778c",
                    }}
                >
                    Đang tải danh sách Requirement...
                </div>
            )}

            {!loading && !error && requirements.length === 0 && (
                <div
                    style={{
                        padding: "50px",
                        textAlign: "center",
                        color: "#6b778c",
                    }}
                >
                    Không có Requirement nào.
                </div>
            )}

            {!loading && !error && requirements.length > 0 && (
                <>
                    <div
                        style={{
                            border: "1px solid #ebecf0",
                            borderRadius: "6px",
                            overflowX: "auto",
                        }}
                    >
                        <table
                            style={{
                                width: "100%",
                                borderCollapse: "collapse",
                                textAlign: "left",
                            }}
                        >
                            <thead>
                                <tr
                                    style={{
                                        backgroundColor: "#f4f5f7",
                                        borderBottom: "1px solid #dfe1e6",
                                    }}
                                >
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Jira Issue Key
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Title
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Actor
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Description
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Priority
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Status
                                    </th>
                                    <th
                                        style={{
                                            padding: "12px 14px",
                                            fontSize: "13px",
                                        }}
                                    >
                                        Updated
                                    </th>
                                    {canManage && (
                                        <th
                                            style={{
                                                padding: "12px 14px",
                                                fontSize: "13px",
                                            }}
                                        >
                                            Thao tác
                                        </th>
                                    )}
                                </tr>
                            </thead>
                            <tbody>
                                {requirements.map((req) => (
                                    <tr
                                        key={req.id}
                                        style={{
                                            borderBottom: "1px solid #ebecf0",
                                        }}
                                    >
                                        <td
                                            style={{
                                                padding: "12px 14px",
                                                fontWeight: 600,
                                            }}
                                        >
                                            {req.jiraIssueKey || "-"}
                                        </td>
                                        <td style={{ padding: "12px 14px" }}>
                                            {req.title}
                                        </td>
                                        <td style={{ padding: "12px 14px" }}>
                                            {req.actor || "-"}
                                        </td>
                                        <td
                                            style={{
                                                padding: "12px 14px",
                                                maxWidth: "250px",
                                            }}
                                        >
                                            {req.description || "-"}
                                        </td>
                                        <td style={{ padding: "12px 14px" }}>
                                            {req.priority || "-"}
                                        </td>
                                        <td style={{ padding: "12px 14px" }}>
                                            {req.status || "-"}
                                        </td>
                                        <td style={{ padding: "12px 14px" }}>
                                            {req.updatedAt
                                                ? new Date(
                                                      req.updatedAt,
                                                  ).toLocaleString("vi-VN")
                                                : "-"}
                                        </td>
                                        {canManage && (
                                            <td
                                                style={{ padding: "12px 14px" }}
                                            >
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        setEditingRequirementId(
                                                            req.id,
                                                        );
                                                        setShowForm(true);
                                                    }}
                                                    style={{
                                                        padding: "5px 10px",
                                                        backgroundColor: "#fff",
                                                        border: "1px solid #0052cc",
                                                        color: "#0052cc",
                                                        borderRadius: "4px",
                                                        cursor: "pointer",
                                                        fontSize: "13px",
                                                    }}
                                                >
                                                    Sửa
                                                </button>
                                            </td>
                                        )}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div
                        style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            marginTop: "16px",
                        }}
                    >
                        <span style={{ color: "#6b778c", fontSize: "14px" }}>
                            Tổng số: {totalElements} Requirement
                        </span>
                        <div
                            style={{
                                display: "flex",
                                gap: "8px",
                                alignItems: "center",
                            }}
                        >
                            <button
                                type="button"
                                disabled={page === 0}
                                onClick={handlePreviousPage}
                                style={{
                                    padding: "6px 12px",
                                    cursor:
                                        page === 0 ? "not-allowed" : "pointer",
                                }}
                            >
                                Trước
                            </button>
                            <span style={{ fontSize: "14px" }}>
                                Trang {page + 1} / {totalPages}
                            </span>
                            <button
                                type="button"
                                disabled={page >= totalPages - 1}
                                onClick={handleNextPage}
                                style={{
                                    padding: "6px 12px",
                                    cursor:
                                        page >= totalPages - 1
                                            ? "not-allowed"
                                            : "pointer",
                                }}
                            >
                                Sau
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default RequirementList;
