package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfigStatus;
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
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class JiraSyncErrorAndRetryTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String PROJECT_KEY = "CNPM";
    private static final String BASE_URL = "https://example.atlassian.net";
    private static final String IDEMPOTENCY_KEY = "idemp-key-test-123456";

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

    @InjectMocks
    private JiraIntegrationServiceImpl service;

    private Task task;
    private IntegrationConfig integrationConfig;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(TASK_ID);
        task.setProjectId(PROJECT_ID);
        task.setTitle("Test Task Title");
        task.setDescription("Test Task Description");
        task.setSyncStatus(SyncStatus.NOT_SYNCED);

        integrationConfig = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, "encrypted_token");
        integrationConfig.setBaseUrl(BASE_URL);
        integrationConfig.setStatus(IntegrationConfigStatus.CONNECTED);
    }

    private void mockProjectKeyAndConfig() {
        when(taskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
        when(configRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA))
                .thenReturn(Optional.of(integrationConfig));
        when(jdbcTemplate.query(eq("SELECT jira_project_key FROM projects WHERE id = ?"), any(RowMapper.class), eq(PROJECT_ID)))
                .thenReturn(List.of(PROJECT_KEY));
    }

    @Nested
    @DisplayName("Idempotency Tests")
    class IdempotencyTests {

        @Test
        @DisplayName("Gửi lại cùng Idempotency Key khi đã hoàn tất -> Trả kết quả cũ, không gọi Jira")
        void syncTask_WhenCompletedSyncLogExists_ReturnsPreviousResult() {
            when(taskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));

            SyncLog previousSuccessLog = new SyncLog();
            previousSuccessLog.setStatus(SyncLogStatus.SUCCESS);
            previousSuccessLog.setRequestFingerprint("5a5076cf98cb28db76156e54f9d76c33c3a778e24c53d4a46a6f6df6eb69df5d");

            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    eq(PROJECT_ID), eq("TASK"), eq(String.valueOf(TASK_ID)), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(previousSuccessLog));

            JiraIssue issue = new JiraIssue(TASK_ID, "10001", "CNPM-100", BASE_URL + "/browse/CNPM-100", Instant.now());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.of(issue));

            JiraTaskSyncResponse response = service.syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);

            assertThat(response).isNotNull();
            assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);
            assertThat(response.jiraIssueKey()).isEqualTo("CNPM-100");
            verify(jiraClient, never()).createIssue(any(), any(), any());
        }

        @Test
        @DisplayName("Tái sử dụng Idempotency Key cho nội dung Task đã sửa đổi -> Ném IDEMPOTENCY_KEY_REUSED (409)")
        void syncTask_WhenReusingKeyWithModifiedData_ThrowsConflictException() {
            when(taskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));

            SyncLog previousLog = new SyncLog();
            previousLog.setStatus(SyncLogStatus.SUCCESS);
            previousLog.setRequestFingerprint("different_old_fingerprint");

            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    eq(PROJECT_ID), eq("TASK"), eq(String.valueOf(TASK_ID)), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(previousLog));

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraApiException.class)
                    .satisfies(ex -> {
                        JiraApiException apiEx = (JiraApiException) ex;
                        assertThat(apiEx.getErrorCode()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    });
        }
    }

    @Nested
    @DisplayName("Jira API Error Handling Tests")
    class JiraApiErrorTests {

        @Test
        @DisplayName("Jira API ném JiraApiException -> Cập nhật Task thành SYNC_FAILED và lưu SyncLog FAILED")
        void syncTask_WhenJiraApiThrowsException_MarksTaskFailedAndLogsError() {
            mockProjectKeyAndConfig();
            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());
            when(jiraClient.findIssuesByLabel(any(), any(), any())).thenReturn(List.of());

            JiraApiException jiraError = new JiraApiException(
                    HttpStatus.BAD_REQUEST, "JIRA_INVALID_FIELD", false, null, "Field is invalid in Jira", null);

            when(jiraClient.createIssue(eq(PROJECT_ID), eq(PROJECT_KEY), any(JiraCreateIssueRequest.class)))
                    .thenThrow(jiraError);

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY))
                    .isSameAs(jiraError);

            assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);

            ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
            verify(syncLogRepository, times(2)).save(captor.capture());
            SyncLog failedLog = captor.getAllValues().get(1);
            assertThat(failedLog.getStatus()).isEqualTo(SyncLogStatus.FAILED);
            assertThat(failedLog.getErrorCode()).isEqualTo("JIRA_INVALID_FIELD");
        }

        @Test
        @DisplayName("Sprint chưa map với Jira -> Ném SPRINT_MAPPING_MISSING (422)")
        void syncTask_WhenSprintNotMapped_ThrowsUnprocessableEntity() {
            mockProjectKeyAndConfig();
            task.setSprintId(50L);

            Sprint sprint = new Sprint();
            sprint.setJiraSprintId(null);
            when(sprintRepository.findByIdAndProjectId(50L, PROJECT_ID)).thenReturn(Optional.of(sprint));

            assertThatThrownBy(() -> service.syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraApiException.class)
                    .satisfies(ex -> {
                        JiraApiException apiEx = (JiraApiException) ex;
                        assertThat(apiEx.getErrorCode()).isEqualTo("SPRINT_MAPPING_MISSING");
                        assertThat(apiEx.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    });
        }
    }

    @Nested
    @DisplayName("Retry & Reconciliation Tests")
    class RetryTests {

        @Test
        @DisplayName("Retry Task thành công khi phát hiện Issue bằng nhãn (Label Reconcile)")
        void retryTaskSync_WhenIssueDiscoveredViaLabel_ReconcilesSuccessfully() {
            mockProjectKeyAndConfig();
            task.setSyncStatus(SyncStatus.SYNC_FAILED);

            when(syncLogRepository.findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                    any(), any(), any(), any())).thenReturn(Optional.empty());
            when(jiraIssueRepository.findByTaskId(TASK_ID)).thenReturn(Optional.empty());

            JiraCreateIssueResponse discoveredIssue = new JiraCreateIssueResponse(
                    "10009", "CNPM-200", BASE_URL + "/rest/api/3/issue/10009");
            when(jiraClient.findIssuesByLabel(eq(PROJECT_ID), eq(PROJECT_KEY), eq("cnpm-local-task-" + TASK_ID)))
                    .thenReturn(List.of(discoveredIssue));

            JiraTaskSyncResponse response = service.retryTaskSync(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);

            assertThat(response.syncStatus()).isEqualTo(SyncStatus.SYNCED);
            assertThat(response.jiraIssueKey()).isEqualTo("CNPM-200");
            assertThat(response.retryCount()).isEqualTo(2);

            verify(jiraClient, never()).createIssue(any(), any(), any());
            verify(jiraIssueRepository).saveAndFlush(any(JiraIssue.class));
            assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        }
    }
}
