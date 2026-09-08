package vn.edu.cnpm.projectsupport.integration.github.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GitHubStatisticsCalculator {

    private GitHubStatisticsCalculator() {
    }

    public record Member(Long userId, String username, String fullName, boolean githubLinked, String githubLogin) {
    }

    public record Counts(long commits, long openPullRequests, long closedPullRequests, long mergedPullRequests) {
        public Counts add(Counts other) {
            return new Counts(commits + other.commits, openPullRequests + other.openPullRequests, closedPullRequests + other.closedPullRequests,mergedPullRequests + other.mergedPullRequests);
        }
    }

    public static List<GitHubMemberStatisticsResponse> calculate(List<Member> members,Map<Long, Counts> countsByUserId) {
        List<GitHubMemberStatisticsResponse> result = new ArrayList<>(members.size());
        for (Member member : members) {
            Counts counts = countsByUserId.getOrDefault(member.userId(), new Counts(0, 0, 0, 0));
            result.add(new GitHubMemberStatisticsResponse(
                    member.userId(),
                    member.username(),
                    member.fullName(),
                    member.githubLinked(),
                    member.githubLogin(),
                    counts.commits(),
                    counts.openPullRequests(),
                    counts.closedPullRequests(),
                    counts.mergedPullRequests()));
        }
        return result;
    }

    public static Map<Long, Counts> merge(
            Map<Long, Long> commitCounts,
            Map<Long, Long> openPrCounts,
            Map<Long, Long> closedPrCounts,
            Map<Long, Long> mergedPrCounts) {
        Map<Long, Counts> result = new HashMap<>();
        commitCounts.forEach((userId, count) -> result.put(userId, new Counts(count, 0, 0, 0)));
        openPrCounts.forEach((userId, count) -> result.merge(userId, new Counts(0, count, 0, 0), Counts::add));
        closedPrCounts.forEach((userId, count) -> result.merge(userId, new Counts(0, 0, count, 0), Counts::add));
        mergedPrCounts.forEach((userId, count) -> result.merge(userId, new Counts(0, 0, 0, count), Counts::add));
        return result;
    }
}
