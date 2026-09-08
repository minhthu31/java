package vn.edu.cnpm.projectsupport.integration.github.statistics;

public record GitHubMemberStatisticsResponse(
        Long userId,
        String username,
        String fullName,
        boolean githubLinked,
        String githubLogin,
        long commitCount,
        long openPullRequestCount,
        long closedPullRequestCount,
        long mergedPullRequestCount) {
}
