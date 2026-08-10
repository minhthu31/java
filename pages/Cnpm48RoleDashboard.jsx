import React from "react";
import { Navigate, useNavigate } from "react-router-dom";
import "./Cnpm48RoleDashboard.css";

/*
 * CNPM-48: Tạo trang đích cơ bản theo vai trò
 *
 * Role chuẩn theo tài liệu:
 * ADMIN
 * LECTURER
 * TEAM_LEADER
 * TEAM_MEMBER
 *
 * Cách sử dụng:
 * <Route path="/dashboard" element={<RoleDashboard />} />
 *
 * Sau khi CNPM-47 login thành công, lưu user vào localStorage:
 * localStorage.setItem("user", JSON.stringify({
 *   id: 1,
 *   username: "leader01",
 *   role: "TEAM_LEADER"
 * }));
 */

const ROLE_CONFIG = {
  ADMIN: {
    label: "Admin",
    dashboardPath: "/admin/dashboard",
    menu: [
      ["Dashboard", "/admin/dashboard"],
      ["Accounts", "/admin/accounts"],
      ["Lecturers", "/admin/lecturers"],
      ["Groups", "/admin/groups"],
      ["Jira / GitHub", "/admin/integrations"],
      ["Connection Status", "/admin/connection-status"],
      ["Audit / Sync Logs", "/admin/logs"],
      ["Reports", "/admin/reports"],
    ],
  },
  LECTURER: {
    label: "Lecturer",
    dashboardPath: "/lecturer/dashboard",
    menu: [
      ["Dashboard", "/lecturer/dashboard"],
      ["My Groups", "/lecturer/groups"],
      ["Requirements / SRS", "/lecturer/requirements"],
      ["Tasks", "/lecturer/tasks"],
      ["Progress Report", "/lecturer/progress"],
      ["Contribution Report", "/lecturer/contribution"],
      ["Auto-test", "/lecturer/auto-test"],
      ["Alerts", "/lecturer/alerts"],
    ],
  },
  TEAM_LEADER: {
    label: "Team Leader",
    dashboardPath: "/leader/dashboard",
    menu: [
      ["Dashboard", "/leader/dashboard"],
      ["Requirements / SRS", "/leader/requirements"],
      ["Tasks", "/leader/tasks"],
      ["Create Task", "/leader/tasks/create"],
      ["Jira Sync", "/leader/jira-sync"],
      ["Sprint / Feature", "/leader/sprint"],
      ["Team Report", "/leader/team-report"],
      ["Commit / PR", "/leader/commit-pr"],
      ["Unlinked Activity", "/leader/unlinked"],
      ["Auto-test", "/leader/auto-test"],
      ["Activity Log", "/leader/activity-log"],
    ],
  },
  TEAM_MEMBER: {
    label: "Team Member",
    dashboardPath: "/member/dashboard",
    menu: [
      ["Dashboard", "/member/dashboard"],
      ["My Tasks", "/member/tasks"],
      ["My Commits / PR", "/member/commits"],
      ["My Test Results", "/member/tests"],
      ["Personal Statistics", "/member/statistics"],
    ],
  },
};

function getStoredUser() {
  try {
    const raw = localStorage.getItem("user");
    if (!raw) return null;

    const parsed = JSON.parse(raw);

    // Hỗ trợ cả role và nested role.name nếu backend trả khác cấu trúc.
    const role =
      parsed?.role ||
      parsed?.user?.role ||
      parsed?.roleName ||
      parsed?.user?.roleName;

    return {
      ...parsed,
      role: String(role || "").toUpperCase(),
    };
  } catch {
    return null;
  }
}

function logout(navigate) {
  localStorage.removeItem("user");
  localStorage.removeItem("token");
  localStorage.removeItem("accessToken");
  navigate("/login", { replace: true });
}

function Sidebar({ user, config, currentPath, navigate }) {
  return (
    <aside className="cnpm48-sidebar">
      <div className="cnpm48-brand">
        <div className="cnpm48-logo">J</div>
        <div>
          <strong>Jira × GitHub</strong>
          <span>Project Management</span>
        </div>
      </div>

      <div className="cnpm48-role-badge">{config.label}</div>

      <nav className="cnpm48-nav">
        {config.menu.map(([label, path]) => {
          const active =
            currentPath === path ||
            (path.endsWith("/dashboard") &&
              currentPath === config.dashboardPath);

          return (
            <button
              key={path}
              className={`cnpm48-nav-item ${active ? "active" : ""}`}
              onClick={() => navigate(path)}
            >
              <span className="cnpm48-nav-dot" />
              {label}
            </button>
          );
        })}
      </nav>

      <button className="cnpm48-logout" onClick={() => logout(navigate)}>
        Đăng xuất
      </button>
    </aside>
  );
}

function Header({ user, config }) {
  return (
    <header className="cnpm48-header">
      <div>
        <div className="cnpm48-breadcrumb">Dashboard</div>
        <h1>{config.label} Dashboard</h1>
      </div>

      <div className="cnpm48-user">
        <div className="cnpm48-avatar">
          {(user?.username || config.label).charAt(0).toUpperCase()}
        </div>
        <div>
          <strong>{user?.username || "User"}</strong>
          <span>{config.label}</span>
        </div>
      </div>
    </header>
  );
}

function StatCard({ title, value, note }) {
  return (
    <div className="cnpm48-stat-card">
      <span>{title}</span>
      <strong>{value}</strong>
      <small>{note}</small>
    </div>
  );
}

function AdminDashboard() {
  return (
    <>
      <div className="cnpm48-welcome">
        <h2>Xin chào, Admin 👋</h2>
        <p>Quản lý người dùng, nhóm và trạng thái tích hợp của hệ thống.</p>
      </div>

      <div className="cnpm48-stats">
        <StatCard title="Users" value="24" note="Tài khoản hệ thống" />
        <StatCard title="Groups" value="6" note="Nhóm đang quản lý" />
        <StatCard title="Jira" value="Connected" note="Kết nối hiện tại" />
        <StatCard title="GitHub" value="Connected" note="Kết nối hiện tại" />
      </div>

      <section className="cnpm48-panel">
        <div className="cnpm48-panel-title">
          <h3>Connection Status</h3>
          <span className="cnpm48-status success">● Connected</span>
        </div>
        <div className="cnpm48-connection-grid">
          <div>
            <strong>Jira</strong>
            <span>Project: CNPM</span>
          </div>
          <div>
            <strong>GitHub</strong>
            <span>Repository connected</span>
          </div>
        </div>
      </section>

      <section className="cnpm48-panel">
        <h3>Recent Activity</h3>
        <div className="cnpm48-list">
          <div>Group CNPM-01 was updated</div>
          <div>Jira connection checked successfully</div>
          <div>New member account created</div>
        </div>
      </section>
    </>
  );
}

function LecturerDashboard() {
  return (
    <>
      <div className="cnpm48-welcome">
        <h2>Xin chào, Lecturer 👋</h2>
        <p>Theo dõi các nhóm được phân công và tiến độ đồ án.</p>
      </div>

      <div className="cnpm48-stats">
        <StatCard title="My Groups" value="3" note="Nhóm phụ trách" />
        <StatCard title="Total Tasks" value="45" note="Trong các nhóm" />
        <StatCard title="Completion" value="72%" note="Tỷ lệ hoàn thành" />
        <StatCard title="Overdue" value="4" note="Task quá hạn" />
      </div>

      <section className="cnpm48-panel">
        <h3>Nhóm phụ trách</h3>
        <div className="cnpm48-group-list">
          <div>
            <strong>CNPM Team 01</strong>
            <span>75% hoàn thành</span>
          </div>
          <div>
            <strong>CNPM Team 02</strong>
            <span>68% hoàn thành</span>
          </div>
          <div>
            <strong>CNPM Team 03</strong>
            <span>74% hoàn thành</span>
          </div>
        </div>
      </section>

      <section className="cnpm48-panel">
        <h3>Alerts</h3>
        <div className="cnpm48-alert">4 task đang quá hạn cần theo dõi.</div>
        <div className="cnpm48-alert">Có task Done chưa có commit.</div>
      </section>
    </>
  );
}

function LeaderDashboard() {
  return (
    <>
      <div className="cnpm48-welcome">
        <h2>Xin chào, Team Leader 👋</h2>
        <p>Quản lý requirement, task và theo dõi tiến độ nhóm.</p>
      </div>

      <div className="cnpm48-stats">
        <StatCard title="To Do" value="8" note="Task chưa bắt đầu" />
        <StatCard title="In Progress" value="12" note="Đang thực hiện" />
        <StatCard title="In Review" value="5" note="Đang review" />
        <StatCard title="Done" value="20" note="Đã hoàn thành" />
      </div>

      <section className="cnpm48-panel">
        <div className="cnpm48-panel-title">
          <h3>Task cần chú ý</h3>
          <span className="cnpm48-status warning">3 items</span>
        </div>

        <div className="cnpm48-task-list">
          <div>
            <strong>CNPM-47</strong>
            <span>Xây dựng giao diện đăng nhập</span>
            <em>In Progress</em>
          </div>
          <div>
            <strong>CNPM-48</strong>
            <span>Tạo trang đích cơ bản theo vai trò</span>
            <em>In Progress</em>
          </div>
          <div>
            <strong>CNPM-30</strong>
            <span>Login API</span>
            <em>In Review</em>
          </div>
        </div>
      </section>

      <div className="cnpm48-two-col">
        <section className="cnpm48-panel">
          <h3>Integration</h3>
          <p>
            Jira <span className="cnpm48-status success">● Connected</span>
          </p>
          <p>
            GitHub <span className="cnpm48-status success">● Connected</span>
          </p>
        </section>

        <section className="cnpm48-panel">
          <h3>Sprint</h3>
          <p>
            <strong>Sprint 1</strong>
          </p>
          <p>Progress: 72%</p>
        </section>
      </div>
    </>
  );
}

function MemberDashboard({ user }) {
  return (
    <>
      <div className="cnpm48-welcome">
        <h2>Xin chào, {user?.username || "Member"} 👋</h2>
        <p>Đây là khu vực công việc và đóng góp cá nhân của bạn.</p>
      </div>

      <div className="cnpm48-stats">
        <StatCard title="To Do" value="3" note="Task được giao" />
        <StatCard title="In Progress" value="2" note="Đang thực hiện" />
        <StatCard title="Done" value="5" note="Đã hoàn thành" />
        <StatCard title="PRs" value="4" note="Pull Requests" />
      </div>

      <section className="cnpm48-panel">
        <h3>My Tasks</h3>
        <div className="cnpm48-task-list">
          <div>
            <strong>CNPM-47</strong>
            <span>Xây dựng giao diện đăng nhập</span>
            <em>In Progress</em>
          </div>
          <div>
            <strong>CNPM-48</strong>
            <span>Tạo trang đích cơ bản theo vai trò</span>
            <em>In Progress</em>
          </div>
          <div>
            <strong>CNPM-50</strong>
            <span>Login Controller</span>
            <em>To Do</em>
          </div>
        </div>
      </section>

      <section className="cnpm48-panel">
        <h3>My Contributions</h3>
        <div className="cnpm48-contribution-grid">
          <div>
            <strong>12</strong>
            <span>Commits</span>
          </div>
          <div>
            <strong>4</strong>
            <span>Pull Requests</span>
          </div>
          <div>
            <strong>8</strong>
            <span>Linked Tasks</span>
          </div>
          <div>
            <strong>6</strong>
            <span>Passed Tests</span>
          </div>
        </div>
      </section>
    </>
  );
}

function Forbidden() {
  const navigate = useNavigate();

  return (
    <div className="cnpm48-forbidden">
      <div className="cnpm48-forbidden-box">
        <div className="cnpm48-forbidden-code">403</div>
        <h1>Access Denied</h1>
        <p>Bạn không có quyền truy cập trang này.</p>
        <button onClick={() => navigate("/dashboard")}>
          Quay về Dashboard
        </button>
      </div>
    </div>
  );
}

export default function Cnpm48RoleDashboard()  {
  const navigate = useNavigate();
  const user = getStoredUser();

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const config = ROLE_CONFIG[user.role];

  if (!config) {
    return <Forbidden />;
  }

  const path = window.location.pathname;

  // /dashboard tự động đưa về dashboard của role.
  if (path === "/dashboard") {
    return <Navigate to={config.dashboardPath} replace />;
  }

  // Chỉ render dashboard của chính role đó.
  const allowedDashboardPaths = Object.values(ROLE_CONFIG).map(
    (item) => item.dashboardPath,
  );

  if (!allowedDashboardPaths.includes(path)) {
    return <Forbidden />;
  }

  let content = null;

  switch (user.role) {
    case "ADMIN":
      content = <AdminDashboard />;
      break;
    case "LECTURER":
      content = <LecturerDashboard />;
      break;
    case "TEAM_LEADER":
      content = <LeaderDashboard />;
      break;
    case "TEAM_MEMBER":
      content = <MemberDashboard user={user} />;
      break;
    default:
      content = <Forbidden />;
  }

  return (
    <div className="cnpm48-app">
      <Sidebar
        user={user}
        config={config}
        currentPath={path}
        navigate={navigate}
      />

      <main className="cnpm48-main">
        <Header user={user} config={config} />

        <div className="cnpm48-content">{content}</div>
      </main>
    </div>
  );
}

export { ROLE_CONFIG, getStoredUser };
