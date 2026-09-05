package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.List;

public record CommitResponse(
        Long id,
        Long repositoryId,
        String sha,
        String message,
        Long authorGithubUserId,
        String authorLogin,
        Instant committedAt,
        String htmlUrl,
        Integer additions,
        Integer deletions,
        Integer filesChanged,
        List<String> issueKeys
) {}
