package vn.edu.cnpm.projectsupport.integration.jira.contract;

import java.time.Instant;

public record JiraConnectionTestResponse(
        Long projectId,
        boolean connected,
        String accountId,
        String displayName,
        String jiraProjectId,
        String projectKey,
        Instant testedAt,
        String errorCode,
        String message) {
}
