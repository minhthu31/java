import axios from "axios";

const API_BASE_URL =
    process.env.REACT_APP_API_BASE_URL || "http://localhost:8080/api";

export const GitHubConfigService = {
    getConfig: async () => {
        const response = await axios.get(`${API_BASE_URL}/admin/config/github`);
        return response.data;
    },

    saveConfig: async (configData) => {
        const response = await axios.post(
            `${API_BASE_URL}/admin/config/github`,
            configData,
        );
        return response.data;
    },

    testConnection: async (connectionData) => {
        const response = await axios.post(
            `${API_BASE_URL}/admin/config/github/test`,
            connectionData,
        );
        return response.data;
    },
};
