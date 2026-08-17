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

    const [showForm, setShowForm] = useState(false);
    const [editingRequirementId, setEditingRequirementId] = useState(null);

    const [keywordInput, setKeywordInput] = useState("");
    const [jiraIssueKeyInput, setJiraIssueKeyInput] = useState("");

    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState("");
    const [priority, setPriority] = useState("");
    const [jiraIssueKey, setJiraIssueKey] = useState("");

    const canManage = currentUserRole === "TEAM_LEADER";

    const fetchRequirements = useCallback(async () => {
        if (projectId === undefined || projectId === null || projectId === "") {
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
    }, [projectId, keyword, status, priority, jiraIssueKey, page]);

    useEffect(() => {
        fetchRequirements();
    }, [fetchRequirements]);

    const handleSearchSubmit = (e) => {
        e.preventDefault();
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

    if (showForm) {
        return (
            <div style={{ padding: "32px" }}>
                <RequirementForm
                    projectId={projectId}
                    requirementId={editingRequirementId}
                    onCancel={() => {
                        setShowForm(false);
                        setEditingRequirementId(null);
                    }}
                    onSuccess={() => {
                        setShowForm(false);
                        setEditingRequirementId(null);
                        fetchRequirements();
                    }}
                />
            </div>
        );
    }

    return (
        <div style={{ padding: "32px", maxWidth: "1400px", margin: "0 auto" }}>
            {/* HEADER */}
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "24px",
                }}
            >
                <div>
                    <h1 style={{ margin: 0, fontSize: "28px" }}>
                        Requirements
                    </h1>
                    <p style={{ margin: "6px 0 0", color: "#6b778c" }}>
                        Danh sách yêu cầu của project
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
                            borderRadius: "5px",
                            cursor: "pointer",
                            fontWeight: 600,
                        }}
                    >
                        + Tạo Requirement
                    </button>
                )}
            </div>

            {/* FILTER FORM */}
            <form
                onSubmit={handleSearchSubmit}
                style={{
                    backgroundColor: "#fff",
                    padding: "20px",
                    borderRadius: "8px",
                    marginBottom: "20px",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
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
                            onChange={(e) => setKeywordInput(e.target.value)}
                            style={{
                                width: "100%",
                                boxSizing: "border-box",
                                padding: "9px 10px",
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
                            onChange={(e) => {
                                setStatus(e.target.value);
                                setPage(0);
                            }}
                            style={{
                                width: "100%",
                                padding: "9px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                            }}
                        >
                            {STATUS_OPTIONS.map((opt) => (
                                <option key={opt || "ALL_STATUS"} value={opt}>
                                    {opt || "Tất cả"}
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
                            onChange={(e) => {
                                setPriority(e.target.value);
                                setPage(0);
                            }}
                            style={{
                                width: "100%",
                                padding: "9px 10px",
                                border: "1px solid #dfe1e6",
                                borderRadius: "4px",
                            }}
                        >
                            {PRIORITY_OPTIONS.map((opt) => (
                                <option key={opt || "ALL_PRIORITY"} value={opt}>
                                    {opt || "Tất cả"}
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
                            onChange={(e) =>
                                setJiraIssueKeyInput(e.target.value)
                            }
                            style={{
                                width: "100%",
                                boxSizing: "border-box",
                                padding: "9px 10px",
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

            {/* ERROR NOTIFICATION */}
            {error && (
                <div
                    role="alert"
                    style={{
                        padding: "16px",
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
                            padding: "7px 14px",
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

            {/* LOADING */}
            {loading && (
                <div
                    style={{
                        backgroundColor: "#fff",
                        padding: "40px",
                        textAlign: "center",
                        borderRadius: "8px",
                    }}
                >
                    Đang tải danh sách Requirement...
                </div>
            )}

            {/* EMPTY */}
            {!loading && !error && requirements.length === 0 && (
                <div
                    style={{
                        backgroundColor: "#fff",
                        padding: "50px",
                        textAlign: "center",
                        borderRadius: "8px",
                        color: "#6b778c",
                    }}
                >
                    Không có Requirement nào.
                </div>
            )}

            {/* TABLE */}
            {!loading && !error && requirements.length > 0 && (
                <>
                    <div
                        style={{
                            backgroundColor: "#fff",
                            borderRadius: "8px",
                            overflow: "auto",
                            boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
                        }}
                    >
                        <table
                            style={{
                                width: "100%",
                                borderCollapse: "collapse",
                            }}
                        >
                            <thead>
                                <tr style={{ backgroundColor: "#f4f5f7" }}>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Jira Issue Key
                                    </th>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Title
                                    </th>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Description
                                    </th>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Priority
                                    </th>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Status
                                    </th>
                                    <th
                                        style={{
                                            padding: "14px",
                                            textAlign: "left",
                                        }}
                                    >
                                        Updated
                                    </th>
                                    {canManage && (
                                        <th
                                            style={{
                                                padding: "14px",
                                                textAlign: "left",
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
                                            borderTop: "1px solid #ebecf0",
                                        }}
                                    >
                                        <td
                                            style={{
                                                padding: "14px",
                                                fontWeight: 600,
                                            }}
                                        >
                                            {req.jiraIssueKey || "-"}
                                        </td>
                                        <td style={{ padding: "14px" }}>
                                            {req.title}
                                        </td>
                                        <td
                                            style={{
                                                padding: "14px",
                                                maxWidth: "300px",
                                            }}
                                        >
                                            {req.description || "-"}
                                        </td>
                                        <td style={{ padding: "14px" }}>
                                            {req.priority || "-"}
                                        </td>
                                        <td style={{ padding: "14px" }}>
                                            {req.status || "-"}
                                        </td>
                                        <td style={{ padding: "14px" }}>
                                            {req.updatedAt
                                                ? new Date(
                                                      req.updatedAt,
                                                  ).toLocaleString("vi-VN")
                                                : "-"}
                                        </td>
                                        {canManage && (
                                            <td style={{ padding: "14px" }}>
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        setEditingRequirementId(
                                                            req.id,
                                                        );
                                                        setShowForm(true);
                                                    }}
                                                    style={{
                                                        padding: "6px 12px",
                                                        backgroundColor: "#fff",
                                                        border: "1px solid #0052cc",
                                                        color: "#0052cc",
                                                        borderRadius: "4px",
                                                        cursor: "pointer",
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

                    {/* PAGINATION */}
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
                                onClick={() =>
                                    setPage((p) => Math.max(p - 1, 0))
                                }
                                style={{
                                    padding: "7px 12px",
                                    cursor:
                                        page === 0 ? "not-allowed" : "pointer",
                                }}
                            >
                                Trước
                            </button>
                            <span>
                                Trang {page + 1} / {totalPages}
                            </span>
                            <button
                                type="button"
                                disabled={page >= totalPages - 1}
                                onClick={() =>
                                    setPage((p) =>
                                        Math.min(p + 1, totalPages - 1),
                                    )
                                }
                                style={{
                                    padding: "7px 12px",
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
