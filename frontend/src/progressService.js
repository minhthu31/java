import axios from "axios";

const API_BASE_URL =
    process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api/v1";

const getAuthHeaders = () => {
    const token = localStorage.getItem("accessToken");
    return {
        headers: {
            Authorization: token ? `Bearer ${token}` : "",
            "Content-Type": "application/json",
        },
    };
};

export const progressService = {
    getSprints: async (projectId) => {
        const response = await axios.get(
            `${API_BASE_URL}/projects/${projectId}/sprints`,
            getAuthHeaders(),
        );
        return response.data?.data || response.data || [];
    },

    getProjectSummary: async (projectId, params = {}) => {
        const response = await axios.get(
            `${API_BASE_URL}/projects/${projectId}/reports/summary`,
            {
                ...getAuthHeaders(),
                params,
            },
        );
        return response.data?.data || response.data;
    },
};
