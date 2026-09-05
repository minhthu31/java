import api from "./api";

export const GitHubActivityService = {
    getActivity: async (projectId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/integrations/github/activities`,
        );
        return response.data?.data || response.data;
    },
};
