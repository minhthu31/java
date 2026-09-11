
package vn.edu.cnpm.projectsupport.reporting.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.ReportCommitActivityProjection;
import vn.edu.cnpm.projectsupport.integration.github.repository.ReportPullRequestActivityProjection;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.reporting.dto.MemberContributionResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ProjectProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSource;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceFreshnessResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.SprintProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.TaskMetricsResponse;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
@Transactional(readOnly = true)
public class ProgressReportServiceImpl implements ProgressReportService {

    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final RequirementRepository requirementRepository;
    private final FeatureRepository featureRepository;
    private final TaskRepository taskRepository;

    private final GitHubRepositoryRepository githubRepositoryRepository;
    private final GitHubCommitRepository commitRepository;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final TaskCommitLinkRepository commitLinkRepository;
    private final TaskPullRequestLinkRepository pullRequestLinkRepository;
    private final UserExternalAccountRepository externalAccountRepository;

    private final SyncLogRepository syncLogRepository;
    private final CurrentUserService currentUserService;
    private final Duration freshnessThreshold;

    public ProgressReportServiceImpl(
            ProjectRepository projectRepository,
            SprintRepository sprintRepository,
            RequirementRepository requirementRepository,
            FeatureRepository featureRepository,
            TaskRepository taskRepository,
            GitHubRepositoryRepository githubRepositoryRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            TaskCommitLinkRepository commitLinkRepository,
            TaskPullRequestLinkRepository pullRequestLinkRepository,
            UserExternalAccountRepository externalAccountRepository,
            SyncLogRepository syncLogRepository,
            CurrentUserService currentUserService,
            @Value("${reporting.freshness-threshold:24h}")
            Duration freshnessThreshold) {

        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.requirementRepository = requirementRepository;
        this.featureRepository = featureRepository;
        this.taskRepository = taskRepository;

        this.githubRepositoryRepository = githubRepositoryRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.commitLinkRepository = commitLinkRepository;
        this.pullRequestLinkRepository = pullRequestLinkRepository;
        this.externalAccountRepository = externalAccountRepository;

        this.syncLogRepository = syncLogRepository;
        this.currentUserService = currentUserService;
        this.freshnessThreshold = freshnessThreshold;
    }

    @Override
    public ProjectProgressResponse getProgress(
            Long projectId,
            ReportFilterRequest filter) {

        Instant asOf = Instant.now();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project không tồn tại"));

        User current = currentUserService.findCurrentUser()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Chưa xác thực"));

        ReportFilterRequest effectiveFilter =
                normalizeFilter(filter);

        Long effectiveMemberId =
                current.getRole().getCode() == RoleCode.TEAM_MEMBER
                        ? current.getId()
                        : effectiveFilter.memberId();

        authorize(
                projectId,
                current,
                effectiveMemberId);

        validateSprintAndMember(
                projectId,
                effectiveFilter,
                effectiveMemberId);

        /*
         * Lấy toàn bộ Task của Project.
         *
         * Nếu có member filter thì chỉ lấy Task của member đó.
         *
         * from/to áp dụng theo createdAt của Task.
         */
        List<Task> allTasks =
                taskRepository.findByProjectId(projectId)
                        .stream()
                        .filter(task ->
                                effectiveMemberId == null
                                        || effectiveMemberId.equals(
                                                task.getAssigneeUserId()))
                        .filter(timePredicate(
                                effectiveFilter.from(),
                                effectiveFilter.to(),
                                Task::getCreatedAt))
                        .toList();

        TaskMetricsResponse overall =
                taskMetrics(allTasks, asOf);

        /*
         * Tiến độ theo Sprint.
         *
         * Sprint filter chỉ giới hạn Sprint được trả về.
         */
        List<SprintProgressResponse> sprintProgress =
                sprintRepository.findByProjectId(projectId)
                        .stream()
                        .filter(sprint ->
                                effectiveFilter.sprintId() == null
                                        || effectiveFilter.sprintId().equals(
                                                sprint.getId()))
                        .map(sprint -> {

                            List<Task> sprintTasks =
                                    allTasks.stream()
                                            .filter(task ->
                                                    sprint.getId().equals(
                                                            task.getSprintId()))
                                            .toList();

                            TaskMetricsResponse metrics =
                                    taskMetrics(
                                            sprintTasks,
                                            asOf);

                            return new SprintProgressResponse(
                                    sprint.getId(),
                                    sprint.getName(),
                                    metrics.totalTasks(),
                                    metrics.completedTasks(),
                                    metrics.overdueTasks(),
                                    percent(
                                            metrics.completedTasks(),
                                            metrics.totalTasks()));
                        })
                        .toList();

        /*
         * Requirement và Feature chỉ được dùng để tổng hợp
         * tiến độ Project/Sprint.
         */
        long requirements =
                effectiveFilter.sprintId() == null
                        ? requirementCount(projectId)
                        : requirementCountForTasks(allTasks);

        long features =
                effectiveFilter.sprintId() == null
                        ? featureRepository
                                .findByProjectId(projectId)
                                .size()
                        : allTasks.stream()
                                .map(Task::getFeatureId)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .count();

        long sprints = sprintProgress.size();

        return new ProjectProgressResponse(
                project.getId(),
                requirements,
                features,
                sprints,
                overall.totalTasks(),
                overall.completedTasks(),
                overall.overdueTasks(),
                percent(
                        overall.completedTasks(),
                        overall.totalTasks()),
                sprintProgress,
                asOf);
    }

    @Override
    public ReportSummaryResponse getSummary(
            Long projectId,
            ReportFilterRequest filter) {

        Instant asOf = Instant.now();

        Project project =
                projectRepository.findById(projectId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Project không tồn tại"));

        User current =
                currentUserService.findCurrentUser()
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Chưa xác thực"));

        ReportFilterRequest effectiveFilter =
                normalizeFilter(filter);

        Long effectiveMemberId =
                current.getRole().getCode() == RoleCode.TEAM_MEMBER
                        ? current.getId()
                        : effectiveFilter.memberId();

        authorize(
                projectId,
                current,
                effectiveMemberId);

        validateSprintAndMember(
                projectId,
                effectiveFilter,
                effectiveMemberId);

        /*
         * Task được lọc theo:
         * - Project
         * - Sprint nếu có
         * - Member nếu có
         * - Khoảng thời gian nếu có
         */
        List<Task> tasks =
                taskRepository.findByProjectId(projectId)
                        .stream()
                        .filter(task ->
                                effectiveFilter.sprintId() == null
                                        || effectiveFilter.sprintId().equals(
                                                task.getSprintId()))
                        .filter(task ->
                                effectiveMemberId == null
                                        || effectiveMemberId.equals(
                                                task.getAssigneeUserId()))
                        .filter(timePredicate(
                                effectiveFilter.from(),
                                effectiveFilter.to(),
                                Task::getCreatedAt))
                        .toList();

        TaskMetricsResponse metrics =
                taskMetrics(tasks, asOf);

        List<MemberContributionResponse> contributions =
                buildMemberContributions(
                        projectId,
                        effectiveFilter,
                        effectiveMemberId,
                        tasks);

        List<ReportSourceFreshnessResponse> sources =
                List.of(
                        new ReportSourceFreshnessResponse(
                                ReportSource.LOCAL_TASK,
                                ReportSourceStatus.CURRENT,
                                null),

                        sourceStatus(
                                projectId,
                                IntegrationProvider.JIRA,
                                ReportSource.JIRA,
                                effectiveFilter.to(),
                                asOf),

                        sourceStatus(
                                projectId,
                                IntegrationProvider.GITHUB,
                                ReportSource.GITHUB,
                                effectiveFilter.to(),
                                asOf));

        ReportDataStatus dataStatus =
                dataStatus(sources);

        List<String> warnings =
                warnings(
                        sources,
                        contributions,
                        effectiveMemberId);

        return new ReportSummaryResponse(
                project.getId(),
                effectiveFilter.sprintId(),
                effectiveMemberId,
                effectiveFilter.from(),
                effectiveFilter.to(),
                asOf,
                metrics,
                contributions,
                dataStatus,
                sources,
                warnings);
    }

    private ReportFilterRequest normalizeFilter(
            ReportFilterRequest filter) {

        if (filter == null) {
            return new ReportFilterRequest(
                    null,
                    null,
                    null,
                    null);
        }

        return filter;
    }

    private void validateSprintAndMember(
            Long projectId,
            ReportFilterRequest filter,
            Long effectiveMemberId) {

        if (filter.sprintId() != null) {
            sprintRepository.findByIdAndProjectId(
                            filter.sprintId(),
                            projectId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Sprint không thuộc Project"));
        }

        /*
         * Member không tồn tại hoặc không còn active
         * không làm report bị lỗi nếu Task không có assignee.
         *
         * Chỉ validate memberId khi client thực sự yêu cầu
         * lọc theo member.
         */
        if (effectiveMemberId != null) {
            boolean activeMember = projectRepository.findActiveMembers(projectId)
                    .stream()
                    .anyMatch(member -> effectiveMemberId.equals(member.getId()));
            boolean activeLeader = projectRepository.findActiveLeader(projectId)
                    .map(leader -> effectiveMemberId.equals(leader.getId()))
                    .orElse(false);

            if (!activeMember && !activeLeader) {
                throw new ResourceNotFoundException(
                        "Member không active trong Project");
            }
        }
    }

    private long requirementCount(Long projectId) {
        return requirementRepository.countByProjectId(projectId);
    }

    private long requirementCountForTasks(
            List<Task> tasks) {

        return tasks.stream()
                .map(Task::getRequirementId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    private double percent(
            long completed,
            long total) {

        if (total == 0) {
            return 0.0;
        }

        return Math.round(
                completed * 10000.0 / total)
                / 100.0;
    }

    private void authorize(
            Long projectId,
            User user,
            Long requestedMemberId) {

        RoleCode role =
                user.getRole().getCode();

        boolean allowed =
                switch (role) {

                    case ADMIN -> true;

                    case TEAM_LEADER ->
                            projectRepository.countActiveLeader(
                                    projectId,
                                    user.getId()) > 0;

                    case LECTURER ->
                            projectRepository.countAssignedLecturer(
                                    projectId,
                                    user.getId()) > 0;

                    case TEAM_MEMBER ->
                            projectRepository.countActiveMember(
                                    projectId,
                                    user.getId()) > 0;
                };

        if (!allowed) {
            throw new AccessDeniedException(
                    "Không có quyền xem báo cáo Project");
        }

        if (role == RoleCode.TEAM_MEMBER
                && requestedMemberId != null
                && !user.getId().equals(requestedMemberId)) {

            throw new AccessDeniedException(
                    "TEAM_MEMBER chỉ được xem số liệu của chính mình");
        }
    }

    /**
     * Tổng hợp Task theo trạng thái.
     *
     * Công thức:
     *
     * totalTasks      = tổng số Task
     * completedTasks  = Task có status DONE
     * overdueTasks    = deadline < thời điểm báo cáo
     *                   và status không DONE/CANCELLED
     *
     * Task không có deadline:
     * - vẫn được tính vào totalTasks
     * - vẫn được tính theo status
     * - không được tính overdue
     *
     * Task không có assignee:
     * - vẫn được tính vào báo cáo
     * - không gây NullPointerException
     */
    private TaskMetricsResponse taskMetrics(
            List<Task> tasks,
            Instant asOf) {

        EnumMap<TaskStatus, Long> byStatus =
                new EnumMap<>(TaskStatus.class);

        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, 0L);
        }

        long completed = 0;
        long overdue = 0;

        for (Task task : tasks) {

            TaskStatus status = task.getStatus();

            /*
             * Status null không làm report lỗi.
             */
            if (status != null) {

                byStatus.merge(
                        status,
                        1L,
                        Long::sum);

                if (status == TaskStatus.DONE) {
                    completed++;
                }
            }

            /*
             * Task quá hạn:
             *
             * 1. Có deadline
             * 2. Deadline đã qua
             * 3. Chưa DONE
             * 4. Chưa CANCELLED
             */
            if (task.getDeadline() != null
                    && task.getDeadline().isBefore(asOf)
                    && status != TaskStatus.DONE
                    && status != TaskStatus.CANCELLED) {

                overdue++;
            }
        }

        return new TaskMetricsResponse(
                tasks.size(),
                completed,
                overdue,
                byStatus);
    }

    private List<MemberContributionResponse>
    buildMemberContributions(
            Long projectId,
            ReportFilterRequest filter,
            Long effectiveMemberId,
            List<Task> scopedTasks) {

        Map<Long, ProjectRepository.ActiveMemberProjection> members =
                new LinkedHashMap<>();

        projectRepository.findActiveMembers(projectId)
                .forEach(member -> members.put(member.getId(), member));
        projectRepository.findActiveLeader(projectId)
                .ifPresent(leader -> members.putIfAbsent(leader.getId(), leader));

        Long taskMemberId = effectiveMemberId;
        List<ReportCommitActivityProjection> commitRows =
                commitRepository.findReportActivity(
                        projectId,
                        effectiveMemberId,
                        filter.from(),
                        filter.to(),
                        filter.sprintId(),
                        taskMemberId);
        List<ReportPullRequestActivityProjection> prRows =
                pullRequestRepository.findReportActivity(
                        projectId,
                        effectiveMemberId,
                        filter.from(),
                        filter.to(),
                        filter.sprintId(),
                        taskMemberId);

        Map<Long, Long> commitUserByActivity = new HashMap<>();
        Map<Long, Set<Long>> commitTasksByUser = new HashMap<>();
        for (ReportCommitActivityProjection row : commitRows) {
            if (row.getActivityId() == null || row.getUserId() == null) {
                continue;
            }
            commitUserByActivity.put(row.getActivityId(), row.getUserId());
            if (row.getTaskId() != null) {
                commitTasksByUser.computeIfAbsent(row.getUserId(), ignored -> new HashSet<>())
                        .add(row.getTaskId());
            }
        }

        Map<Long, Long> prUserByActivity = new HashMap<>();
        Map<Long, Set<Long>> prTasksByUser = new HashMap<>();
        for (ReportPullRequestActivityProjection row : prRows) {
            if (row.getActivityId() == null || row.getUserId() == null) {
                continue;
            }
            prUserByActivity.put(row.getActivityId(), row.getUserId());
            if (row.getTaskId() != null) {
                prTasksByUser.computeIfAbsent(row.getUserId(), ignored -> new HashSet<>())
                        .add(row.getTaskId());
            }
        }

        List<MemberContributionResponse> result = new ArrayList<>();
        for (ProjectRepository.ActiveMemberProjection member : members.values()) {
            if (effectiveMemberId != null && !effectiveMemberId.equals(member.getId())) {
                continue;
            }

            Set<Long> taskUnion = new HashSet<>(
                    commitTasksByUser.getOrDefault(member.getId(), Set.of()));
            taskUnion.addAll(prTasksByUser.getOrDefault(member.getId(), Set.of()));

            long commitsCount = commitUserByActivity.values().stream()
                    .filter(member.getId()::equals)
                    .count();
            long prCount = prUserByActivity.values().stream()
                    .filter(member.getId()::equals)
                    .count();

            result.add(new MemberContributionResponse(
                    member.getId(),
                    member.getUsername(),
                    member.getFullName(),
                    commitsCount,
                    prCount,
                    taskUnion.size()));
        }

        return result;
    }

    private ReportSourceFreshnessResponse sourceStatus(
            Long projectId,
            IntegrationProvider provider,
            ReportSource source,
            Instant to,
            Instant asOf) {

        List<SyncLog> logs =
                syncLogRepository
                        .findByProjectIdOrderByStartedAtDesc(
                                projectId,
                                Pageable.unpaged())
                        .getContent()
                        .stream()
                        .filter(log ->
                                log.getProvider() == provider)
                        .toList();

        SyncLog latestSuccess =
                logs.stream()
                        .filter(log ->
                                log.getStatus()
                                        == SyncLogStatus.SUCCESS)
                        .findFirst()
                        .orElse(null);

        SyncLog latest =
                logs.isEmpty()
                        ? null
                        : logs.get(0);

        if (latestSuccess == null) {

            return new ReportSourceFreshnessResponse(
                    source,
                    latest != null
                            && latest.getStatus()
                            == SyncLogStatus.FAILED
                            ? ReportSourceStatus.SYNC_FAILED
                            : ReportSourceStatus.NOT_SYNCED,
                    null);
        }

        Instant syncedAt =
                latestSuccess.getCompletedAt() != null
                        ? latestSuccess.getCompletedAt()
                        : latestSuccess.getStartedAt();

        boolean stale;

        if (to != null) {
            stale = syncedAt.isBefore(to);
        } else {
            stale =
                    syncedAt
                            .plus(freshnessThreshold)
                            .isBefore(asOf);
        }

        ReportSourceStatus status =
                stale
                        ? ReportSourceStatus.STALE
                        : ReportSourceStatus.CURRENT;

        if (latest != null
                && latest.getStatus()
                == SyncLogStatus.FAILED
                && latest.getStartedAt() != null
                && latest.getStartedAt()
                .isAfter(syncedAt)) {

            status =
                    ReportSourceStatus.SYNC_FAILED;
        }

        return new ReportSourceFreshnessResponse(
                source,
                status,
                syncedAt);
    }

    private ReportDataStatus dataStatus(
            List<ReportSourceFreshnessResponse> sources) {

        boolean unsynced =
                sources.stream()
                        .anyMatch(source ->
                                source.source()
                                        != ReportSource.LOCAL_TASK
                                        && source.status()
                                        == ReportSourceStatus.NOT_SYNCED);

        if (unsynced) {
            return ReportDataStatus.NOT_SYNCED;
        }

        boolean partial =
                sources.stream()
                        .anyMatch(source ->
                                source.status()
                                        == ReportSourceStatus.STALE
                                        || source.status()
                                        == ReportSourceStatus.SYNC_FAILED);

        return partial
                ? ReportDataStatus.PARTIAL
                : ReportDataStatus.COMPLETE;
    }

    private List<String> warnings(
            List<ReportSourceFreshnessResponse> sources,
            List<MemberContributionResponse> contributions,
            Long memberId) {

        List<String> warnings =
                new ArrayList<>();

        for (ReportSourceFreshnessResponse source :
                sources) {

            if (source.source()
                    == ReportSource.GITHUB) {

                if (source.status()
                        == ReportSourceStatus.NOT_SYNCED) {

                    warnings.add(
                            "GITHUB_NOT_SYNCED");
                }

                if (source.status()
                        == ReportSourceStatus.SYNC_FAILED) {

                    warnings.add(
                            "GITHUB_SYNC_FAILED");
                }
            }

            if (source.source()
                    == ReportSource.JIRA
                    && source.status()
                    == ReportSourceStatus.SYNC_FAILED) {

                warnings.add(
                        "JIRA_SYNC_FAILED");
            }
        }

        if (memberId != null
                && externalAccountRepository
                        .findByUserIdAndProvider(
                                memberId,
                                IntegrationProvider.GITHUB)
                        .filter(account ->
                                account.getExternalUserId() != null
                                        && !account.getExternalUserId().isBlank())
                        .isEmpty()) {

            warnings.add("GITHUB_ACCOUNT_NOT_LINKED");
        }

        return warnings;
    }

    /**
     * Không truyền from/to:
     * lấy toàn bộ dữ liệu.
     *
     * Có ít nhất một mốc:
     * áp dụng khoảng [from, to).
     *
     * Timestamp null chỉ bị loại khi thực sự
     * đang áp dụng filter thời gian.
     */
    private <T> Predicate<T> timePredicate(
            Instant from,
            Instant to,
            Function<T, Instant> getter) {

        if (from == null && to == null) {
            return item -> true;
        }

        return item ->
                inRange(
                        getter.apply(item),
                        from,
                        to);
    }

    private boolean inRange(
            Instant value,
            Instant from,
            Instant to) {

        if (value == null) {
            return false;
        }

        return (from == null
                || !value.isBefore(from))
                && (to == null
                || value.isBefore(to));
    }
}
