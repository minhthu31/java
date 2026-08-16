import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { currentUser, logout } from "./authService";
import { RequirementList } from "./RequirementList";

export default function Dashboard({ title }) {
    const navigate = useNavigate();
    const user = currentUser();
    const [activeTab, setActiveTab] = useState("requirements");

    return (
        <main className="dashboard">
            <header>
                <div>
                    <span>CNPM Project Support</span>
                    <h1>{title}</h1>
                </div>
                <button
                    onClick={() => {
                        logout();
                        navigate("/login");
                    }}
                >
                    Đăng xuất
                </button>
            </header>

            <section className="welcome">
                <h2>Xin chào, {user?.fullName}</h2>
                <p>
                    Tài khoản: {user?.username} · Vai trò: {user?.role}
                </p>
            </section>

            <section className="grid">
                <article
                    onClick={() => setActiveTab("requirements")}
                    style={{
                        cursor: "pointer",
                        fontWeight:
                            activeTab === "requirements" ? "700" : "normal",
                    }}
                >
                    Yêu cầu dự án
                </article>
                <article
                    onClick={() => setActiveTab("tasks")}
                    style={{
                        cursor: "pointer",
                        fontWeight: activeTab === "tasks" ? "700" : "normal",
                    }}
                >
                    Công việc được giao
                </article>
                <article
                    onClick={() => setActiveTab("progress")}
                    style={{
                        cursor: "pointer",
                        fontWeight: activeTab === "progress" ? "700" : "normal",
                    }}
                >
                    Tiến độ nhóm
                </article>
                <article
                    onClick={() => setActiveTab("github")}
                    style={{
                        cursor: "pointer",
                        fontWeight: activeTab === "github" ? "700" : "normal",
                    }}
                >
                    Hoạt động GitHub
                </article>
            </section>

            <section style={{ marginTop: "20px" }}>
                {activeTab === "requirements" && (
                    <RequirementList
                        currentUserRole={user?.role || "TEAM_MEMBER"}
                    />
                )}
                {activeTab === "tasks" && (
                    <p style={{ textAlign: "center", color: "#64748b" }}>
                        Đang phát triển...
                    </p>
                )}
                {activeTab === "progress" && (
                    <p style={{ textAlign: "center", color: "#64748b" }}>
                        Đang phát triển...
                    </p>
                )}
                {activeTab === "github" && (
                    <p style={{ textAlign: "center", color: "#64748b" }}>
                        Đang phát triển...
                    </p>
                )}
            </section>
        </main>
    );
}
