import api from "./api";

export const GitHubActivityService = {
    getActivity: async (projectId, params = {}) => {
        const response = await api.get(
            `/projects/${projectId}/integrations/github/activities`,
            { params },
        );
        return response.data?.data || response.data;
    },
};

export default GitHubActivityService;
