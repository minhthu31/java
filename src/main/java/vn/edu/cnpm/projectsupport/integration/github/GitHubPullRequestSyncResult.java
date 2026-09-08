package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public record GitHubPullRequestSyncResult(
        Long projectId,
        Long repositoryId,
        int pullRequestsSynced,
        int linksCreated,
        int unlinkedActivities,
        int errors,
        Instant lastSyncedAt,
        String correlationId) {
}
