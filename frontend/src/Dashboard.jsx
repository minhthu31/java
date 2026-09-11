import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { currentUser, logout } from "./authService";
import RequirementList from "./RequirementList";
import SrsPreview from "./SrsPreview";
import TaskComponent from "./TaskComponent";
import JiraConfigComponent from "./JiraConfigComponent";
import { GitHubConfigComponent } from "./GitHubConfigComponent";
import { GitHubActivityComponent } from "./GitHubActivityComponent";
import { ProjectProgressComponent } from "./ProjectProgressComponent";
import MemberContributionComponent from "./MemberContributionComponent";

const getRoleTitle = (role) => {
    switch (role) {
        case "TEAM_LEADER":
            return "Trưởng nhóm";
        case "LECTURER":
            return "Giảng viên hướng dẫn";
        case "ADMIN":
            return "Quản trị hệ thống";
        case "TEAM_MEMBER":
            return "Thành viên nhóm";
        default:
            return "Tổng quan dự án";
    }
};

export default function Dashboard({ title }) {
    const navigate = useNavigate();
    const user = currentUser();

    const userRole = user?.role
        ? String(user.role).replace("ROLE_", "").toUpperCase()
        : null;

    const [activeTab, setActiveTab] = useState("requirements");

    useEffect(() => {
        if (!user) {
            navigate("/login");
        }
    }, [user, navigate]);

    if (!user) return null;

    const displayTitle = title || getRoleTitle(userRole);

    const getProjectId = () => {
        const fromUser =
            user?.projectId || user?.currentProjectId || user?.project?.id;
        if (fromUser && Number(fromUser) > 0) return Number(fromUser);

        const fromStorage =
            localStorage.getItem("currentProjectId") ||
            localStorage.getItem("projectId");
        if (fromStorage && Number(fromStorage) > 0) return Number(fromStorage);

        return null;
    };

    const selectedProjectId = getProjectId();
    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    const canAccessRequirement =
        userRole === "TEAM_LEADER" || userRole === "LECTURER";

    const canAccessProgress =
        userRole === "TEAM_LEADER" || userRole === "LECTURER";

    const canAccessJira = userRole === "ADMIN";
    const canAccessGitHubConfig = userRole === "ADMIN";

    return (
        <main
            className="dashboard"
            style={{
                padding: "28px 36px",
                background: "linear-gradient(180deg, #f0f6ff 0%, #e2eaf8 100%)",
                minHeight: "100vh",
                boxSizing: "border-box",
                fontFamily:
                    "'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif",
            }}
        >
            <header
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginBottom: "20px",
                    padding: "16px 26px",
                    backgroundColor: "#ffffff",
                    borderRadius: "14px",
                    border: "1px solid #dbeafe",
                    boxShadow: "0 2px 10px rgba(37, 99, 235, 0.04)",
                }}
            >
                <div>
                    <span
                        style={{
                            fontSize: "11px",
                            color: "#2563eb",
                            letterSpacing: "0.8px",
                            textTransform: "uppercase",
                            fontWeight: 700,
                        }}
                    >
                        CNPM Project Hub
                    </span>
                    <h1
                        style={{
                            margin: "3px 0 0",
                            fontSize: "20px",
                            color: "#0f172a",
                            fontWeight: 700,
                        }}
                    >
                        {displayTitle}
                    </h1>
                </div>

                <button
                    type="button"
                    onClick={handleLogout}
                    style={{
                        padding: "8px 18px",
                        backgroundColor: "#ffffff",
                        color: "#dc2626",
                        border: "1px solid #fecaca",
                        borderRadius: "8px",
                        cursor: "pointer",
                        fontWeight: 600,
                        fontSize: "13px",
                        transition: "all 0.15s ease",
                    }}
                    onMouseOver={(e) =>
                        (e.currentTarget.style.backgroundColor = "#fee2e2")
                    }
                    onMouseOut={(e) =>
                        (e.currentTarget.style.backgroundColor = "#ffffff")
                    }
                >
                    Đăng xuất
                </button>
            </header>

            <section
                className="welcome"
                style={{
                    marginBottom: "24px",
                    padding: "24px 30px",
                    background:
                        "linear-gradient(135deg, #0b2545 0%, #134074 50%, #1d4ed8 100%)",
                    borderRadius: "16px",
                    boxShadow: "0 10px 25px -4px rgba(19, 64, 116, 0.28)",
                    color: "#ffffff",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                }}
            >
                <div>
                    <h2
                        style={{
                            margin: "0 0 6px",
                            fontSize: "20px",
                            color: "#ffffff",
                            fontWeight: 700,
                        }}
                    >
                        Xin chào,{" "}
                        {user.fullName || user.username || "Người dùng"}
                    </h2>
                    <p
                        style={{
                            margin: 0,
                            color: "#bfdbfe",
                            fontSize: "13px",
                        }}
                    >
                        Tài khoản:{" "}
                        <strong style={{ color: "#ffffff" }}>
                            {user.username}
                        </strong>
                        {"  "}•{"  "}Vai trò:{" "}
                        <span
                            style={{
                                display: "inline-block",
                                padding: "2px 8px",
                                backgroundColor: "rgba(255, 255, 255, 0.16)",
                                borderRadius: "5px",
                                color: "#e0f2fe",
                                fontWeight: 600,
                                fontSize: "12px",
                            }}
                        >
                            {userRole || "Chưa phân quyền"}
                        </span>
                    </p>
                </div>

                {selectedProjectId && (
                    <div
                        style={{
                            padding: "8px 18px",
                            backgroundColor: "rgba(255, 255, 255, 0.12)",
                            borderRadius: "12px",
                            border: "1px solid rgba(255, 255, 255, 0.18)",
                            textAlign: "right",
                        }}
                    >
                        <div
                            style={{
                                fontSize: "11px",
                                color: "#93c5fd",
                                textTransform: "uppercase",
                                fontWeight: 600,
                            }}
                        >
                            Dự án hiện tại
                        </div>
                        <div
                            style={{
                                fontSize: "17px",
                                fontWeight: 800,
                                color: "#ffffff",
                            }}
                        >
                            Project #{selectedProjectId}
                        </div>
                    </div>
                )}
            </section>

            <div
                style={{
                    display: "flex",
                    gap: "22px",
                    alignItems: "flex-start",
                }}
            >
                <aside
                    className="grid"
                    style={{
                        width: "240px",
                        flexShrink: 0,
                        display: "flex",
                        flexDirection: "column",
                        gap: "6px",
                        backgroundColor: "#ffffff",
                        padding: "12px",
                        borderRadius: "14px",
                        border: "1px solid #dbeafe",
                        boxShadow: "0 2px 10px rgba(37, 99, 235, 0.03)",
                    }}
                >
                    <button
                        type="button"
                        onClick={() => setActiveTab("requirements")}
                        style={{
                            display: "block",
                            width: "100%",
                            padding: "12px 16px",
                            borderRadius: "10px",
                            border: "none",
                            backgroundColor:
                                activeTab === "requirements"
                                    ? "#1d4ed8"
                                    : "transparent",
                            color:
                                activeTab === "requirements"
                                    ? "#ffffff"
                                    : "#334155",
                            fontWeight:
                                activeTab === "requirements" ? 600 : 500,
                            fontSize: "13px",
                            cursor: "pointer",
                            textAlign: "left",
                            transition: "all 0.15s ease",
                        }}
                    >
                        Yêu cầu dự án
                    </button>

                    <button
                        type="button"
                        onClick={() => setActiveTab("tasks")}
                        style={{
                            display: "block",
                            width: "100%",
                            padding: "12px 16px",
                            borderRadius: "10px",
                            border: "none",
                            backgroundColor:
                                activeTab === "tasks"
                                    ? "#1d4ed8"
                                    : "transparent",
                            color:
                                activeTab === "tasks" ? "#ffffff" : "#334155",
                            fontWeight: activeTab === "tasks" ? 600 : 500,
                            fontSize: "13px",
                            cursor: "pointer",
                            textAlign: "left",
                            transition: "all 0.15s ease",
                        }}
                    >
                        Công việc được giao
                    </button>

                    {canAccessRequirement && (
                        <button
                            type="button"
                            onClick={() => setActiveTab("srs")}
                            style={{
                                display: "block",
                                width: "100%",
                                padding: "12px 16px",
                                borderRadius: "10px",
                                border: "none",
                                backgroundColor:
                                    activeTab === "srs"
                                        ? "#1d4ed8"
                                        : "transparent",
                                color:
                                    activeTab === "srs" ? "#ffffff" : "#334155",
                                fontWeight: activeTab === "srs" ? 600 : 500,
                                fontSize: "13px",
                                cursor: "pointer",
                                textAlign: "left",
                                transition: "all 0.15s ease",
                            }}
                        >
                            Xem trước SRS
                        </button>
                    )}

                    <button
                        type="button"
                        data-testid="progress-tab"
                        onClick={() => setActiveTab("progress")}
                        style={{
                            display: "block",
                            width: "100%",
                            padding: "12px 16px",
                            borderRadius: "10px",
                            border: "none",
                            backgroundColor:
                                activeTab === "progress"
                                    ? "#1d4ed8"
                                    : "transparent",
                            color:
                                activeTab === "progress"
                                    ? "#ffffff"
                                    : "#334155",
                            fontWeight: activeTab === "progress" ? 600 : 500,
                            fontSize: "13px",
                            cursor: "pointer",
                            textAlign: "left",
                            transition: "all 0.15s ease",
                        }}
                    >
                        Tiến độ nhóm
                    </button>

                    <button
                        type="button"
                        onClick={() => setActiveTab("github")}
                        style={{
                            display: "block",
                            width: "100%",
                            padding: "12px 16px",
                            borderRadius: "10px",
                            border: "none",
                            backgroundColor:
                                activeTab === "github"
                                    ? "#1d4ed8"
                                    : "transparent",
                            color:
                                activeTab === "github" ? "#ffffff" : "#334155",
                            fontWeight: activeTab === "github" ? 600 : 500,
                            fontSize: "13px",
                            cursor: "pointer",
                            textAlign: "left",
                            transition: "all 0.15s ease",
                        }}
                    >
                        Hoạt động GitHub
                    </button>

                    {canAccessJira && (
                        <button
                            type="button"
                            data-testid="jira-config-tab"
                            onClick={() => setActiveTab("jira-config")}
                            style={{
                                display: "block",
                                width: "100%",
                                padding: "12px 16px",
                                borderRadius: "10px",
                                border: "none",
                                backgroundColor:
                                    activeTab === "jira-config"
                                        ? "#1d4ed8"
                                        : "transparent",
                                color:
                                    activeTab === "jira-config"
                                        ? "#ffffff"
                                        : "#334155",
                                fontWeight:
                                    activeTab === "jira-config" ? 600 : 500,
                                fontSize: "13px",
                                cursor: "pointer",
                                textAlign: "left",
                                transition: "all 0.15s ease",
                            }}
                        >
                            Cấu hình Jira
                        </button>
                    )}

                    {canAccessGitHubConfig && (
                        <button
                            type="button"
                            data-testid="github-config-tab"
                            onClick={() => setActiveTab("github-config")}
                            style={{
                                display: "block",
                                width: "100%",
                                padding: "12px 16px",
                                borderRadius: "10px",
                                border: "none",
                                backgroundColor:
                                    activeTab === "github-config"
                                        ? "#1d4ed8"
                                        : "transparent",
                                color:
                                    activeTab === "github-config"
                                        ? "#ffffff"
                                        : "#334155",
                                fontWeight:
                                    activeTab === "github-config" ? 600 : 500,
                                fontSize: "13px",
                                cursor: "pointer",
                                textAlign: "left",
                                transition: "all 0.15s ease",
                            }}
                        >
                            Cấu hình GitHub
                        </button>
                    )}
                </aside>

                <section
                    style={{
                        flex: 1,
                        backgroundColor: "#ffffff",
                        borderRadius: "14px",
                        border: "1px solid #dbeafe",
                        boxShadow: "0 2px 10px rgba(37, 99, 235, 0.03)",
                        overflow: "hidden",
                        minHeight: "480px",
                    }}
                >
                    {activeTab === "github-config" && (
                        <div style={{ padding: "24px" }}>
                            <GitHubConfigComponent
                                currentUserRole={userRole}
                                projectId={selectedProjectId}
                            />
                        </div>
                    )}

                    {activeTab === "jira-config" &&
                        (!selectedProjectId ? (
                            <div
                                data-testid="no-project-message"
                                style={{
                                    padding: "60px 24px",
                                    textAlign: "center",
                                    color: "#64748b",
                                }}
                            >
                                <h3
                                    style={{
                                        color: "#0f172a",
                                        marginBottom: "6px",
                                    }}
                                >
                                    Chưa chọn dự án
                                </h3>
                                <p style={{ margin: 0, fontSize: "14px" }}>
                                    Vui lòng chọn một project trước khi cấu hình
                                    tích hợp Jira.
                                </p>
                            </div>
                        ) : (
                            <JiraConfigComponent
                                projectId={selectedProjectId}
                                role={userRole}
                            />
                        ))}

                    {activeTab === "requirements" && (
                        <>
                            {!canAccessRequirement ? (
                                <div
                                    style={{
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
                                    }}
                                    data-testid="unauthorized-message"
                                >
                                    <h3
                                        style={{
                                            color: "#dc2626",
                                            marginBottom: "8px",
                                        }}
                                    >
                                        Không có quyền truy cập
                                    </h3>
                                    <p style={{ margin: 0, fontSize: "14px" }}>
                                        Theo quy định phân quyền (CNPM-52), vai
                                        trò{" "}
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
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
                                    }}
                                    data-testid="no-project-message"
                                >
                                    <h3
                                        style={{
                                            color: "#0f172a",
                                            marginBottom: "6px",
                                        }}
                                    >
                                        Chưa chọn dự án
                                    </h3>
                                    <p style={{ margin: 0, fontSize: "14px" }}>
                                        Vui lòng chọn một project trước khi xem
                                        danh sách Requirement.
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
                        <div style={{ padding: "24px" }}>
                            {!selectedProjectId ? (
                                <div
                                    data-testid="no-project-message"
                                    style={{
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
                                    }}
                                >
                                    <h3
                                        style={{
                                            color: "#0f172a",
                                            marginBottom: "6px",
                                        }}
                                    >
                                        Chưa chọn dự án
                                    </h3>
                                    <p style={{ margin: 0, fontSize: "14px" }}>
                                        Vui lòng chọn một project trước khi xem
                                        danh sách Task.
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
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
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
                        <>
                            {!canAccessProgress ? (
                                <div
                                    style={{
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
                                    }}
                                    data-testid="unauthorized-progress-message"
                                >
                                    <h3
                                        style={{
                                            color: "#dc2626",
                                            marginBottom: "8px",
                                        }}
                                    >
                                        Không có quyền truy cập
                                    </h3>
                                    <p style={{ margin: 0, fontSize: "14px" }}>
                                        Chỉ có{" "}
                                        <strong>
                                            Trưởng nhóm (TEAM_LEADER)
                                        </strong>{" "}
                                        và{" "}
                                        <strong>Giảng viên (LECTURER)</strong>{" "}
                                        mới được phép theo dõi tiến độ dự án.
                                    </p>
                                </div>
                            ) : !selectedProjectId ? (
                                <div
                                    data-testid="no-project-message"
                                    style={{
                                        padding: "60px 24px",
                                        textAlign: "center",
                                        color: "#64748b",
                                    }}
                                >
                                    <h3
                                        style={{
                                            color: "#0f172a",
                                            marginBottom: "6px",
                                        }}
                                    >
                                        Chưa chọn dự án
                                    </h3>
                                    <p style={{ margin: 0, fontSize: "14px" }}>
                                        Vui lòng chọn một project trước khi xem
                                        tiến độ nhóm.
                                    </p>
                                </div>
                            ) : (
                                <div style={{ padding: "24px" }}>
                                    <ProjectProgressComponent
                                        key={selectedProjectId}
                                        projectId={selectedProjectId}
                                        currentUserRole={userRole}
                                    />
                                    <MemberContributionComponent
                                        key={`contrib-${selectedProjectId}`}
                                        projectId={selectedProjectId}
                                    />
                                </div>
                            )}
                        </>
                    )}

                    {activeTab === "github" &&
                        (!selectedProjectId ? (
                            <div
                                data-testid="no-project-message"
                                style={{
                                    padding: "60px 24px",
                                    textAlign: "center",
                                    color: "#64748b",
                                }}
                            >
                                <h3
                                    style={{
                                        color: "#0f172a",
                                        marginBottom: "6px",
                                    }}
                                >
                                    Chưa chọn dự án
                                </h3>
                                <p style={{ margin: 0, fontSize: "14px" }}>
                                    Vui lòng chọn một project trước khi xem hoạt
                                    động GitHub.
                                </p>
                            </div>
                        ) : (
                            <GitHubActivityComponent
                                key={selectedProjectId}
                                projectId={selectedProjectId}
                            />
                        ))}
                </section>
            </div>
        </main>
    );
}

export { Dashboard };
