package vn.edu.cnpm.projectsupport.reporting;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
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

    @Value("${reporting.freshness-threshold:24h}")
    private Duration freshnessThreshold = DEFAULT_FRESHNESS_THRESHOLD;

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
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }

        ReportFilterRequest safeFilter = (filter == null)
                ? new ReportFilterRequest(null, null, null, null)
                : filter;
        validateTimeRange(safeFilter);

        if (safeFilter.sprintId() != null
                && sprintRepository.findByIdAndProjectId(safeFilter.sprintId(), projectId).isEmpty()) {
            throw new ResourceNotFoundException("Sprint not found: " + safeFilter.sprintId());
        }

        User currentUser = currentUserService.findCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("User is not authenticated"));

        Long effectiveMemberId = resolveMemberId(projectId, safeFilter.memberId(), currentUser);
        if (effectiveMemberId != null && !isActiveProjectMember(projectId, effectiveMemberId)) {
            throw new ResourceNotFoundException("Member not found: " + effectiveMemberId);
        }

        Instant asOf = Instant.now();
        TaskMetricsResponse taskMetrics = calculateTaskMetrics(projectId, safeFilter, effectiveMemberId, asOf);

        List<MemberContributionResponse> contributions = projectRepository.findActiveMembers(projectId)
                .stream()
                .filter(member -> effectiveMemberId == null || effectiveMemberId.equals(member.getId()))
                .map(member -> contribution(
                        projectId,
                        safeFilter,
                        member.getId(),
                        member.getUsername(),
                        member.getFullName(),
                        asOf))
                .toList();

        SyncSource github = syncSource(projectId, IntegrationProvider.GITHUB, safeFilter.to(), asOf);
        SyncSource jira = syncSource(projectId, IntegrationProvider.JIRA, safeFilter.to(), asOf);

        List<String> warnings = new ArrayList<>();
        addSourceWarning(warnings, ReportSource.GITHUB, github.status());
        addSourceWarning(warnings, ReportSource.JIRA, jira.status());

        // Cảnh báo chính xác theo từng member hoặc toàn project
        if (effectiveMemberId != null) {
            if (contributions.stream().noneMatch(MemberContributionResponse::githubLinked)) {
                warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
            }
        } else {
            if (contributions.stream().anyMatch(m -> !m.githubLinked())) {
                warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
            }
        }

        ReportDataStatus dataStatus = calculateDataStatus(jira.status(), github.status());

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
                List.of(
                        new ReportSourceFreshnessResponse(
                                ReportSource.LOCAL_TASK,
                                ReportSourceStatus.CURRENT,
                                null),
                        new ReportSourceFreshnessResponse(
                                ReportSource.JIRA,
                                jira.status(),
                                jira.lastSuccessfulSync()),
                        new ReportSourceFreshnessResponse(
                                ReportSource.GITHUB,
                                github.status(),
                                github.lastSuccessfulSync())),
                List.copyOf(warnings));
    }

    private ReportDataStatus calculateDataStatus(
            ReportSourceStatus jiraStatus,
            ReportSourceStatus githubStatus) {
        if (jiraStatus == ReportSourceStatus.NOT_SYNCED
                && githubStatus == ReportSourceStatus.NOT_SYNCED) {
            return ReportDataStatus.NOT_SYNCED;
        }
        if (jiraStatus != ReportSourceStatus.CURRENT
                || githubStatus != ReportSourceStatus.CURRENT) {
            return ReportDataStatus.PARTIAL;
        }
        return ReportDataStatus.COMPLETE;
    }

    private void addSourceWarning(
            List<String> warnings,
            ReportSource source,
            ReportSourceStatus status) {
        String prefix = source.name();
        if (status == ReportSourceStatus.SYNC_FAILED) {
            warnings.add(prefix + "_SYNC_FAILED");
        } else if (status == ReportSourceStatus.NOT_SYNCED) {
            warnings.add(prefix + "_NOT_SYNCED");
        } else if (status == ReportSourceStatus.STALE) {
            warnings.add(prefix + "_STALE");
        }
    }

    private SyncSource syncSource(
            Long projectId,
            IntegrationProvider provider,
            Instant to,
            Instant asOf) {
        Optional<ReportingRepository.LatestSyncProjection> latest =
                reportingRepository.findLatestSync(projectId, provider.name());
        Instant lastSuccessful =
                reportingRepository.findLastSuccessfulSync(projectId, provider.name());

        if (latest.isPresent()
                && "FAILED".equalsIgnoreCase(latest.get().getStatus())) {
            return new SyncSource(ReportSourceStatus.SYNC_FAILED, lastSuccessful);
        }
        if (lastSuccessful == null) {
            return new SyncSource(ReportSourceStatus.NOT_SYNCED, null);
        }

        ReportSourceStatus status = isCurrent(lastSuccessful, to, asOf)
                ? ReportSourceStatus.CURRENT
                : ReportSourceStatus.STALE;
        return new SyncSource(status, lastSuccessful);
    }

    private void validateTimeRange(ReportFilterRequest filter) {
        if (filter.from() != null
                && filter.to() != null
                && !filter.from().isBefore(filter.to())) {
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
        boolean activeMember = projectRepository.findActiveMembers(projectId)
                .stream()
                .anyMatch(member -> userId.equals(member.getId()));
        boolean activeLeader = projectRepository.findActiveLeader(projectId)
                .map(leader -> userId.equals(leader.getId()))
                .orElse(false);
        return activeMember || activeLeader;
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
        for (ReportingRepository.StatusCountProjection row :
                reportingRepository.countTasksByStatus(
                        projectId,
                        filter.sprintId(),
                        memberId,
                        filter.from(),
                        filter.to(),
                        asOf)) {
            TaskStatus status = TaskStatus.valueOf(row.getStatus());
            counts.put(status, row.getCount());
            total += row.getCount();
            if (status == TaskStatus.DONE) {
                completed = row.getCount();
            }
        }
        long overdue = countOverdue(projectId, filter, memberId, asOf);
        return new TaskMetricsResponse(total, completed, overdue, counts);
    }

    private long countOverdue(
            Long projectId,
            ReportFilterRequest filter,
            Long memberId,
            Instant asOf) {
        return reportingRepository.countOverdueTasks(
                projectId,
                filter.sprintId(),
                memberId,
                filter.from(),
                filter.to(),
                asOf);
    }

    private MemberContributionResponse contribution(
            Long projectId,
            ReportFilterRequest filter,
            Long memberId,
            String username,
            String fullName,
            Instant asOf) {
        boolean linked = reportingRepository.isGithubLinked(memberId);
        long commits = reportingRepository.countCommits(
                projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
        long pullRequests = reportingRepository.countPullRequests(
                projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
        long open = reportingRepository.countPullRequestsByState(
                projectId, filter.sprintId(), memberId,
                GitHubPullRequestState.OPEN.name(), filter.from(), filter.to(), asOf);
        long closed = reportingRepository.countPullRequestsByState(
                projectId, filter.sprintId(), memberId,
                GitHubPullRequestState.CLOSED.name(), filter.from(), filter.to(), asOf);
        long merged = reportingRepository.countPullRequestsByState(
                projectId, filter.sprintId(), memberId,
                GitHubPullRequestState.MERGED.name(), filter.from(), filter.to(), asOf);
        long linkedTasks = reportingRepository.countLinkedTasks(
                projectId, filter.sprintId(), memberId, filter.from(), filter.to(), asOf);
        return new MemberContributionResponse(
                memberId, username, fullName, linked,
                commits, pullRequests, open, closed, merged, linkedTasks);
    }

    private boolean isCurrent(Instant lastSyncedAt, Instant to, Instant asOf) {
        if (to != null) {
            return !lastSyncedAt.isBefore(to) && !lastSyncedAt.isAfter(asOf);
        }
        Duration threshold = freshnessThreshold == null
                ? DEFAULT_FRESHNESS_THRESHOLD
                : freshnessThreshold;
        return !lastSyncedAt.isBefore(asOf.minus(threshold))
                && !lastSyncedAt.isAfter(asOf);
    }

    private record SyncSource(
            ReportSourceStatus status,
            Instant lastSuccessfulSync) {
    }
}