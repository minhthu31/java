package vn.edu.cnpm.projectsupport.reporting.dto;

/** GitHub contribution totals attributed to one local project member. */
public record MemberContributionResponse(
        Long memberId,
        String username,
        String fullName,
        boolean githubLinked,
        long commits,
        long pullRequests,
        long openPullRequests,
        long closedPullRequests,
        long mergedPullRequests,
        long linkedTasks) {
}
