package vn.edu.cnpm.projectsupport.integration.jira.service;

import java.time.Instant;

public record JiraSyncResult(
        Long projectId,
        String jiraProjectKey,
        int issuesSynced,
        int backlogItems,
        int sprintsSynced,
        int errors,
        Instant lastSyncedAt,
        String correlationId) {
}
