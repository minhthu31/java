package vn.edu.cnpm.projectsupport.integration.jira;

import static org.mockito.ArgumentMatchers.any;
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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(jiraIntegrationController).build();
    }

    @Nested
    @DisplayName("RBAC and Jira Sync API Endpoint Tests")
    class SyncEndpointTests {

        @Test
        @DisplayName("Gửi request sync thành công -> Trả về 200 OK kèm dữ liệu đồng bộ")
        void syncTask_WhenValidRequest_ReturnsOk() throws Exception {
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

            when(jiraIntegrationService.syncTask(eq(PROJECT_ID), eq(TASK_ID), eq("idemp-key-12345")))
                    .thenReturn(successResponse);

            mockMvc.perform(post("/api/projects/{projectId}/tasks/{taskId}/jira/sync", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", "idemp-key-12345")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                    .andExpect(jsonPath("$.jiraIssueKey").value("CNPM-100"));
        }

        @Test
        @DisplayName("Gửi request retry sync thành công -> Trả về 200 OK")
        void retryTaskSync_WhenValidRequest_ReturnsOk() throws Exception {
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

            when(jiraIntegrationService.retryTaskSync(eq(PROJECT_ID), eq(TASK_ID), eq("idemp-retry-12345")))
                    .thenReturn(retryResponse);

            mockMvc.perform(post("/api/projects/{projectId}/tasks/{taskId}/jira/sync/retry", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", "idemp-retry-12345")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                    .andExpect(jsonPath("$.jiraIssueKey").value("CNPM-100"));
        }
    }
}
