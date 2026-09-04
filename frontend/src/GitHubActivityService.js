import api from "./api";

export const GitHubActivityService = {
    getActivity: async (projectId) => {
        const response = await api.get(
            `/api/v1/projects/${projectId}/integrations/github/activity`,
        );
        const payload = response.data?.data || response.data;
        return {
            commits: payload?.commits || [],
            pullRequests: payload?.pullRequests || [],
        };
    },
};
