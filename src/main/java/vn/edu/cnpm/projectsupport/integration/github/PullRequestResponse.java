package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.List;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;

public record PullRequestResponse(
        Long id,
        Long repositoryId,
        Long githubPullRequestId,
        Integer number,
        String title,
        Long authorGithubUserId,
        String authorLogin,
        String headRef,
        String headSha,
        String baseRef,
        GitHubPullRequestState state,
        Boolean draft,
        Instant mergedAt,
        String htmlUrl,
        List<String> issueKeys
) {}