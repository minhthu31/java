package vn.edu.cnpm.projectsupport.integration.jira;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;

@SpringBootTest
@AutoConfigureMockMvc
class JiraSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JiraIntegrationService jiraIntegrationService;

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String IDEMPOTENCY_KEY = "idemp-key-test-123456";

    @Nested
    @DisplayName("RBAC Security Tests")
    class RbacSecurityTests {

        @Test
        @DisplayName("Chưa đăng nhập -> Trả về 401 Unauthorized")
        void syncTask_WhenAnonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(post("/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}/sync", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Role không đủ quyền (USER/MEMBER) -> Trả về 403 Forbidden")
        @WithMockUser(username = "member_user", roles = {"USER"})
        void syncTask_WhenForbiddenRole_ShouldReturn403() throws Exception {
            mockMvc.perform(post("/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}/sync", PROJECT_ID, TASK_ID)
                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Role ADMIN hợp lệ -> Trả về 200 OK")
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        void syncTask_WhenAdminRole_ShouldReturn200() throws Exception {
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
    }
}
