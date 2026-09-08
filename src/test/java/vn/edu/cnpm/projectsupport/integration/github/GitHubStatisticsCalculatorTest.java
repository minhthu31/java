package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.integration.github.statistics.GitHubMemberStatisticsResponse;
import vn.edu.cnpm.projectsupport.integration.github.statistics.GitHubStatisticsCalculator;

class GitHubStatisticsCalculatorTest {

    @Test
    void calculatesAllGithubMetricsAndKeepsUnlinkedMemberVisible() {
        List<GitHubStatisticsCalculator.Member> members = List.of(
                new GitHubStatisticsCalculator.Member(1L, "alice", "Alice", true, "alice-gh"),
                new GitHubStatisticsCalculator.Member(2L, "bob", "Bob", false, null));

        Map<Long, GitHubStatisticsCalculator.Counts> counts = GitHubStatisticsCalculator.merge(
                Map.of(1L, 3L),
                Map.of(1L, 2L),
                Map.of(1L, 1L),
                Map.of(1L, 4L));

        List<GitHubMemberStatisticsResponse> result = GitHubStatisticsCalculator.calculate(members, counts);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).commitCount()).isEqualTo(3);
        assertThat(result.get(0).openPullRequestCount()).isEqualTo(2);
        assertThat(result.get(0).closedPullRequestCount()).isEqualTo(1);
        assertThat(result.get(0).mergedPullRequestCount()).isEqualTo(4);
        assertThat(result.get(0).githubLinked()).isTrue();

        assertThat(result.get(1).commitCount()).isZero();
        assertThat(result.get(1).openPullRequestCount()).isZero();
        assertThat(result.get(1).closedPullRequestCount()).isZero();
        assertThat(result.get(1).mergedPullRequestCount()).isZero();
        assertThat(result.get(1).githubLinked()).isFalse();
        assertThat(result.get(1).githubLogin()).isNull();
    }

    @Test
    void mergesCountsForSameMemberWithoutLosingAnyMetric() {
        Map<Long, GitHubStatisticsCalculator.Counts> result = GitHubStatisticsCalculator.merge(
                Map.of(10L, 5L), Map.of(10L, 2L), Map.of(10L, 3L), Map.of(10L, 1L));

        assertThat(result.get(10L)).isEqualTo(new GitHubStatisticsCalculator.Counts(5, 2, 3, 1));
    }
}
