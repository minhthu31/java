package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public record GitHubCheckRunSyncResult(
        Long projectId,
        Long repositoryId,
        int synced,
        int created,
        int updated,
        int errors,
        boolean actionsEnabled,
        Instant syncedAt,
        String correlationId) {
}
