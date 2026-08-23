package vn.edu.cnpm.projectsupport.integration.jira.contract;

import java.time.Instant;

public record JiraConnectionResponse(
        Long projectId,
        String siteUrl,
        String jiraProjectId,
        String projectKey,
        JiraAuthType authType,
        boolean configured,
        Instant lastTestedAt,
        Boolean lastTestSucceeded) {
}
