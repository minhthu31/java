import api from "./api";

export const TaskService = {
    // Lấy danh sách tasks từ backend
    getTasks: async (projectId) => {
        const response = await api.get(`/api/v1/projects/${projectId}/tasks`);
        return response.data?.data || response.data || [];
    },

    // Lấy chi tiết task
    getTaskById: async (projectId, taskId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/tasks/${taskId}`,
        );
        return response.data?.data || response.data;
    },

    // Tạo mới task
    createTask: async (projectId, taskData) => {
        const response = await api.post(
            `/api/v1/projects/${projectId}/tasks`,
            taskData,
        );
        return response.data?.data || response.data;
    },

    // Cập nhật trạng thái task
    updateTaskStatus: async (projectId, taskId, status) => {
        const response = await api.patch(
            `/api/v1/projects/${projectId}/tasks/${taskId}/status`,
            { status },
        );
        return response.data?.data || response.data;
    },

    // Lấy danh sách thành viên/metadata thực từ dự án
    getTaskMetadata: async (projectId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/task-metadata`,
        );
        return response.data?.data || response.data;
    },
};
