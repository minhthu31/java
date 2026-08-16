import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { currentUser, logout } from "./authService";
import { RequirementList } from "./RequirementList";
import { RequirementForm } from "./RequirementForm";

export default function Dashboard() {
    const navigate = useNavigate();
    const user = currentUser() || {
        fullName: "User",
        username: "user.test",
        role: "ADMIN",
    };

    const [activeMenu, setActiveMenu] = useState("requirements");
    const [viewMode, setViewMode] = useState("LIST"); // 'LIST' | 'FORM'
    const [editingItem, setEditingItem] = useState(null);

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    const handleOpenCreate = () => {
        setEditingItem(null);
        setViewMode("FORM");
    };

    const handleOpenEdit = (item) => {
        setEditingItem(item);
        setViewMode("FORM");
    };

    const handleBackToList = () => {
        setEditingItem(null);
        setViewMode("LIST");
    };

    return (
        <div
            style={{
                display: "flex",
                flexDirection: "column",
                height: "100vh",
                backgroundColor: "#f8fafc",
                fontFamily: "Arial, sans-serif",
            }}
        >
            <header
                style={{
                    height: "60px",
                    backgroundColor: "#ffffff",
                    borderBottom: "1px solid #e2e8f0",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "0 24px",
                }}
            >
                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "10px",
                    }}
                >
                    <div
                        style={{
                            width: "36px",
                            height: "36px",
                            backgroundColor: "#2563eb",
                            color: "#fff",
                            borderRadius: "6px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontWeight: "bold",
                            fontSize: "14px",
                        }}
                    >
                        LOGO
                    </div>
                    <span
                        style={{
                            fontWeight: "700",
                            fontSize: "16px",
                            color: "#1e293b",
                        }}
                    >
                        CNPM Project Support
                    </span>
                </div>

                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "16px",
                    }}
                >
                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "8px",
                        }}
                    >
                        <div
                            style={{
                                width: "34px",
                                height: "34px",
                                borderRadius: "50%",
                                backgroundColor: "#e2e8f0",
                                color: "#475569",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                                fontWeight: "600",
                                fontSize: "13px",
                            }}
                        >
                            {user?.fullName?.charAt(0) || "U"}
                        </div>
                        <div style={{ fontSize: "13px" }}>
                            <div
                                style={{ fontWeight: "600", color: "#1e293b" }}
                            >
                                {user?.fullName}
                            </div>
                            <div style={{ color: "#64748b", fontSize: "11px" }}>
                                {user?.role}
                            </div>
                        </div>
                    </div>
                    <button
                        onClick={handleLogout}
                        style={{
                            padding: "6px 12px",
                            fontSize: "12px",
                            color: "#ef4444",
                            border: "1px solid #fecaca",
                            backgroundColor: "#fef2f2",
                            borderRadius: "4px",
                            cursor: "pointer",
                            fontWeight: "600",
                        }}
                    >
                        Logout
                    </button>
                </div>
            </header>

            <div style={{ display: "flex", flex: 1, overflow: "hidden" }}>
                <aside
                    style={{
                        width: "220px",
                        backgroundColor: "#ffffff",
                        borderRight: "1px solid #e2e8f0",
                        display: "flex",
                        flexDirection: "column",
                        padding: "16px 0",
                    }}
                >
                    <div
                        style={{
                            padding: "0 16px 12px 16px",
                            fontSize: "11px",
                            fontWeight: "700",
                            color: "#94a3b8",
                            textTransform: "uppercase",
                        }}
                    >
                        Menu
                    </div>

                    {[
                        { id: "dashboard", label: "Dashboard" },
                        { id: "requirements", label: "Requirement Management" },
                        { id: "my_tasks", label: "My Tasks" },
                        { id: "task_list", label: "Task List" },
                        { id: "group_management", label: "Group Management" },
                        {
                            id: "lecturer_management",
                            label: "Lecturer Management",
                        },
                        { id: "user_management", label: "User Management" },
                        { id: "jira_github", label: "Jira / GitHub Config" },
                        { id: "progress_report", label: "Progress Report" },
                        { id: "commit_report", label: "Commit Report" },
                    ].map((item) => {
                        const isActive = activeMenu === item.id;
                        return (
                            <div
                                key={item.id}
                                onClick={() => {
                                    setActiveMenu(item.id);
                                    if (item.id === "requirements")
                                        setViewMode("LIST");
                                }}
                                style={{
                                    padding: "10px 20px",
                                    fontSize: "13px",
                                    fontWeight: isActive ? "600" : "500",
                                    color: isActive ? "#2563eb" : "#475569",
                                    backgroundColor: isActive
                                        ? "#eff6ff"
                                        : "transparent",
                                    borderLeft: isActive
                                        ? "4px solid #2563eb"
                                        : "4px solid transparent",
                                    cursor: "pointer",
                                    transition: "all 0.15s ease",
                                }}
                            >
                                {item.label}
                            </div>
                        );
                    })}
                </aside>

                <main style={{ flex: 1, overflowY: "auto", padding: "24px" }}>
                    {activeMenu === "requirements" &&
                        (viewMode === "LIST" ? (
                            <RequirementList
                                currentUserRole={user?.role || "TEAM_MEMBER"}
                                onCreateRequirement={handleOpenCreate}
                                onEditRequirement={handleOpenEdit}
                            />
                        ) : (
                            <RequirementForm
                                initialData={editingItem}
                                onCancel={handleBackToList}
                                onSuccess={handleBackToList}
                            />
                        ))}

                    {activeMenu !== "requirements" && (
                        <div
                            style={{
                                backgroundColor: "#ffffff",
                                border: "1px solid #e2e8f0",
                                borderRadius: "8px",
                                padding: "40px",
                                textAlign: "center",
                                color: "#64748b",
                            }}
                        >
                            <h3>
                                {activeMenu.toUpperCase().replace("_", " ")}
                            </h3>
                            <p>
                                Màn hình này đang trong lộ trình phát triển theo
                                Wireframe.
                            </p>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
}
