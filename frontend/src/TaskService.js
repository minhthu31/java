import api from "./api";

export const TaskService = {
    // 1. Lấy danh sách tasks của dự án từ backend
    getTasks: async (projectId) => {
        const response = await api.get(`/api/v1/projects/${projectId}/tasks`);
        return response.data?.data || response.data || [];
    },

    // 2. Lấy chi tiết một task theo ID
    getTaskById: async (projectId, taskId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/tasks/${taskId}`,
        );
        return response.data?.data || response.data;
    },

    // 3. Tạo mới task (Gửi payload đầy đủ: title, issue_type, priority, deadline, acceptance_criteria, ...)
    createTask: async (projectId, taskData) => {
        const response = await api.post(
            `/api/v1/projects/${projectId}/tasks`,
            taskData,
        );
        return response.data?.data || response.data;
    },

    // 4. Cập nhật trạng thái task (TODO -> IN_PROGRESS -> DONE, ...)
    updateTaskStatus: async (projectId, taskId, status) => {
        const response = await api.patch(
            `/api/v1/projects/${projectId}/tasks/${taskId}/status`,
            { status },
        );
        return response.data?.data || response.data;
    },

    // 5. Lấy metadata thực của dự án (danh sách thành viên assignee, sprints, features)
    getTaskMetadata: async (projectId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/task-metadata`,
        );
        return response.data?.data || response.data;
    },
};
