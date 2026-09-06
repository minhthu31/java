package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

/** Result of importing commits from one configured GitHub repository. */
public record GitHubCommitSyncResult(
        Long projectId,
        Long repositoryId,
        int commitsSynced,
        int errors,
        Instant lastSyncedAt,
        String correlationId) {
}
