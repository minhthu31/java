package vn.edu.cnpm.projectsupport.integration.github.statistics;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubUserActivityCountProjection;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;

@Service
@Transactional(readOnly = true)
public class GitHubStatisticsService {

    private final ProjectRepository projectRepository;
    private final GitHubCommitRepository commitRepository;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final UserExternalAccountRepository externalAccountRepository;

    public GitHubStatisticsService(
            ProjectRepository projectRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            UserExternalAccountRepository externalAccountRepository) {
        this.projectRepository = projectRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.externalAccountRepository = externalAccountRepository;
    }

    public List<GitHubMemberStatisticsResponse> getMemberStatistics(Long projectId, Instant from, Instant to) {
        validate(projectId, from, to);

        List<ProjectRepository.ActiveMemberProjection> activeMembers = projectRepository.findActiveMembers(projectId);
        List<Long> userIds = activeMembers.stream().map(ProjectRepository.ActiveMemberProjection::getId).toList();
        Map<Long, UserExternalAccount> accounts = userIds.isEmpty()
                ? Map.of()
                : externalAccountRepository.findAllByUserIdInAndProvider(userIds, IntegrationProvider.GITHUB).stream()
                        .collect(Collectors.toMap(UserExternalAccount::getUserId, Function.identity(), (first, ignored) -> first));

        Map<Long, Long> commitCounts = toMap(commitRepository.countByProjectIdAndTimeRange(projectId, from, to));
        Map<Long, Long> openPrCounts = toMap(pullRequestRepository.countByProjectIdAndStateAndTimeRange(projectId, GitHubPullRequestState.OPEN, from, to));
        Map<Long, Long> closedPrCounts = toMap(pullRequestRepository.countByProjectIdAndStateAndTimeRange(projectId, GitHubPullRequestState.CLOSED, from, to));
        Map<Long, Long> mergedPrCounts = toMap(pullRequestRepository.countByProjectIdAndStateAndTimeRange(projectId, GitHubPullRequestState.MERGED, from, to));
        Map<Long, GitHubStatisticsCalculator.Counts> counts = GitHubStatisticsCalculator.merge(commitCounts, openPrCounts, closedPrCounts, mergedPrCounts);

        List<GitHubStatisticsCalculator.Member> members = activeMembers.stream().map(member -> {
                    UserExternalAccount account = accounts.get(member.getId());
                    return new GitHubStatisticsCalculator.Member(
                            member.getId(),
                            member.getUsername(),
                            member.getFullName(),
                            account != null,
                            account == null ? null : account.getExternalLogin());
                }).toList();

        return GitHubStatisticsCalculator.calculate(members, counts);
    }

    private Map<Long, Long> toMap(List<GitHubUserActivityCountProjection> projections) {
        Map<Long, Long> result = new HashMap<>();
        for (GitHubUserActivityCountProjection projection : projections) {
            result.merge(projection.getUserId(), projection.getCount(), Long::sum);
        }
        return result;
    }

    private void validate(Long projectId, Instant from, Instant to) {
        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException("projectId phải là số dương");
        }
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ: from phải <= to");
        }
    }
}
