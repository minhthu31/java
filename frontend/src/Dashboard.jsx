import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { currentUser, logout } from "./authService";
import RequirementList from "./RequirementList";
import SrsPreview from "./SrsPreview";
import TaskComponent from "./TaskComponent";

const getRoleTitle = (role) => {
    switch (role) {
        case "TEAM_LEADER":
            return "Trưởng nhóm";
        case "LECTURER":
            return "Giảng viên hướng dẫn";
        case "ADMIN":
            return "Quản trị hệ thống";
        case "STUDENT":
        case "TEAM_MEMBER":
            return "Thành viên nhóm";
        default:
            return "Tổng quan dự án";
    }
};

export default function Dashboard({ title }) {
    const navigate = useNavigate();
    const user = currentUser();
    const [activeTab, setActiveTab] = useState("requirements");

    useEffect(() => {
        if (!user) {
            navigate("/login");
        }
    }, [user, navigate]);

    if (!user) {
        return null;
    }

    const userRole = user.role
        ? String(user.role).replace("ROLE_", "").toUpperCase()
        : null;

    const displayTitle = title || getRoleTitle(userRole);

    const getProjectId = () => {
        const fromUser =
            user?.projectId || user?.currentProjectId || user?.project?.id;

        if (fromUser && Number(fromUser) > 0) {
            return Number(fromUser);
        }

        const fromStorage =
            localStorage.getItem("currentProjectId") ||
            localStorage.getItem("projectId");

        if (fromStorage && Number(fromStorage) > 0) {
            return Number(fromStorage);
        }

        return null;
    };

    const selectedProjectId = getProjectId();
    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    const canAccessRequirement =
        userRole === "TEAM_LEADER" || userRole === "LECTURER";

    return (
        <main
            className="dashboard"
            style={{
                padding: "24px",
                backgroundColor: "#f4f5f7",
                minHeight: "100vh",
            }}
        >
            <header
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "24px",
                    padding: "16px 20px",
                    backgroundColor: "#fff",
                    borderRadius: "8px",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
                }}
            >
                <div>
                    <span
                        style={{
                            fontSize: "12px",
                            color: "#6b778c",
                            textTransform: "uppercase",
                            fontWeight: 600,
                        }}
                    >
                        CNPM Project Support
                    </span>
                    <h1
                        style={{
                            margin: "4px 0 0",
                            fontSize: "22px",
                            color: "#172b4d",
                        }}
                    >
                        {displayTitle}
                    </h1>
                </div>

                <button
                    type="button"
                    onClick={handleLogout}
                    style={{
                        padding: "8px 16px",
                        backgroundColor: "#de350b",
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontWeight: 500,
                    }}
                >
                    Đăng xuất
                </button>
            </header>

            <section
                className="welcome"
                style={{
                    marginBottom: "24px",
                    padding: "20px",
                    backgroundColor: "#fff",
                    borderRadius: "8px",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
                }}
            >
                <h2
                    style={{
                        margin: "0 0 6px",
                        fontSize: "18px",
                        color: "#172b4d",
                    }}
                >
                    Xin chào, {user.fullName || user.username || "Người dùng"}
                </h2>

                <p style={{ margin: 0, color: "#5e6c84", fontSize: "14px" }}>
                    Tài khoản: <strong>{user.username}</strong> · Vai trò:{" "}
                    <strong>{userRole || "Chưa phân quyền"}</strong>
                    {selectedProjectId && (
                        <span>
                            {" "}
                            · Project ID: <strong>#{selectedProjectId}</strong>
                        </span>
                    )}
                </p>
            </section>

            <section
                className="grid"
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                    gap: "16px",
                    marginBottom: "24px",
                }}
            >
                <button
                    type="button"
                    onClick={() => setActiveTab("requirements")}
                    style={{
                        backgroundColor: "#fff",
                        padding: "16px",
                        borderRadius: "6px",
                        border: "none",
                        borderLeft:
                            activeTab === "requirements"
                                ? "4px solid #0052cc"
                                : "4px solid transparent",
                        fontWeight: activeTab === "requirements" ? 600 : 500,
                        color:
                            activeTab === "requirements"
                                ? "#0052cc"
                                : "#172b4d",
                        cursor: "pointer",
                        textAlign: "left",
                        boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                        fontSize: "14px",
                    }}
                >
                    Yêu cầu dự án
                </button>
                <button
                    type="button"
                    onClick={() => setActiveTab("tasks")}
                    style={{
                        backgroundColor: "#fff",
                        padding: "16px",
                        borderRadius: "6px",
                        border: "none",
                        borderLeft:
                            activeTab === "tasks"
                                ? "4px solid #0052cc"
                                : "4px solid transparent",
                        fontWeight: activeTab === "tasks" ? 600 : 500,
                        color: activeTab === "tasks" ? "#0052cc" : "#172b4d",
                        cursor: "pointer",
                        textAlign: "left",
                        boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                        fontSize: "14px",
                    }}
                >
                    Công việc được giao
                </button>
                {canAccessRequirement && (
                    <button
                        type="button"
                        onClick={() => setActiveTab("srs")}
                        style={{
                            backgroundColor: "#fff",
                            padding: "16px",
                            borderRadius: "6px",
                            border: "none",
                            borderLeft:
                                activeTab === "srs"
                                    ? "4px solid #0052cc"
                                    : "4px solid transparent",
                            fontWeight: activeTab === "srs" ? 600 : 500,
                            color:
                                activeTab === "srs" ? "#0052cc" : "#172b4d",
                            cursor: "pointer",
                            textAlign: "left",
                            boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                            fontSize: "14px",
                        }}
                    >
                        Xem trước SRS
                    </button>
                )}
                <button
                    type="button"
                    onClick={() => setActiveTab("progress")}
                    style={{
                        backgroundColor: "#fff",
                        padding: "16px",
                        borderRadius: "6px",
                        border: "none",
                        borderLeft:
                            activeTab === "progress"
                                ? "4px solid #0052cc"
                                : "4px solid transparent",
                        fontWeight: activeTab === "progress" ? 600 : 500,
                        color: activeTab === "progress" ? "#0052cc" : "#172b4d",
                        cursor: "pointer",
                        textAlign: "left",
                        boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                        fontSize: "14px",
                    }}
                >
                    Tiến độ nhóm
                </button>
                <button
                    type="button"
                    onClick={() => setActiveTab("github")}
                    style={{
                        backgroundColor: "#fff",
                        padding: "16px",
                        borderRadius: "6px",
                        border: "none",
                        borderLeft:
                            activeTab === "github"
                                ? "4px solid #0052cc"
                                : "4px solid transparent",
                        fontWeight: activeTab === "github" ? 600 : 500,
                        color: activeTab === "github" ? "#0052cc" : "#172b4d",
                        cursor: "pointer",
                        textAlign: "left",
                        boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                        fontSize: "14px",
                    }}
                >
                    Hoạt động GitHub
                </button>
            </section>

            <section
                style={{
                    backgroundColor: "#fff",
                    borderRadius: "8px",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
                    overflow: "hidden",
                }}
            >
                {activeTab === "requirements" && (
                    <>
                        {!canAccessRequirement ? (
                            <div
                                style={{
                                    padding: "40px 24px",
                                    textAlign: "center",
                                    color: "#6b778c",
                                }}
                                data-testid="unauthorized-message"
                            >
                                <h3
                                    style={{
                                        color: "#de350b",
                                        marginBottom: "8px",
                                    }}
                                >
                                    Không có quyền truy cập
                                </h3>
                                <p style={{ margin: 0, fontSize: "14px" }}>
                                    Theo quy định phân quyền (CNPM-52), vai trò{" "}
                                    <strong>
                                        {userRole || "Chưa phân quyền"}
                                    </strong>{" "}
                                    không được phép truy cập dữ liệu
                                    Requirement.
                                </p>
                            </div>
                        ) : !selectedProjectId ? (
                            <div
                                style={{
                                    padding: "40px 24px",
                                    textAlign: "center",
                                    color: "#6b778c",
                                }}
                                data-testid="no-project-message"
                            >
                                <h3
                                    style={{
                                        color: "#172b4d",
                                        marginBottom: "8px",
                                    }}
                                >
                                    Chưa chọn dự án
                                </h3>
                                <p style={{ margin: 0, fontSize: "14px" }}>
                                    Vui lòng chọn một project trước khi xem danh
                                    sách Requirement.
                                </p>
                            </div>
                        ) : (
                            <RequirementList
                                projectId={selectedProjectId}
                                currentUserRole={userRole}
                            />
                        )}
                    </>
                )}

                {activeTab === "tasks" && (
                    <div style={{ padding: "20px" }}>
                        {!selectedProjectId ? (
                            <div
                                data-testid="no-project-message"
                                style={{
                                    padding: "40px 24px",
                                    textAlign: "center",
                                    color: "#6b778c",
                                }}
                            >
                                <h3
                                    style={{
                                        color: "#172b4d",
                                        marginBottom: "8px",
                                    }}
                                >
                                    Chưa chọn dự án
                                </h3>
                                <p style={{ margin: 0, fontSize: "14px" }}>
                                    Vui lòng chọn một project trước khi xem danh
                                    sách Task.
                                </p>
                            </div>
                        ) : (
                            <TaskComponent projectId={selectedProjectId} />
                        )}
                    </div>
                )}

                {activeTab === "srs" && (
                    <>
                        {!selectedProjectId ? (
                            <div
                                data-testid="no-project-message"
                                style={{
                                    padding: "40px 24px",
                                    textAlign: "center",
                                    color: "#6b778c",
                                }}
                            >
                                Chưa chọn dự án để xem trước SRS.
                            </div>
                        ) : (
                            <SrsPreview
                                projectId={selectedProjectId}
                                currentUserRole={userRole}
                            />
                        )}
                    </>
                )}

                {activeTab === "progress" && (
                    <div
                        style={{
                            padding: "40px 24px",
                            textAlign: "center",
                            color: "#6b778c",
                        }}
                    >
                        <h3>Theo dõi tiến độ nhóm</h3>
                        <p style={{ margin: 0, fontSize: "14px" }}>
                            Báo cáo tiến độ và phân bổ công việc của các thành
                            viên.
                        </p>
                    </div>
                )}

                {activeTab === "github" && (
                    <div
                        style={{
                            padding: "40px 24px",
                            textAlign: "center",
                            color: "#6b778c",
                        }}
                    >
                        <h3>Hoạt động GitHub</h3>
                        <p style={{ margin: 0, fontSize: "14px" }}>
                            Theo dõi lịch sử commits, pull requests và đóng góp
                            mã nguồn.
                        </p>
                    </div>
                )}
            </section>
        </main>
    );
}

export { Dashboard };
