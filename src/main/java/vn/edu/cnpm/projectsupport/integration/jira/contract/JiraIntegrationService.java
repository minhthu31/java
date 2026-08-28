package vn.edu.cnpm.projectsupport.integration.jira.contract;

public interface JiraIntegrationService {

    JiraConnectionResponse getConnection(Long projectId);

    JiraConnectionResponse configureConnection(
            Long projectId,
            JiraConnectionRequest request);

    JiraConnectionTestResponse testConnection(
            Long projectId,
            String projectKey);

    JiraTaskSyncResponse syncTask(
            Long projectId,
            Long taskId,
            String idempotencyKey);

    JiraTaskSyncResponse retryTaskSync(
            Long projectId,
            Long taskId,
            String idempotencyKey);

    JiraIssueResponse getIssue(
            Long projectId,
            String jiraIssueKey);
}