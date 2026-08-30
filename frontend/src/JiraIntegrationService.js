import api from "./api";

const basePath = (projectId) => `/projects/${projectId}/integrations/jira`;
const unwrap = (response) => response.data?.data || response.data;

const syncHeaders = (idempotencyKey) => {
    if (!idempotencyKey || !idempotencyKey.trim()) {
        throw new Error("Idempotency-Key is required for Jira synchronization");
    }
    return { headers: { "Idempotency-Key": idempotencyKey.trim() } };
};

export const JiraIntegrationService = {
    getConnection: async (projectId) => {
        return unwrap(await api.get(`${basePath(projectId)}/config`));
    },

    configureConnection: async (projectId, payload) => {
        return unwrap(await api.put(`${basePath(projectId)}/config`, payload));
    },

    testConnection: async (projectId) => {
        return unwrap(await api.post(`${basePath(projectId)}/test-connection`));
    },

    syncTask: async (projectId, taskId, idempotencyKey) => {
        const headers = syncHeaders(idempotencyKey);
        return unwrap(
            await api.post(
                `${basePath(projectId)}/tasks/${taskId}/sync`,
                null,
                headers,
            ),
        );
    },

    retryTaskSync: async (projectId, taskId, idempotencyKey) => {
        const key =
            idempotencyKey ||
            (typeof crypto !== "undefined" && crypto.randomUUID
                ? crypto.randomUUID()
                : `retry-${Date.now()}`);
        const headers = syncHeaders(key);
        return unwrap(
            await api.post(
                `${basePath(projectId)}/tasks/${taskId}/retry`,
                null,
                headers,
            ),
        );
    },

    getIssue: async (projectId, jiraIssueKey) => {
        return unwrap(
            await api.get(
                `${basePath(projectId)}/issues/${encodeURIComponent(jiraIssueKey)}`,
            ),
        );
    },
};
