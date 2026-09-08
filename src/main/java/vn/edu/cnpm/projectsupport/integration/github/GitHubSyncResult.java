package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public record GitHubSyncResult(
        Long projectId,
        Long repositoryId,
        int commitsSynced,
        int pullRequestsSynced,
        int usersSynced,
        int linksCreated,
        int unlinkedActivities,
        int errors,
        Instant lastSyncedAt,
        String correlationId) {

    public static GitHubSyncResult combine(
            Long projectId,
            GitHubCommitSyncResult commits,
            GitHubPullRequestSyncResult pullRequests) {
        return new GitHubSyncResult(
                projectId,
                commits.repositoryId() != null ? commits.repositoryId() : pullRequests.repositoryId(),
                commits.commitsSynced(),
                pullRequests.pullRequestsSynced(),
                0,
                commits.linksCreated() + pullRequests.linksCreated(),
                commits.unlinkedActivities() + pullRequests.unlinkedActivities(),
                commits.errors() + pullRequests.errors(),
                Instant.now(),
                commits.correlationId() + "," + pullRequests.correlationId());
    }
}
