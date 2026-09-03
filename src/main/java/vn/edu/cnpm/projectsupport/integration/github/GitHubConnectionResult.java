package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public record GitHubConnectionResult(
        boolean connected,
        Long githubUserId,
        String login,
        Long githubRepositoryId,
        String repositoryFullName,
        String permission,
        Long rateLimitRemaining,
        Instant rateLimitResetAt,
        Instant testedAt) {
}
