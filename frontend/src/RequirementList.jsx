import React, { useState, useEffect, useMemo, useCallback } from "react";
import { requirementService } from "./RequirementService";
export const RequirementList = ({
    currentUserRole = "TEAM_MEMBER",
    onCreateRequirement,
    onEditRequirement,
}) => {
    const [requirements, setRequirements] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const [searchTerm, setSearchTerm] = useState("");
    const [selectedActor, setSelectedActor] = useState("ALL");
    const [selectedPriority, setSelectedPriority] = useState("ALL");
    const [selectedStatus, setSelectedStatus] = useState("ALL");

    const role = (currentUserRole || "").toUpperCase();
    const canManage =
        role === "TEAM_LEADER" || role === "LEADER" || role === "ADMIN";

    const loadData = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await requirementService.getRequirements({
                search: searchTerm,
                actor: selectedActor,
                priority: selectedPriority,
                status: selectedStatus,
            });
            setRequirements(Array.isArray(data) ? data : []);
        } catch (err) {
            setError(err.message || "Không thể tải dữ liệu từ máy chủ.");
        } finally {
            setIsLoading(false);
        }
    }, [searchTerm, selectedActor, selectedPriority, selectedStatus]);

    useEffect(() => {
        const timer = setTimeout(() => loadData(), 200);
        return () => clearTimeout(timer);
    }, [loadData]);

   
    const actorOptions = useMemo(() => {
        const actors = requirements.map((r) => r.actor).filter(Boolean);
        return Array.from(new Set(actors));
    }, [requirements]);

    const handleResetFilter = () => {
        setSearchTerm("");
        setSelectedActor("ALL");
        setSelectedPriority("ALL");
        setSelectedStatus("ALL");
    };

    const badgeStyle = {
        LOW: { bg: "#f1f5f9", text: "#475569", label: "Low" },
        MEDIUM: { bg: "#eff6ff", text: "#1d4ed8", label: "Medium" },
        HIGH: { bg: "#fef3c7", text: "#b45309", label: "High" },
        CRITICAL: { bg: "#ffe4e6", text: "#e11d48", label: "Critical" },
        DRAFT: { bg: "#f3f4f6", text: "#374151", label: "Bản nháp" },
        IN_REVIEW: { bg: "#fef9c3", text: "#854d0e", label: "Đang xem xét" },
        APPROVED: { bg: "#dcfce7", text: "#15803d", label: "Đã duyệt" },
        REJECTED: { bg: "#fee2e2", text: "#b91c1c", label: "Từ chối" },
    };

    return (
        <div
            style={{
                backgroundColor: "#ffffff",
                borderRadius: "12px",
                padding: "24px",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                border: "1px solid #e2e8f0",
            }}
        >
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderBottom: "1px solid #e2e8f0",
                    paddingBottom: "16px",
                    marginBottom: "20px",
                }}
            >
                <div>
                    <h3
                        style={{
                            margin: 0,
                            fontSize: "18px",
                            color: "#0f172a",
                            fontWeight: "700",
                        }}
                    >
                        Danh sách Requirement Description
                    </h3>
                    <span style={{ fontSize: "13px", color: "#64748b" }}>
                        Quyền hiện tại:{" "}
                        <strong style={{ color: "#2563eb" }}>
                            {currentUserRole}
                        </strong>
                    </span>
                </div>

                {canManage && (
                    <button
                        onClick={
                            onCreateRequirement ||
                            (() => alert("Mở form tạo Requirement mới"))
                        }
                        style={{
                            backgroundColor: "#2563eb",
                            color: "#fff",
                            border: "none",
                            padding: "8px 16px",
                            borderRadius: "8px",
                            cursor: "pointer",
                            fontWeight: "600",
                            fontSize: "13px",
                        }}
                    >
                        + Tạo Requirement
                    </button>
                )}
            </div>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                    gap: "12px",
                    marginBottom: "16px",
                }}
            >
                <div>
                    <label
                        style={{
                            display: "block",
                            fontSize: "12px",
                            fontWeight: "600",
                            color: "#475569",
                            marginBottom: "4px",
                        }}
                    >
                        Tìm kiếm Title / Actor
                    </label>
                    <input
                        type="text"
                        placeholder="Nhập từ khóa..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            fontSize: "14px",
                            boxSizing: "border-box",
                        }}
                    />
                </div>

                <div>
                    <label
                        style={{
                            display: "block",
                            fontSize: "12px",
                            fontWeight: "600",
                            color: "#475569",
                            marginBottom: "4px",
                        }}
                    >
                        Lọc theo Actor
                    </label>
                    <select
                        value={selectedActor}
                        onChange={(e) => setSelectedActor(e.target.value)}
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            fontSize: "14px",
                        }}
                    >
                        <option value="ALL">Tất cả Actor</option>
                        {actorOptions.map((actor) => (
                            <option key={actor} value={actor}>
                                {actor}
                            </option>
                        ))}
                    </select>
                </div>

                <div>
                    <label
                        style={{
                            display: "block",
                            fontSize: "12px",
                            fontWeight: "600",
                            color: "#475569",
                            marginBottom: "4px",
                        }}
                    >
                        Độ ưu tiên (Priority)
                    </label>
                    <select
                        value={selectedPriority}
                        onChange={(e) => setSelectedPriority(e.target.value)}
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            fontSize: "14px",
                        }}
                    >
                        <option value="ALL">Tất cả Priority</option>
                        <option value="LOW">Low</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="HIGH">High</option>
                        <option value="CRITICAL">Critical</option>
                    </select>
                </div>

                <div>
                    <label
                        style={{
                            display: "block",
                            fontSize: "12px",
                            fontWeight: "600",
                            color: "#475569",
                            marginBottom: "4px",
                        }}
                    >
                        Trạng thái (Status)
                    </label>
                    <select
                        value={selectedStatus}
                        onChange={(e) => setSelectedStatus(e.target.value)}
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            fontSize: "14px",
                        }}
                    >
                        <option value="ALL">Tất cả Trạng thái</option>
                        <option value="DRAFT">Bản nháp</option>
                        <option value="IN_REVIEW">Đang xem xét</option>
                        <option value="APPROVED">Đã duyệt</option>
                        <option value="REJECTED">Từ chối</option>
                    </select>
                </div>
            </div>

            {(searchTerm ||
                selectedActor !== "ALL" ||
                selectedPriority !== "ALL" ||
                selectedStatus !== "ALL") && (
                <div style={{ marginBottom: "16px", textAlign: "right" }}>
                    <button
                        onClick={handleResetFilter}
                        style={{
                            background: "none",
                            border: "none",
                            color: "#2563eb",
                            fontSize: "13px",
                            fontWeight: "600",
                            cursor: "pointer",
                            textDecoration: "underline",
                        }}
                    >
                        Xóa tất cả bộ lọc
                    </button>
                </div>
            )}

            {/* Hiển thị Trạng thái */}
            {isLoading ? (
                <div
                    style={{
                        textAlign: "center",
                        padding: "40px 0",
                        color: "#64748b",
                    }}
                >
                    Đang tải danh sách Requirement...
                </div>
            ) : error ? (
                <div
                    style={{
                        textAlign: "center",
                        padding: "24px",
                        color: "#b91c1c",
                        backgroundColor: "#fee2e2",
                        borderRadius: "8px",
                    }}
                >
                    <p style={{ margin: "0 0 8px 0" }}>{error}</p>
                    <button
                        onClick={loadData}
                        style={{
                            backgroundColor: "#dc2626",
                            color: "#fff",
                            border: "none",
                            padding: "6px 12px",
                            borderRadius: "6px",
                            cursor: "pointer",
                            fontSize: "12px",
                            fontWeight: "600",
                        }}
                    >
                        Thử lại
                    </button>
                </div>
            ) : requirements.length === 0 ? (
                <div
                    style={{
                        textAlign: "center",
                        padding: "40px 0",
                        color: "#64748b",
                        border: "1px dashed #cbd5e1",
                        borderRadius: "8px",
                    }}
                >
                    <p style={{ margin: "0 0 8px 0", fontSize: "14px" }}>
                        Không tìm thấy Requirement phù hợp.
                    </p>
                    <button
                        onClick={handleResetFilter}
                        style={{
                            backgroundColor: "#f1f5f9",
                            color: "#334155",
                            border: "1px solid #cbd5e1",
                            padding: "6px 12px",
                            borderRadius: "6px",
                            cursor: "pointer",
                            fontSize: "13px",
                            fontWeight: "600",
                        }}
                    >
                        Đặt lại bộ lọc
                    </button>
                </div>
            ) : (
                <div style={{ overflowX: "auto" }}>
                    <table
                        style={{
                            width: "100%",
                            borderCollapse: "collapse",
                            textAlign: "left",
                            fontSize: "14px",
                        }}
                    >
                        <thead>
                            <tr
                                style={{
                                    backgroundColor: "#f8fafc",
                                    borderBottom: "2px solid #e2e8f0",
                                    color: "#475569",
                                }}
                            >
                                <th style={{ padding: "12px 16px" }}>Title</th>
                                <th style={{ padding: "12px 16px" }}>Actor</th>
                                <th style={{ padding: "12px 16px" }}>
                                    Priority
                                </th>
                                <th style={{ padding: "12px 16px" }}>Status</th>
                                {canManage && (
                                    <th
                                        style={{
                                            padding: "12px 16px",
                                            textAlign: "right",
                                        }}
                                    >
                                        Thao tác
                                    </th>
                                )}
                            </tr>
                        </thead>
                        <tbody>
                            {requirements.map((item) => (
                                <tr
                                    key={item.id}
                                    style={{
                                        borderBottom: "1px solid #f1f5f9",
                                    }}
                                >
                                    <td
                                        style={{
                                            padding: "14px 16px",
                                            fontWeight: "600",
                                            color: "#0f172a",
                                        }}
                                    >
                                        {item.title}
                                    </td>
                                    <td
                                        style={{
                                            padding: "14px 16px",
                                            color: "#475569",
                                        }}
                                    >
                                        {item.actor}
                                    </td>
                                    <td style={{ padding: "14px 16px" }}>
                                        <span
                                            style={{
                                                padding: "4px 10px",
                                                borderRadius: "20px",
                                                fontSize: "12px",
                                                fontWeight: "600",
                                                backgroundColor:
                                                    badgeStyle[item.priority]
                                                        ?.bg,
                                                color: badgeStyle[item.priority]
                                                    ?.text,
                                            }}
                                        >
                                            {badgeStyle[item.priority]?.label ||
                                                item.priority}
                                        </span>
                                    </td>
                                    <td style={{ padding: "14px 16px" }}>
                                        <span
                                            style={{
                                                padding: "4px 10px",
                                                borderRadius: "6px",
                                                fontSize: "12px",
                                                fontWeight: "500",
                                                backgroundColor:
                                                    badgeStyle[item.status]?.bg,
                                                color: badgeStyle[item.status]
                                                    ?.text,
                                            }}
                                        >
                                            {badgeStyle[item.status]?.label ||
                                                item.status}
                                        </span>
                                    </td>
                                    {canManage && (
                                        <td
                                            style={{
                                                padding: "14px 16px",
                                                textAlign: "right",
                                            }}
                                        >
                                            <button
                                                onClick={() =>
                                                    onEditRequirement?.(item) ||
                                                    alert(
                                                        `Chỉnh sửa: ${item.title}`,
                                                    )
                                                }
                                                style={{
                                                    background: "none",
                                                    border: "none",
                                                    color: "#2563eb",
                                                    fontWeight: "600",
                                                    cursor: "pointer",
                                                }}
                                            >
                                                Chỉnh sửa
                                            </button>
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};
