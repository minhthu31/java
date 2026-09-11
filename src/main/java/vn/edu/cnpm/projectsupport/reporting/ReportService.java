package vn.edu.cnpm.projectsupport.reporting;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.reporting.dto.MemberContributionResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSource;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceFreshnessResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.TaskMetricsResponse;
import vn.edu.cnpm.projectsupport.reporting.repository.ReportingRepository;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Duration DEFAULT_FRESHNESS_THRESHOLD = Duration.ofHours(24);

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final ReportingRepository reportingRepository;
    private final CurrentUserService currentUserService;

    public ReportService(
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            ReportingRepository reportingRepository,
            CurrentUserService currentUserService) {
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.reportingRepository = reportingRepository;
        this.currentUserService = currentUserService;
    }

    public ReportSummaryResponse getProjectSummary(Long projectId, ReportFilterRequest filter) {
        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException("projectId must be positive");
        }

        if (!projectRepository.existsById(projectId)) {
            throw new vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException("Project not found: " + projectId);
        }

        ReportFilterRequest safeFilter = filter == null? new ReportFilterRequest(null, null, null, null): filter;

        validateTimeRange(safeFilter);

        if (safeFilter.sprintId() != null && sprintRepository.findByIdAndProjectId(safeFilter.sprintId(), projectId).isEmpty()) {
            throw new vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException("Sprint not found: " + safeFilter.sprintId());
        }

        User currentUser = currentUserService.findCurrentUser().orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        Long effectiveMemberId = resolveMemberId(projectId, safeFilter.memberId(), currentUser);
        if (effectiveMemberId != null && !isActiveProjectMember(projectId, effectiveMemberId)) {
            throw new vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException(
                    "Member not found: " + effectiveMemberId);
        }

        Instant asOf = Instant.now();
        TaskMetricsResponse taskMetrics = calculateTaskMetrics(projectId, safeFilter, effectiveMemberId, asOf);

        List<MemberContributionResponse> contributions = projectRepository.findActiveMembers(projectId)
                .stream()
                .filter(member -> effectiveMemberId == null || effectiveMemberId.equals(member.getId()))
                .map(member -> contribution(projectId, safeFilter, member.getId(), member.getUsername(), member.getFullName(), asOf))
                .toList();

        Instant lastGithubSync = reportingRepository.findLastSuccessfulGithubSync(projectId);
        ReportSourceStatus githubStatus = lastGithubSync == null ? ReportSourceStatus.NOT_SYNCED : isCurrent(lastGithubSync, safeFilter.to(), asOf)
                        ? ReportSourceStatus.CURRENT
                        : ReportSourceStatus.STALE;

        List<String> warnings = new java.util.ArrayList<>();
        if (githubStatus == ReportSourceStatus.NOT_SYNCED) {
            warnings.add("GITHUB_NOT_SYNCED");
        }
        if (effectiveMemberId != null && contributions.stream().noneMatch(MemberContributionResponse::githubLinked)) {
            warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
        }

        ReportDataStatus dataStatus = githubStatus == ReportSourceStatus.CURRENT ? ReportDataStatus.COMPLETE
        : githubStatus == ReportSourceStatus.NOT_SYNCED ? ReportDataStatus.NOT_SYNCED : ReportDataStatus.PARTIAL;

        return new ReportSummaryResponse(
                projectId,
                safeFilter.sprintId(),
                effectiveMemberId,
                safeFilter.from(),
                safeFilter.to(),
                asOf,
                taskMetrics,
                contributions,
                dataStatus,
                List.of(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null),
                        new ReportSourceFreshnessResponse(ReportSource.GITHUB, githubStatus, lastGithubSync)),
                List.copyOf(warnings));
    }

    private void validateTimeRange(ReportFilterRequest filter) {
        if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
            throw new IllegalArgumentException("from must be earlier than to");
        }
    }

    private Long resolveMemberId(Long projectId, Long requestedMemberId, User currentUser) {
        if (currentUser.getRole().getCode() != RoleCode.TEAM_MEMBER) {
            return requestedMemberId;
        }

        if (!isActiveProjectMember(projectId, currentUser.getId())) {
            throw new AccessDeniedException("Bạn không phải thành viên của Project");
        }

        if (requestedMemberId != null && !requestedMemberId.equals(currentUser.getId())) {
            throw new AccessDeniedException("TEAM_MEMBER chỉ được xem thống kê của chính mình");
        }
        return currentUser.getId();
    }

    private boolean isActiveProjectMember(Long projectId, Long userId) {
        return projectRepository.findActiveMembers(projectId).stream().anyMatch(member -> userId.equals(member.getId()));
    }

    private TaskMetricsResponse calculateTaskMetrics(
            Long projectId,
            ReportFilterRequest filter,
            Long memberId,
            Instant asOf) {
        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            counts.put(status, 0L);
        }

        long total = 0;
        long completed = 0;
        long overdue = 0;

        for (ReportingRepository.StatusCountProjection row :
                reportingRepository.countTasksByStatus(projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf)) {
            TaskStatus status = TaskStatus.valueOf(row.getStatus());
            counts.put(status, row.getCount());
            total += row.getCount();
            if (status == TaskStatus.DONE) {
                completed = row.getCount();
            }
        }

        overdue = countOverdue(projectId, filter, memberId, asOf);
        return new TaskMetricsResponse(total, completed, overdue, counts);
    }

    private long countOverdue(Long projectId, ReportFilterRequest filter, Long memberId, Instant asOf) {
        return reportingRepository.countOverdueTasks(projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
    }

    private MemberContributionResponse contribution(
            Long projectId,
            ReportFilterRequest filter,
            Long memberId,
            String username,
            String fullName,
            Instant asOf) {
        boolean linked = reportingRepository.isGithubLinked(memberId);
        long commits = reportingRepository.countCommits(projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
        long pullRequests = reportingRepository.countPullRequests(projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
        long open = reportingRepository.countPullRequestsByState(projectId, filter.sprintId(), memberId, GitHubPullRequestState.OPEN.name(),filter.from(), filter.to(), asOf);
        long closed = reportingRepository.countPullRequestsByState(projectId, filter.sprintId(), memberId, GitHubPullRequestState.CLOSED.name(),filter.from(), filter.to(), asOf);
        long merged = reportingRepository.countPullRequestsByState(projectId, filter.sprintId(), memberId, GitHubPullRequestState.MERGED.name(),filter.from(), filter.to(), asOf);
        long linkedTasks = reportingRepository.countLinkedTasks(projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);

        return new MemberContributionResponse(memberId, username, fullName, linked, commits, pullRequests, open, closed, merged, linkedTasks);
    }

    private boolean isCurrent(Instant lastSyncedAt, Instant to, Instant asOf) {
        if (to != null) {
            return !lastSyncedAt.isBefore(to) && !lastSyncedAt.isAfter(asOf);
        }
        return !lastSyncedAt.isBefore(asOf.minus(DEFAULT_FRESHNESS_THRESHOLD)) && !lastSyncedAt.isAfter(asOf);
    }
}
