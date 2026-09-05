package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JiraSyncReliabilityContractTest {

    private static final Long PROJECT_ID = 9201L;
    private static final Long TASK_ID = 9202L;
    private static final String PROJECT_KEY = "CNPM";
    private static final String BASE_URL = "https://example.atlassian.net";

    @Mock
    private IntegrationConfigRepository configRepository;
    @Mock
    private IntegrationSecretService secretService;
    @Mock
    private JiraClient jiraClient;
    @Mock
    private JiraIssueRepository jiraIssueRepository;
    @Mock
    private SyncLogRepository syncLogRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private FeatureRepository featureRepository;

    private JiraIntegrationServiceImpl service;
    private Task task;

    @BeforeEach
    void setUp() {
        service = new JiraIntegrationServiceImpl(
                configRepository,
                secretService,
                jiraClient,
                jiraIssueRepository,
                syncLogRepository,
                taskRepository,
                jdbcTemplate,
                sprintRepository,
                featureRepository);

        task = new Task(
                PROJECT_ID,
                "CNPM-85 Jira reliability",
                "RBAC, error, retry and idempotency must be tested",
                TaskIssueType.TASK,
                TaskPriority.HIGH);
        task.setDescription("Test Jira synchronization reliability");
        task.setSyncStatus(SyncStatus.NOT_SYNCED);

        IntegrationConfig config = new IntegrationConfig(
                PROJECT_ID,
                IntegrationProvider.JIRA,
                "encrypted-secret");
        config.setBaseUrl(BASE_URL);
        config.setAccountIdentifier("jira-account@example.com");

        when(taskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(configRepository.findByProjectIdAndProvider(
                PROJECT_ID, IntegrationProvider.JIRA))
                .thenReturn(Optional.of(config));
        when(jdbcTemplate.query(
                anyString(),
                ArgumentMatchers.<RowMapper<String>>any(),
                eq(PROJECT_ID)))
                .thenReturn(List.of(PROJECT_KEY));
        when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());
        when(jiraIssueRepository.saveAndFlush(any(JiraIssue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                any(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(syncLogRepository.save(any(SyncLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jiraClient.findIssuesByLabel(PROJECT_ID, PROJECT_KEY, "cnpm-local-task-" + TASK_ID))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("Cùng Idempotency-Key trả kết quả đã lưu và không gọi Jira lần hai")
    void completedIdempotentRequestIsReplayedWithoutSecondJiraCall() {
        String key = "cnpm-85-idempotent-success";
        CompletedSync completed = performSuccessfulSyncAndCapture(key);

        when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                PROJECT_ID, "TASK", String.valueOf(TASK_ID), key))
                .thenReturn(Optional.of(completed.log()));
        when(jiraIssueRepository.findByTaskId(TASK_ID))
                .thenReturn(Optional.of(completed.issue()));
        clearInvocations(jiraClient);

        var replay = service.syncTask(PROJECT_ID, TASK_ID, key);

        assertThat(replay.syncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(replay.jiraIssueKey()).isEqualTo("CNPM-9202");
        verify(jiraClient, never()).createIssue(any(), anyString(), any());
        verify(jiraClient, never()).updateIssue(any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Tái sử dụng Idempotency-Key sau khi payload thay đổi trả 409")
    void reusedIdempotencyKeyForChangedTaskIsRejected() {
        String key = "cnpm-85-idempotent-conflict";
        CompletedSync completed = performSuccessfulSyncAndCapture(key);

        task.setTitle("CNPM-85 payload has changed");
        when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                PROJECT_ID, "TASK", String.valueOf(TASK_ID), key))
                .thenReturn(Optional.of(completed.log()));
        clearInvocations(jiraClient);

        assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, key))
                .isInstanceOf(JiraApiException.class)
                .satisfies(throwable -> {
                    JiraApiException exception = (JiraApiException) throwable;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getErrorCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                    assertThat(exception.isRetryable()).isFalse();
                });

        verify(jiraClient, never()).createIssue(any(), anyString(), any());
        verify(jiraClient, never()).updateIssue(any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Jira rate limit đánh dấu Task và SyncLog FAILED, giữ metadata retry")
    void jiraRateLimitMarksTaskAndSyncLogFailed() {
        JiraRateLimitException rateLimit = new JiraRateLimitException(
                "Jira rate limit reached",
                Duration.ofSeconds(45));
        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenThrow(rateLimit);

        assertThatThrownBy(() -> service.syncTask(
                PROJECT_ID, TASK_ID, "cnpm-85-rate-limit"))
                .isSameAs(rateLimit)
                .satisfies(throwable -> {
                    JiraApiException exception = (JiraApiException) throwable;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getErrorCode()).isEqualTo("JIRA_RATE_LIMITED");
                    assertThat(exception.isRetryable()).isTrue();
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(45L);
                });

        assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);
        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        SyncLog failedLog = logCaptor.getAllValues().getLast();
        assertThat(failedLog.getStatus()).isEqualTo(SyncLogStatus.FAILED);
        assertThat(failedLog.getErrorCode()).isEqualTo("JIRA_RATE_LIMITED");
        assertThat(failedLog.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Retry reconcile Jira Issue theo label, không tạo Issue trùng")
    void retryReconcilesExistingIssueWithoutCreatingDuplicate() {
        task.setSyncStatus(SyncStatus.SYNC_FAILED);
        when(jiraClient.findIssuesByLabel(
                PROJECT_ID, PROJECT_KEY, "cnpm-local-task-" + TASK_ID))
                .thenReturn(List.of(new JiraCreateIssueResponse(
                        "109202",
                        "CNPM-9202",
                        BASE_URL + "/rest/api/3/issue/109202")));

        var response = service.retryTaskSync(
                PROJECT_ID, TASK_ID, "cnpm-85-retry-reconcile");

        assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(response.jiraIssueKey()).isEqualTo("CNPM-9202");
        assertThat(response.attempt()).isEqualTo(2);
        assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        verify(jiraClient, never()).createIssue(any(), anyString(), any());
        verify(jiraIssueRepository).saveAndFlush(any(JiraIssue.class));
    }

    private CompletedSync performSuccessfulSyncAndCapture(String key) {
        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenReturn(new JiraCreateIssueResponse(
                        "109202",
                        "CNPM-9202",
                        BASE_URL + "/rest/api/3/issue/109202"));

        var response = service.syncTask(PROJECT_ID, TASK_ID, key);
        assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);

        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        SyncLog completedLog = logCaptor.getAllValues().getLast();
        assertThat(completedLog.getStatus()).isEqualTo(SyncLogStatus.SUCCESS);

        ArgumentCaptor<JiraIssue> issueCaptor = ArgumentCaptor.forClass(JiraIssue.class);
        verify(jiraIssueRepository).saveAndFlush(issueCaptor.capture());
        return new CompletedSync(completedLog, issueCaptor.getValue());
    }

    private record CompletedSync(SyncLog log, JiraIssue issue) {
    }
}
