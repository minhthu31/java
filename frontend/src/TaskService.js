import api from "./api";

export const TaskService = {
    getActiveMembers: async (projectId) => {
        const response = await api.get(`/projects/${projectId}/members`);
        const data = response.data?.data || response.data;
        return Array.isArray(data) ? data : [];
    },

    getTasks: async (projectId, params = { page: 0, size: 20 }) => {
        const response = await api.get(`/projects/${projectId}/tasks`, {
            params,
        });
        const resData = response.data?.data || response.data;
        if (resData && Array.isArray(resData.content)) {
            return resData;
        }
        if (Array.isArray(resData)) {
            return {
                content: resData,
                page: 0,
                size: resData.length,
                totalPages: 1,
                totalElements: resData.length,
                first: true,
                last: true,
            };
        }
        return {
            content: [],
            page: 0,
            size: 20,
            totalPages: 0,
            totalElements: 0,
            first: true,
            last: true,
        };
    },

    getTaskById: async (projectId, taskId) => {
        const response = await api.get(
            `/projects/${projectId}/tasks/${taskId}`,
        );
        return response.data?.data || response.data;
    },

    createTask: async (projectId, taskData) => {
        const response = await api.post(
            `/projects/${projectId}/tasks`,
            taskData,
        );
        return response.data?.data || response.data;
    },

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
