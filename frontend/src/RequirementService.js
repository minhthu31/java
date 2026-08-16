import { currentUser } from "./authService";

const API_BASE_URL = "http://localhost:8080/api/v1/requirements";
let MOCK_REQUIREMENTS = [
    {
        id: 1,
        title: "Đăng nhập hệ thống qua JWT",
        actor: "User",
        priority: "HIGH",
        status: "APPROVED",
        description: "Xác thực tài khoản người dùng và sinh JWT token.",
        precondition: "Người dùng đã có tài khoản trên hệ thống.",
        mainFlow:
            "1. Nhập username & password\n2. Bấm Đăng nhập\n3. Hệ thống trả về token.",
        alternativeFlow: "Quên mật khẩu -> Bấm khôi phục mật khẩu.",
        exceptionFlow: "Nhập sai -> Báo lỗi thông tin không chính xác.",
        postcondition: "Người dùng được chuyển vào Dashboard.",
    },
    {
        id: 2,
        title: "Quản lý danh sách thành viên nhóm",
        actor: "Leader",
        priority: "MEDIUM",
        status: "APPROVED",
        description: "Thêm, sửa, xóa thành viên trong nhóm đồ án.",
        precondition: "Đã tạo nhóm thành công.",
        mainFlow:
            "1. Mở danh sách nhóm\n2. Chọn thêm thành viên\n3. Lưu thay đổi.",
        alternativeFlow: "",
        exceptionFlow: "Thành viên đã tồn tại trong nhóm khác.",
        postcondition: "Cập nhật sĩ số nhóm.",
    },
    {
        id: 3,
        title: "Tạo và cập nhật Requirement Description",
        actor: "Leader",
        priority: "CRITICAL",
        status: "IN_REVIEW",
        description: "Mô tả chi tiết các yêu cầu chức năng của hệ thống.",
        precondition: "Có quyền Leader hoặc Admin.",
        mainFlow: "1. Điền thông tin vào form\n2. Bấm Lưu.",
        alternativeFlow: "",
        exceptionFlow: "Thiếu các trường bắt buộc.",
        postcondition:
            "Requirement được lưu ở trạng thái DRAFT hoặc IN_REVIEW.",
    },
    {
        id: 4,
        title: "Phê duyệt yêu cầu đồ án môn học",
        actor: "Lecturer",
        priority: "HIGH",
        status: "IN_REVIEW",
        description:
            "Giảng viên xem xét và phê duyệt hoặc từ chối requirement.",
        precondition: "Requirement đang ở trạng thái IN_REVIEW.",
        mainFlow: "1. Xem chi tiết\n2. Bấm Phê duyệt hoặc Từ chối kèm lý do.",
        alternativeFlow: "",
        exceptionFlow: "",
        postcondition: "Trạng thái chuyển sang APPROVED hoặc REJECTED.",
    },
    {
        id: 5,
        title: "Theo dõi tiến độ qua GitHub API",
        actor: "Lecturer",
        priority: "HIGH",
        status: "DRAFT",
        description: "Đồng bộ commit và pull request từ GitHub về hệ thống.",
        precondition: "Đã liên kết repository.",
        mainFlow: "1. Lấy dữ liệu commit/PR qua Webhook.",
        alternativeFlow: "",
        exceptionFlow: "GitHub Token hết hạn.",
        postcondition: "Hiển thị lịch sử đóng góp của sinh viên.",
    },
];

export const requirementService = {
    getAuthHeaders() {
        const user = currentUser();
        const token =
            user?.token ||
            user?.accessToken ||
            localStorage.getItem("token") ||
            localStorage.getItem("access_token");

        return {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        };
    },

    async getRequirements(params = {}) {
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
                    headers: this.getAuthHeaders(),
                },
            );

            if (response.ok) {
                const result = await response.json();
                return result.data || result;
            }

            throw new Error(`BACKEND_ERROR_${response.status}`);
        } catch (err) {
            console.warn(
                "Backend API chưa sẵn sàng, dùng dữ liệu Fallback để kiểm thử UI.",
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

    async createRequirement(data) {
        try {
            const response = await fetch(API_BASE_URL, {
                method: "POST",
                headers: this.getAuthHeaders(),
                body: JSON.stringify(data),
            });

            if (response.ok) {
                const result = await response.json();
                return result.data || result;
            }
            throw new Error(
                `Lỗi máy chủ (${response.status}) khi tạo Requirement.`,
            );
        } catch (err) {
            console.warn("Lưu tạm vào bộ nhớ Mock:");
            const newItem = { ...data, id: Date.now() };
            MOCK_REQUIREMENTS.unshift(newItem);
            return newItem;
        }
    },

    async updateRequirement(id, data) {
        try {
            const response = await fetch(`${API_BASE_URL}/${id}`, {
                method: "PUT",
                headers: this.getAuthHeaders(),
                body: JSON.stringify(data),
            });

            if (response.ok) {
                const result = await response.json();
                return result.data || result;
            }
            throw new Error(`Lỗi máy chủ (${response.status}) khi cập nhật.`);
        } catch (err) {
            console.warn("Cập nhật tạm vào bộ nhớ Mock:");
            MOCK_REQUIREMENTS = MOCK_REQUIREMENTS.map((item) =>
                item.id === id ? { ...item, ...data } : item,
            );
            return { ...data, id };
        }
    },
};
