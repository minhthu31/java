package vn.edu.cnpm.projectsupport.integration.jira.contract;

import java.time.Instant;
import java.time.LocalDate;

public record JiraIssueResponse(
        Long projectId,
        Long localTaskId,
        String jiraIssueId,
        String jiraIssueKey,
        String jiraIssueUrl,
        String summary,
        String description,
        String issueType,
        String priority,
        String jiraStatusId,
        String jiraStatusName,
        String jiraStatusCategory,
        String assigneeAccountId,
        String assigneeDisplayName,
        LocalDate dueDate,
        Long jiraSprintId,
        String sprintName,
        String parentIssueKey,
        Instant remoteUpdatedAt,
        Instant lastSyncedAt) {
}
