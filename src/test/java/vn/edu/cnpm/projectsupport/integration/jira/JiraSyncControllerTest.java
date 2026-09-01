package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;

@ExtendWith(MockitoExtension.class)
class JiraSyncControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JiraIntegrationService jiraIntegrationService;

    @InjectMocks
    private JiraIntegrationController jiraIntegrationController;

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String IDEMPOTENCY_KEY = "idemp-key-test-123456";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(jiraIntegrationController).build();
    }

    @Nested
    @DisplayName("Tests for Sync & Retry Task to Jira Endpoints")
    class SyncEndpointTests {

        @Test
        @DisplayName("POST /sync thành công -> Trả về 200 OK kèm dữ liệu ApiResponse")
        void syncTaskToJira_WhenSuccess_ReturnsOk() throws Exception {
            JiraTaskSyncResponse successResponse = new JiraTaskSyncResponse(
                    TASK_ID,
                    SyncStatus.SYNCED,
                    "10001",
                    "CNPM-100",
                    "https://example.atlassian.net/browse/CNPM-100",
                    1,
                    false,
                    Instant.now(),
                    null,
                    "Đồng bộ Task lên Jira thành công"
            );

            when(jiraIntegrationService.syncTask(eq(PROJECT_ID), eq(TASK_ID), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(successResponse);

            mockMvc.perform(post("/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}/sync", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskId").value(TASK_ID))
                    .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"))
                    .andExpect(jsonPath("$.data.jiraIssueKey").value("CNPM-100"));
        }

        @Test
        @DisplayName("POST /sync thiếu Header Idempotency-Key -> Trả về 400 Bad Request")
        void syncTaskToJira_WhenMissingIdempotencyKeyHeader_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}/sync", PROJECT_ID, TASK_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /retry thành công -> Trả về 200 OK kèm dữ liệu ApiResponse")
        void retryTaskSync_WhenSuccess_ReturnsOk() throws Exception {
            JiraTaskSyncResponse retryResponse = new JiraTaskSyncResponse(
                    TASK_ID,
                    SyncStatus.SYNCED,
                    "10001",
                    "CNPM-100",
                    "https://example.atlassian.net/browse/CNPM-100",
                    2,
                    false,
                    Instant.now(),
                    null,
                    "Retry đồng bộ Jira thành công"
            );

            when(jiraIntegrationService.retryTaskSync(eq(PROJECT_ID), eq(TASK_ID), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(retryResponse);

            mockMvc.perform(post("/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}/retry", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taskId").value(TASK_ID))
                    .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"))
                    .andExpect(jsonPath("$.data.jiraIssueKey").value("CNPM-100"));
        }

        @Test
        @DisplayName("POST /sync gặp lỗi từ Jira Service -> Ném JiraApiException tương ứng")
        void syncTaskToJira_WhenServiceReturnsError_ThrowsJiraApiException() {
            JiraTaskSyncResponse errorResponse = new JiraTaskSyncResponse(
                    TASK_ID,
                    SyncStatus.SYNC_FAILED,
                    null,
                    null,
                    null,
                    1,
                    true,
                    Instant.now(),
                    "SYNC_ALREADY_RUNNING",
                    "Sync process is currently running"
            );

            when(jiraIntegrationService.syncTask(eq(PROJECT_ID), eq(TASK_ID), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(errorResponse);

            assertThatThrownBy(() -> jiraIntegrationController.syncTaskToJira(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY))
                    .isInstanceOf(JiraApiException.class);
        }
    }
}
