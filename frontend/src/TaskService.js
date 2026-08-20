import api from "./api";

export const TaskService = {
    // 1. Lấy danh sách task (Section 5.2 - hỗ trợ phân trang chuẩn success envelope & page response)
    getTasks: async (projectId, params = {}) => {
        const response = await api.get(`/projects/${projectId}/tasks`, {
            params,
        });
        const resData = response.data?.data || response.data;
        if (Array.isArray(resData?.content)) {
            return resData.content;
        }
        if (Array.isArray(resData)) {
            return resData;
        }
        return [];
    },

    // 2. Lấy chi tiết task theo ID (Section 5.4)
    getTaskById: async (projectId, taskId) => {
        const response = await api.get(
            `/projects/${projectId}/tasks/${taskId}`,
        );
        return response.data?.data || response.data;
    },

    // 3. Tạo mới task (Section 5.3 - gửi payload camelCase)
    createTask: async (projectId, taskData) => {
        const response = await api.post(
            `/projects/${projectId}/tasks`,
            taskData,
        );
        return response.data?.data || response.data;
    },

    // 4. Chuyển trạng thái task (Section 5.5 - gửi kèm reason nếu BLOCKED/CANCELLED)
    updateTaskStatus: async (projectId, taskId, status, reason = "") => {
        const payload = { status };
        if (reason) {
            payload.reason = reason;
        }
        const response = await api.patch(
            `/projects/${projectId}/tasks/${taskId}/status`,
            payload,
        );
        return response.data?.data || response.data;
    },
};
