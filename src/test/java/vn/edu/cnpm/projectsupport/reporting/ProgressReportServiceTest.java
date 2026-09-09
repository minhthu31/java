package vn.edu.cnpm.projectsupport.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.service.ProgressReportServiceImpl;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class ProgressReportServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    SprintRepository sprintRepository;

    @Mock
    FeatureRepository featureRepository;

    @Mock
    vn.edu.cnpm.projectsupport.requirement.RequirementRepository requirementRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    GitHubRepositoryRepository githubRepositoryRepository;

    @Mock
    GitHubCommitRepository commitRepository;

    @Mock
    GitHubPullRequestRepository pullRequestRepository;

    @Mock
    TaskCommitLinkRepository commitLinkRepository;

    @Mock
    TaskPullRequestLinkRepository pullRequestLinkRepository;

    @Mock
    UserExternalAccountRepository externalAccountRepository;

    @Mock
    SyncLogRepository syncLogRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    User user;

    @Mock
    Role role;

    private ProgressReportServiceImpl service;

    @BeforeEach
    void setUp() {

        service = new ProgressReportServiceImpl(
                projectRepository,
                sprintRepository,
                requirementRepository,
                featureRepository,
                taskRepository,
                githubRepositoryRepository,
                commitRepository,
                pullRequestRepository,
                commitLinkRepository,
                pullRequestLinkRepository,
                externalAccountRepository,
                syncLogRepository,
                currentUserService,
                Duration.ofHours(24));

        Project project =
                org.mockito.Mockito.mock(Project.class);

        when(project.getId())
                .thenReturn(1L);

        when(projectRepository.findById(1L))
                .thenReturn(Optional.of(project));

        when(currentUserService.findCurrentUser())
                .thenReturn(Optional.of(user));

        when(user.getId())
                .thenReturn(7L);

        when(user.getRole())
                .thenReturn(role);

        when(role.getCode())
                .thenReturn(RoleCode.TEAM_LEADER);

        when(projectRepository.countActiveLeader(
                1L,
                7L))
                .thenReturn(1L);

        /*
         * Các mock bên dưới chỉ phục vụ phần GitHub/source status
         * của report.
         *
         * Một số test, ví dụ getProgress(), không sử dụng chúng.
         * Dùng lenient để tránh Mockito báo UnnecessaryStubbing
         * trong các test không cần dữ liệu GitHub.
         */
        lenient().when(projectRepository.findActiveMembers(1L))
                .thenReturn(List.of());

        lenient().when(githubRepositoryRepository
                .findByProjectIdOrderByFullNameAsc(
                        any(),
                        any(Pageable.class)))
                .thenReturn(
                        new PageImpl<>(List.of()));

        lenient().when(commitRepository.findAll())
                .thenReturn(List.of());

        lenient().when(pullRequestRepository.findAll())
                .thenReturn(List.of());

        lenient().when(externalAccountRepository.findAll())
                .thenReturn(List.of());

        lenient().when(syncLogRepository
                .findByProjectIdOrderByStartedAtDesc(
                        1L,
                        Pageable.unpaged()))
                .thenReturn(
                        new PageImpl<>(List.of()));
    }

    /**
     * AC:
     * Thống kê tổng số Task theo từng trạng thái.
     *
     * Đồng thời kiểm tra:
     * - Task DONE được tính completed.
     * - Task quá hạn được tính overdue.
     * - Task thiếu deadline không được tính overdue.
     */
    @Test
    void countsEveryStatusAndDoesNotTreatMissingDeadlineAsOverdue() {

        Instant now = Instant.now();

        Task todo =
                task(
                        TaskStatus.TO_DO,
                        null);

        Task done =
                task(
                        TaskStatus.DONE,
                        now.minusSeconds(3600));

        Task overdue =
                task(
                        TaskStatus.IN_PROGRESS,
                        now.minusSeconds(3600));

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(
                                todo,
                                done,
                                overdue));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(3);

        assertThat(
                report.taskMetrics().completedTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().overdueTasks())
                .isEqualTo(1);

        /*
         * Kiểm tra đầy đủ các status.
         */
        assertThat(
                report.taskMetrics().tasksByStatus())
                .containsEntry(
                        TaskStatus.TO_DO,
                        1L)
                .containsEntry(
                        TaskStatus.DONE,
                        1L)
                .containsEntry(
                        TaskStatus.IN_PROGRESS,
                        1L)
                .containsEntry(
                        TaskStatus.BLOCKED,
                        0L)
                .containsEntry(
                        TaskStatus.CANCELLED,
                        0L);

        /*
         * Tổng các status phải bằng tổng Task.
         */
        assertThat(
                report.taskMetrics()
                        .tasksByStatus()
                        .values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum())
                .isEqualTo(
                        report.taskMetrics()
                                .totalTasks());

        /*
         * Task chưa hoàn thành = total - completed.
         */
        long notCompleted =
                report.taskMetrics().totalTasks()
                        - report.taskMetrics().completedTasks();

        assertThat(notCompleted)
                .isEqualTo(2);
    }

    /**
     * AC:
     * Project không có Task phải trả về số liệu 0,
     * không được trả về status map null.
     */
    @Test
    void emptyProjectReturnsZeroAndNotNullStatusMap() {

        when(taskRepository.findByProjectId(1L))
                .thenReturn(List.of());

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isZero();

        assertThat(
                report.taskMetrics().completedTasks())
                .isZero();

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();

        assertThat(
                report.taskMetrics().tasksByStatus())
                .isNotNull()
                .hasSize(
                        TaskStatus.values().length);

        assertThat(report.dataStatus())
                .isEqualTo(
                        ReportDataStatus.NOT_SYNCED);

        assertThat(report.sources())
                .anyMatch(
                        source ->
                                source.source()
                                        .name()
                                        .equals("GITHUB")
                                        && source.status()
                                        == ReportSourceStatus.NOT_SYNCED);
    }

    /**
     * AC:
     * Task thiếu assignee không được làm báo cáo bị lỗi.
     */
    @Test
    void taskWithoutAssigneeDoesNotBreakReport() {

        Instant now = Instant.now();

        Task unassigned =
                task(
                        TaskStatus.IN_PROGRESS,
                        now.plusSeconds(3600));

        /*
         * Không có assignee là trường hợp hợp lệ
         * đối với báo cáo.
         */
        unassigned.setAssigneeUserId(null);

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(unassigned));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().completedTasks())
                .isZero();

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();
    }

    /**
     * Task có deadline nhưng đã hoàn thành
     * không được tính overdue.
     */
    @Test
    void completedTaskIsNotOverdueEvenWhenDeadlinePassed() {

        Instant now = Instant.now();

        Task completed =
                task(
                        TaskStatus.DONE,
                        now.minusSeconds(3600));

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(completed));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().completedTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();
    }

    /**
     * Task CANCELLED có deadline đã qua
     * cũng không được tính overdue.
     */
    @Test
    void cancelledTaskIsNotOverdueEvenWhenDeadlinePassed() {

        Instant now = Instant.now();

        Task cancelled =
                task(
                        TaskStatus.CANCELLED,
                        now.minusSeconds(3600));

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(cancelled));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().completedTasks())
                .isZero();

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();
    }

    /**
     * AC:
     * Task có deadline trong tương lai không bị tính overdue.
     */
    @Test
    void futureDeadlineIsNotOverdue() {

        Instant now = Instant.now();

        Task task =
                task(
                        TaskStatus.IN_PROGRESS,
                        now.plusSeconds(3600));

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(task));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().completedTasks())
                .isZero();

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();
    }

    /**
     * AC:
     * Tiến độ phải được tính theo công thức:
     *
     * completed / total * 100
     *
     * Project có 2 Task, trong đó 1 DONE
     * => tiến độ = 50%.
     */
    @Test
    void projectProgressIsCalculatedFromCompletedTasks() {

        Instant now = Instant.now();

        Task done =
                task(
                        TaskStatus.DONE,
                        now.plusSeconds(3600));

        Task inProgress =
                task(
                        TaskStatus.IN_PROGRESS,
                        now.plusSeconds(3600));

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(
                                done,
                                inProgress));

        var report =
                service.getProgress(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(report.totalTasks())
                .isEqualTo(2);

        assertThat(report.completedTasks())
                .isEqualTo(1);

        assertThat(report.overdueTasks())
                .isZero();

        assertThat(report.progressPercent())
                .isEqualTo(50.0);
    }

    /**
     * Task không có status không được làm report crash.
     */
    @Test
    void taskWithoutStatusDoesNotBreakReport() {

        Task task =
                task(
                        null,
                        null);

        when(taskRepository.findByProjectId(1L))
                .thenReturn(
                        List.of(task));

        var report =
                service.getSummary(
                        1L,
                        new ReportFilterRequest(
                                null,
                                null,
                                null,
                                null));

        assertThat(
                report.taskMetrics().totalTasks())
                .isEqualTo(1);

        assertThat(
                report.taskMetrics().completedTasks())
                .isZero();

        assertThat(
                report.taskMetrics().overdueTasks())
                .isZero();
    }

    private Task task(
            TaskStatus status,
            Instant deadline) {

        Task task =
                new Task(
                        1L,
                        "task",
                        "criteria",
                        TaskIssueType.TASK,
                        TaskPriority.MEDIUM);

        task.setStatus(status);
        task.setDeadline(deadline);

        /*
         * Assignee null là dữ liệu hợp lệ.
         */
        task.setAssigneeUserId(null);

        return task;
    }
}