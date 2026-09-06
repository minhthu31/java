package vn.edu.cnpm.projectsupport.integration.github.service;

import java.time.Instant;

public record GitHubRepositorySyncResult(
        Long projectId,
        Long repositoryId,
        Long githubRepositoryId,
        String owner,
        String name,
        String fullName,
        String url,
        String defaultBranch,
        Instant lastSyncedAt,
        String correlationId) {
}
