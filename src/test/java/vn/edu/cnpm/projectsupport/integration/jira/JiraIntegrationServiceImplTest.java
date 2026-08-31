
package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JiraIntegrationServiceImplTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long TASK_ID = 200L;

    private static final String PROJECT_KEY = "CNPM";

    private static final String BASE_URL =
            "https://example.atlassian.net";

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

    private JiraIntegrationServiceImpl service;

    private Task task;

    private IntegrationConfig config;

    @BeforeEach
    void setUp() {

        service =
                new JiraIntegrationServiceImpl(
                        configRepository,
                        secretService,
                        jiraClient,
                        jiraIssueRepository,
                        syncLogRepository,
                        taskRepository,
                        jdbcTemplate);

        task =
                new Task(
                        PROJECT_ID,
                        "Implement Jira sync",
                        "Sync Task to Jira",
                        TaskIssueType.TASK,
                        TaskPriority.MEDIUM);

        task.setSyncStatus(
                SyncStatus.NOT_SYNCED);

        config =
                new IntegrationConfig(
                        PROJECT_ID,
                        IntegrationProvider.JIRA,
                        "enc:test-secret");

        config.setBaseUrl(BASE_URL);

        config.setAccountIdentifier(
                "integration@example.com");

        when(taskRepository.findByIdForUpdate(TASK_ID))
                .thenReturn(Optional.of(task));

        when(configRepository
                .findByProjectIdAndProvider(
                        PROJECT_ID,
                        IntegrationProvider.JIRA))
                .thenReturn(Optional.of(config));

        when(jdbcTemplate.query(
                anyString(),
                ArgumentMatchers.<RowMapper<String>>any(),
                eq(PROJECT_ID)))
                .thenReturn(
                        java.util.List.of(PROJECT_KEY));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(syncLogRepository.save(any(SyncLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(jiraIssueRepository.saveAndFlush(
                any(JiraIssue.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        when(jiraIssueRepository.findByTaskId(TASK_ID))
                .thenReturn(Optional.empty());
    }

    @Test
    void taskStartsAsNotSynced() {

        assertThat(task.getSyncStatus())
                .isEqualTo(SyncStatus.NOT_SYNCED);
    }

    @Test
    void changesTaskToSyncingBeforeCallingJira() {

        AtomicReference<SyncStatus> statusWhenJiraCalled =
                new AtomicReference<>();

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenAnswer(invocation -> {

                    statusWhenJiraCalled.set(
                            task.getSyncStatus());

                    return new JiraCreateIssueResponse(
                            "10001",
                            "CNPM-100",
                            BASE_URL
                                    + "/rest/api/3/issue/10001");
                });

        service.syncTask(
                PROJECT_ID,
                TASK_ID,
                "sync-200");

        assertThat(statusWhenJiraCalled.get())
                .isEqualTo(SyncStatus.SYNCING);
    }

    @Test
    void successfulSyncStoresJiraMappingAndMarksTaskSynced() {

        JiraCreateIssueResponse jiraResponse =
                new JiraCreateIssueResponse(
                        "10001",
                        "CNPM-100",
                        BASE_URL
                                + "/rest/api/3/issue/10001");

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenReturn(jiraResponse);

        var response =
                service.syncTask(
                        PROJECT_ID,
                        TASK_ID,
                        "sync-200");

        assertThat(response.syncStatus())
                .isEqualTo(SyncStatus.SYNCED);

        assertThat(response.jiraIssueId())
                .isEqualTo("10001");

        assertThat(response.jiraIssueKey())
                .isEqualTo("CNPM-100");

        assertThat(response.jiraIssueUrl())
                .isEqualTo(
                        BASE_URL
                                + "/browse/CNPM-100");

        assertThat(response.syncedAt())
                .isNotNull();

        assertThat(task.getSyncStatus())
                .isEqualTo(SyncStatus.SYNCED);

        assertThat(task.getIdempotencyKey())
                .isEqualTo("sync-200");

        ArgumentCaptor<JiraIssue> issueCaptor =
                ArgumentCaptor.forClass(JiraIssue.class);

        verify(jiraIssueRepository)
                .saveAndFlush(issueCaptor.capture());

        JiraIssue savedIssue =
                issueCaptor.getValue();

        assertThat(savedIssue.getTaskId())
                .isEqualTo(TASK_ID);

        assertThat(savedIssue.getJiraIssueId())
                .isEqualTo("10001");

        assertThat(savedIssue.getJiraIssueKey())
                .isEqualTo("CNPM-100");

        assertThat(savedIssue.getUrl())
                .isEqualTo(
                        BASE_URL
                                + "/browse/CNPM-100");

        assertThat(savedIssue.getLastSyncedAt())
                .isNotNull();
    }

    @Test
    void failedSyncMarksTaskSyncFailed() {

        JiraConnectionException failure =
                new JiraConnectionException(
                        "Jira unavailable");

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenThrow(failure);

        var response =
                service.syncTask(
                        PROJECT_ID,
                        TASK_ID,
                        "sync-failure");

        assertThat(response.syncStatus())
                .isEqualTo(SyncStatus.SYNC_FAILED);

        assertThat(response.errorCode())
                .isEqualTo(
                        "JIRA_CONNECTION_FAILED");

        assertThat(response.retryable())
                .isTrue();

        assertThat(task.getSyncStatus())
                .isEqualTo(SyncStatus.SYNC_FAILED);

        verify(jiraIssueRepository, never())
                .saveAndFlush(any(JiraIssue.class));
    }

    @Test
    void retryFailedTaskCanSyncSuccessfully() {

        task.setSyncStatus(
                SyncStatus.SYNC_FAILED);

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenReturn(
                        new JiraCreateIssueResponse(
                                "10002",
                                "CNPM-101",
                                BASE_URL
                                        + "/rest/api/3/issue/10002"));

        var response =
                service.retryTaskSync(
                        PROJECT_ID,
                        TASK_ID,
                        "retry-200");

        assertThat(response.syncStatus())
                .isEqualTo(SyncStatus.SYNCED);

        assertThat(task.getSyncStatus())
                .isEqualTo(SyncStatus.SYNCED);

        verify(jiraClient)
                .createIssue(
                        eq(PROJECT_ID),
                        eq(PROJECT_KEY),
                        any(JiraCreateIssueRequest.class));
    }

    @Test
    void existingMappingPreventsDuplicateJiraIssue() {

        JiraIssue existingIssue =
                new JiraIssue(
                        TASK_ID,
                        "10001",
                        "CNPM-100",
                        BASE_URL + "/browse/CNPM-100",
                        Instant.parse(
                                "2026-08-25T08:00:00Z"));

        when(jiraIssueRepository.findByTaskId(TASK_ID))
                .thenReturn(Optional.of(existingIssue));

        var response =
                service.syncTask(
                        PROJECT_ID,
                        TASK_ID,
                        "same-key");

        assertThat(response.syncStatus())
                .isEqualTo(SyncStatus.SYNCED);

        assertThat(response.jiraIssueId())
                .isEqualTo("10001");

        assertThat(response.jiraIssueKey())
                .isEqualTo("CNPM-100");

        verify(jiraClient, never())
                .createIssue(
                        any(),
                        any(),
                        any());

        verify(jiraIssueRepository, never())
                .saveAndFlush(any(JiraIssue.class));
    }

    @Test
    void localTaskIsRetainedWhenJiraFails() {

        task.setTitle(
                "Original local task");

        task.setDescription(
                "Original description");

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenThrow(
                        new JiraConnectionException(
                                "Jira unavailable"));

        service.syncTask(
                PROJECT_ID,
                TASK_ID,
                "failure-key");

        assertThat(task.getTitle())
                .isEqualTo(
                        "Original local task");

        assertThat(task.getDescription())
                .isEqualTo(
                        "Original description");

        assertThat(task.getSyncStatus())
                .isEqualTo(
                        SyncStatus.SYNC_FAILED);

        verify(taskRepository, times(2))
                .save(any(Task.class));
    }

    @Test
    void createsSyncLogForEverySyncAttempt() {

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenReturn(
                        new JiraCreateIssueResponse(
                                "10001",
                                "CNPM-100",
                                BASE_URL
                                        + "/rest/api/3/issue/10001"));

        service.syncTask(
                PROJECT_ID,
                TASK_ID,
                "log-key-1");

        ArgumentCaptor<SyncLog> captor =
                ArgumentCaptor.forClass(
                        SyncLog.class);

        verify(syncLogRepository, times(2))
                .save(captor.capture());

        SyncLog log =
                captor.getValue();

        assertThat(log.getProjectId())
                .isEqualTo(PROJECT_ID);

        assertThat(log.getProvider())
                .isEqualTo(
                        IntegrationProvider.JIRA);

        assertThat(log.getEntityType())
                .isEqualTo("TASK");

        assertThat(log.getEntityId())
                .isEqualTo(
                        String.valueOf(TASK_ID));

        assertThat(log.getDirection())
                .isEqualTo(
                        vn.edu.cnpm.projectsupport
                                .integration.jira.domain
                                .SyncDirection.EXPORT);

        assertThat(log.getStatus())
                .isEqualTo(
                        SyncLogStatus.SUCCESS);

        assertThat(log.getCorrelationId())
                .isNotBlank();

        assertThat(log.getStartedAt())
                .isNotNull();

        assertThat(log.getCompletedAt())
                .isNotNull();
    }

    @Test
    void repeatedRetriesCreateOnlyOneJiraIssue() {

        JiraCreateIssueResponse jiraResponse =
                new JiraCreateIssueResponse(
                        "10001",
                        "CNPM-100",
                        BASE_URL
                                + "/rest/api/3/issue/10001");

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenThrow(
                        new JiraConnectionException(
                                "Temporary Jira outage"))
                .thenReturn(jiraResponse);

        var first =
                service.syncTask(
                        PROJECT_ID,
                        TASK_ID,
                        "stable-key");

        assertThat(first.syncStatus())
                .isEqualTo(
                        SyncStatus.SYNC_FAILED);

        assertThat(task.getSyncStatus())
                .isEqualTo(
                        SyncStatus.SYNC_FAILED);

        clearInvocations(
                jiraIssueRepository);

        when(jiraIssueRepository.findByTaskId(TASK_ID))
                .thenReturn(Optional.empty());

        var second =
                service.retryTaskSync(
                        PROJECT_ID,
                        TASK_ID,
                        "stable-key");

        assertThat(second.syncStatus())
                .isEqualTo(
                        SyncStatus.SYNCED);

        assertThat(second.jiraIssueKey())
                .isEqualTo("CNPM-100");

        assertThat(task.getSyncStatus())
                .isEqualTo(
                        SyncStatus.SYNCED);

        JiraIssue savedIssue =
                new JiraIssue(
                        TASK_ID,
                        "10001",
                        "CNPM-100",
                        BASE_URL
                                + "/browse/CNPM-100",
                        second.syncedAt());

        when(jiraIssueRepository.findByTaskId(TASK_ID))
                .thenReturn(
                        Optional.of(savedIssue));

        var third =
                service.retryTaskSync(
                        PROJECT_ID,
                        TASK_ID,
                        "stable-key");

        assertThat(third.syncStatus())
                .isEqualTo(
                        SyncStatus.SYNCED);

        assertThat(third.jiraIssueId())
                .isEqualTo("10001");

        assertThat(third.jiraIssueKey())
                .isEqualTo("CNPM-100");

        assertThat(third.jiraIssueUrl())
                .isEqualTo(
                        BASE_URL
                                + "/browse/CNPM-100");

        verify(jiraClient, times(2))
                .createIssue(
                        eq(PROJECT_ID),
                        eq(PROJECT_KEY),
                        any(JiraCreateIssueRequest.class));
    }

    @Test
    void retryIsRejectedWhenTaskIsCurrentlySyncing() {

        task.setSyncStatus(
                SyncStatus.SYNCING);

        assertThatThrownBy(() ->
                service.retryTaskSync(
                        PROJECT_ID,
                        TASK_ID,
                        "retry-key"))
                .isInstanceOf(
                        JiraClientException.class);

        verify(jiraClient, never())
                .createIssue(
                        any(),
                        any(),
                        any());
    }

    @Test
    void rejectsMissingIdempotencyKey() {

        assertThatThrownBy(() ->
                service.syncTask(
                        PROJECT_ID,
                        TASK_ID,
                        null))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "Idempotency-Key là bắt buộc");
    }

    @Test
    void syncingTaskReturnsConflictWithSyncAlreadyRunning() {

        task.setSyncStatus(
                SyncStatus.SYNCING);

        Throwable thrown =
                catchThrowable(() ->
                        service.syncTask(
                                PROJECT_ID,
                                TASK_ID,
                                "running-1"));

        assertThat(thrown)
                .isInstanceOf(
                        JiraClientException.class);

        JiraClientException exception =
                (JiraClientException) thrown;

        assertThat(exception.getStatus())
                .isEqualTo(
                        HttpStatus.CONFLICT);

        assertThat(exception.getErrorCode())
                .isEqualTo(
                        "SYNC_ALREADY_RUNNING");

        assertThat(exception.isRetryable())
                .isTrue();

        verify(jiraClient, never())
                .createIssue(
                        any(),
                        any(),
                        any());
    }

    @Test
    void createRequestContainsTaskMappingFields() {

        task.setAssigneeUserId(50L);

        task.setDeadline(
                Instant.parse(
                        "2026-09-10T15:30:00Z"));

        task.setSprintId(60L);

        task.setFeatureId(70L);

        when(jdbcTemplate.query(
                ArgumentMatchers.contains("FROM users"),
                ArgumentMatchers.<RowMapper<String>>any(),
                eq(50L)))
                .thenReturn(
                        java.util.List.of(
                                "assignee@example.com"));

        when(jdbcTemplate.query(
                ArgumentMatchers.contains("FROM sprints"),
                ArgumentMatchers.<RowMapper<String>>any(),
                eq(60L),
                eq(PROJECT_ID)))
                .thenReturn(
                        java.util.List.of("9001"));

        when(jdbcTemplate.query(
                ArgumentMatchers.contains("FROM features"),
                ArgumentMatchers.<RowMapper<String>>any(),
                eq(70L),
                eq(PROJECT_ID)))
                .thenReturn(
                        java.util.List.of(
                                "CNPM-EPIC-1"));

        when(jiraClient.createIssue(
                eq(PROJECT_ID),
                eq(PROJECT_KEY),
                any(JiraCreateIssueRequest.class)))
                .thenReturn(
                        new JiraCreateIssueResponse(
                                "10001",
                                "CNPM-100",
                                BASE_URL
                                        + "/rest/api/3/issue/10001"));

        service.syncTask(
                PROJECT_ID,
                TASK_ID,
                "mapping-1");

        ArgumentCaptor<JiraCreateIssueRequest> captor =
                ArgumentCaptor.forClass(
                        JiraCreateIssueRequest.class);

        verify(jiraClient)
                .createIssue(
                        eq(PROJECT_ID),
                        eq(PROJECT_KEY),
                        captor.capture());

        JiraCreateIssueRequest request =
                captor.getValue();

        assertThat(request.assigneeEmail())
                .isEqualTo(
                        "assignee@example.com");

        assertThat(request.dueDate())
                .startsWith(
                        "2026-09-10");

        assertThat(request.sprintId())
                .isEqualTo("9001");

        assertThat(request.epicKey())
                .isEqualTo(
                        "CNPM-EPIC-1");

        assertThat(request.labels())
                .containsExactly(
                        "cnpm-local-task-" + TASK_ID);
    }
}
