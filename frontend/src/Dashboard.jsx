import React from "react";
import RequirementList from "./RequirementList";

export const Dashboard = () => {
    const currentPath = window.location.pathname.toLowerCase();

    let roleFromPath = null;

    if (
        currentPath.includes("/team-leader") ||
        currentPath.includes("/leader")
    ) {
        roleFromPath = "TEAM_LEADER";
    } else if (currentPath.includes("/lecturer")) {
        roleFromPath = "LECTURER";
    } else if (currentPath.includes("/student")) {
        roleFromPath = "STUDENT";
    } else if (currentPath.includes("/admin")) {
        roleFromPath = "ADMIN";
    }

    let storedUser = null;

    try {
        const user = localStorage.getItem("user");
        storedUser = user ? JSON.parse(user) : null;
    } catch {
        storedUser = null;
    }

    const storedRole =
        roleFromPath ||
        localStorage.getItem("role") ||
        localStorage.getItem("userRole") ||
        storedUser?.role ||
        "TEAM_LEADER";

    const currentUserRole = String(storedRole)
        .replace("ROLE_", "")
        .toUpperCase();

    const savedProjectId =
        localStorage.getItem("currentProjectId") ||
        localStorage.getItem("projectId");

    const currentProjectId =
        savedProjectId && Number(savedProjectId) > 0
            ? Number(savedProjectId)
            : 1;

    const handleLogout = () => {
        localStorage.removeItem("user");
        localStorage.removeItem("token");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("jwtToken");
        localStorage.removeItem("jwt");

        window.location.href = "/login";
    };

    return (
        <div
            style={{
                display: "flex",
                minHeight: "100vh",
                backgroundColor: "#f4f5f7",
            }}
        >
            <aside
                style={{
                    width: "240px",
                    backgroundColor: "#0747a6",
                    color: "#fff",
                    padding: "24px 16px",
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "space-between",
                    boxSizing: "border-box",
                }}
            >
                <div>
                    <h3
                        style={{
                            margin: "0 0 24px",
                            fontSize: "18px",
                            fontWeight: 600,
                        }}
                    >
                        Project Support
                    </h3>

                    <div
                        style={{
                            padding: "10px 14px",
                            backgroundColor: "#0052cc",
                            borderRadius: "6px",
                            fontWeight: 500,
                        }}
                    >
                        Requirements
                    </div>
                </div>

                <div
                    style={{
                        borderTop: "1px solid rgba(255,255,255,0.2)",
                        paddingTop: "16px",
                    }}
                >
                    <div
                        style={{
                            fontSize: "13px",
                            color: "#deebff",
                            marginBottom: "8px",
                        }}
                    >
                        Vai trò: <strong>{currentUserRole}</strong>
                    </div>

                    {currentUserRole === "ADMIN" && (
                        <div
                            style={{
                                fontSize: "11px",
                                color: "#ffbdad",
                                marginBottom: "12px",
                            }}
                        >
                            Admin chỉ được xem Requirement.
                        </div>
                    )}

                    <button
                        type="button"
                        onClick={handleLogout}
                        style={{
                            width: "100%",
                            padding: "8px 12px",
                            backgroundColor: "#de350b",
                            color: "#fff",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer",
                            fontWeight: 500,
                            fontSize: "13px",
                        }}
                    >
                        Đăng xuất
                    </button>
                </div>
            </aside>

            <main
                style={{
                    flex: 1,
                    overflowY: "auto",
                }}
            >
                <RequirementList
                    projectId={currentProjectId}
                    currentUserRole={currentUserRole}
                />
            </main>
        </div>
    );
};

export default Dashboard;
