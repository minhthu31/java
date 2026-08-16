import { currentUser } from "./authService";

const API_BASE_URL = "http://localhost:8080/api/v1/requirements";

const MOCK_REQUIREMENTS = [
    {
        id: 1,
        title: "Đăng nhập hệ thống qua JWT",
        actor: "User",
        priority: "HIGH",
        status: "APPROVED",
    },
    {
        id: 2,
        title: "Quản lý danh sách thành viên nhóm",
        actor: "Leader",
        priority: "MEDIUM",
        status: "APPROVED",
    },
    {
        id: 3,
        title: "Tạo và cập nhật Requirement Description",
        actor: "Leader",
        priority: "CRITICAL",
        status: "IN_REVIEW",
    },
    {
        id: 4,
        title: "Phê duyệt yêu cầu đồ án môn học",
        actor: "Lecturer",
        priority: "HIGH",
        status: "IN_REVIEW",
    },
    {
        id: 5,
        title: "Theo dõi tiến độ qua GitHub API",
        actor: "Lecturer",
        priority: "HIGH",
        status: "DRAFT",
    },
];

export const requirementService = {
    async getRequirements(params = {}) {
        const user = currentUser();
        const token =
            user?.token ||
            user?.accessToken ||
            localStorage.getItem("token") ||
            localStorage.getItem("access_token");

        const query = new URLSearchParams();
        if (params.search) query.append("search", params.search);
        if (params.actor && params.actor !== "ALL")
            query.append("actor", params.actor);
        if (params.priority && params.priority !== "ALL")
            query.append("priority", params.priority);
        if (params.status && params.status !== "ALL")
            query.append("status", params.status);

        try {
            const response = await fetch(
                `${API_BASE_URL}?${query.toString()}`,
                {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        ...(token ? { Authorization: `Bearer ${token}` } : {}),
                    },
                },
            );

            if (response.ok) {
                const result = await response.json();
                return result.data || result;
            }

            throw new Error(`BACKEND_ERROR_${response.status}`);
        } catch (err) {
            console.warn(
                "Backend chưa sẵn sàng API Requirement (Lỗi 500/404), hiển thị dữ liệu fallback để kiểm thử UI.",
            );

            const keyword = (params.search || "").trim().toLowerCase();
            return MOCK_REQUIREMENTS.filter((item) => {
                const matchSearch =
                    !keyword ||
                    item.title.toLowerCase().includes(keyword) ||
                    item.actor.toLowerCase().includes(keyword);
                const matchActor =
                    !params.actor ||
                    params.actor === "ALL" ||
                    item.actor === params.actor;
                const matchPriority =
                    !params.priority ||
                    params.priority === "ALL" ||
                    item.priority === params.priority;
                const matchStatus =
                    !params.status ||
                    params.status === "ALL" ||
                    item.status === params.status;
                return (
                    matchSearch && matchActor && matchPriority && matchStatus
                );
            });
        }
    },
};
