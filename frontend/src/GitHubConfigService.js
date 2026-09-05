import api from "./api";

const BASE_PROJECT_URL = "/projects";

export const GitHubConfigService = {
    getConfig: async (projectId) => {
        const response = await api.get(
            `${BASE_PROJECT_URL}/${projectId}/integrations/github/config`,
        );
        return response.data?.data;
    },
    saveConfig: async (projectId, configData) => {
        const response = await api.put(
            `${BASE_PROJECT_URL}/${projectId}/integrations/github/config`,
            configData,
        );
        return response.data?.data;
    },
    testConnection: async (projectId) => {
        const response = await api.post(
            `${BASE_PROJECT_URL}/${projectId}/integrations/github/test-connection`,
        );
        return response.data?.data;
    },
};

export default GitHubConfigService;
