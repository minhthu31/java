import api from "./api";

export const JiraIntegrationService = {
    getConnection: async (projectId) => {
        const response = await api.get(
            `/projects/${projectId}/integrations/jira/config`,
        );
        return response.data;
    },

    configureConnection: async (projectId, payload) => {
        const response = await api.put(
            `/projects/${projectId}/integrations/jira/config`,
            payload,
        );
        return response.data;
    },

    testConnection: async (projectId) => {
        const response = await api.post(
            `/projects/${projectId}/integrations/jira/test-connection`,
        );
        const resData = response.data || {};
        return resData.data !== undefined ? resData.data : resData;
    },

    getIssue: async (projectId, issueKey) => {
        try {
            const response = await api.get(
                `/projects/${projectId}/integrations/jira/issues/${issueKey}`,
            );
            return response.data;
        } catch (err) {
            const message =
                err.response?.data?.message ||
                err.response?.data?.error ||
                err.message ||
                "Không thể lấy thông tin Jira Issue.";
            throw new Error(message);
        }
    },

    syncTask: async (projectId, taskId, idempotencyKey) => {
        const key =
            (typeof idempotencyKey === "string" ? idempotencyKey.trim() : "") ||
            "";
        if (!key) {
            throw new Error("Idempotency-Key is required");
        }
        const response = await api.post(
            `/projects/${projectId}/integrations/jira/tasks/${taskId}/sync`,
            null,
            {
                headers: {
                    "Idempotency-Key": key,
                },
            },
        );
        return response.data;
    },

    retryTaskSync: async (projectId, taskId, customKey) => {
        try {
            const key =
                (typeof customKey === "string" ? customKey.trim() : "") ||
                `retry-sync-${taskId}-${Date.now()}`;
            const response = await api.post(
                `/projects/${projectId}/integrations/jira/tasks/${taskId}/retry`,
                null,
                {
                    headers: {
                        "Idempotency-Key": key,
                    },
                },
            );

            const resData = response.data || {};
            const data = resData.data !== undefined ? resData.data : resData;

            return {
                jiraIssueKey: data.jiraIssueKey || null,
                jiraIssueUrl: data.jiraIssueUrl || null,
                lastSyncedAt: data.syncedAt || data.lastSyncedAt || null,
                syncedAt: data.syncedAt || data.lastSyncedAt || null,
                syncStatus: data.syncStatus || "SYNCED",
            };
        } catch (err) {
            const message =
                err.response?.data?.message ||
                err.response?.data?.error ||
                err.message ||
                "Đồng bộ Jira thất bại.";
            throw new Error(message);
        }
    },
};
