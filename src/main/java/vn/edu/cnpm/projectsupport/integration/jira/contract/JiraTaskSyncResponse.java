package vn.edu.cnpm.projectsupport.integration.jira.contract;

import java.time.Instant;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;

public record JiraTaskSyncResponse(
        Long taskId,
        SyncStatus syncStatus,
        String jiraIssueId,
        String jiraIssueKey,
        String jiraIssueUrl,
        int attempt,
        boolean retryable,
        Instant syncedAt,
        String errorCode,
        String message) {
}
