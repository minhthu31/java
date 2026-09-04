import api from "./api";
import { JiraIntegrationService } from "./JiraIntegrationService";

jest.mock("./api");

describe("CNPM-74 Jira integration API contract", () => {
    beforeEach(() => jest.clearAllMocks());

    test("reads and updates the project-scoped Jira configuration", async () => {
        const payload = {
            siteUrl: "https://example.atlassian.net",
            projectKey: "CNPM",
            email: "admin@example.com",
            apiToken: "local-test-token",
            authType: "API_TOKEN",
        };
        api.get.mockResolvedValue({ data: { data: { configured: true } } });
        api.put.mockResolvedValue({ data: { data: { projectKey: "CNPM" } } });

        await JiraIntegrationService.getConnection(10);
        await JiraIntegrationService.configureConnection(10, payload);

        expect(api.get).toHaveBeenCalledWith(
            "/projects/10/integrations/jira/config",
        );
        expect(api.put).toHaveBeenCalledWith(
            "/projects/10/integrations/jira/config",
            payload,
        );
    });

    test("tests the configured Jira connection", async () => {
        api.post.mockResolvedValue({ data: { data: { connected: true } } });

        const result = await JiraIntegrationService.testConnection(10);

        expect(api.post).toHaveBeenCalledWith(
            "/projects/10/integrations/jira/test-connection",
        );
        expect(result.connected).toBe(true);
    });

    test("sync and retry send the mandatory Idempotency-Key", async () => {
        api.post.mockResolvedValue({
            data: { data: { taskId: 501, syncStatus: "SYNCED" } },
        });

        await JiraIntegrationService.syncTask(10, 501, " cnpm-74-sync-501 ");
        await JiraIntegrationService.retryTaskSync(10, 501, "cnpm-74-retry-501");

        expect(api.post).toHaveBeenNthCalledWith(
            1,
            "/projects/10/integrations/jira/tasks/501/sync",
            null,
            { headers: { "Idempotency-Key": "cnpm-74-sync-501" } },
        );
        expect(api.post).toHaveBeenNthCalledWith(
            2,
            "/projects/10/integrations/jira/tasks/501/retry",
            null,
            { headers: { "Idempotency-Key": "cnpm-74-retry-501" } },
        );
    });

    test("rejects sync without an idempotency key before calling the API", async () => {
        await expect(JiraIntegrationService.syncTask(10, 501, " "))
            .rejects.toThrow("Idempotency-Key is required");
        expect(api.post).not.toHaveBeenCalled();
    });

    test("reads a Jira issue through the internal API", async () => {
        api.get.mockResolvedValue({
            data: { data: { jiraIssueKey: "CNPM-74" } },
        });

        await JiraIntegrationService.getIssue(10, "CNPM-74");

        expect(api.get).toHaveBeenCalledWith(
            "/projects/10/integrations/jira/issues/CNPM-74",
        );
    });
});
