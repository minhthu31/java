package vn.edu.cnpm.projectsupport.integration.jira;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JiraSyncRbacControllerTest {

    private static final long PROJECT_ID = 9101L;
    private static final long TASK_ID = 9102L;
    private static final String IDEMPOTENCY_KEY = "cnpm-85-sync-9102";
    private static final String BASE_URL =
            "/api/v1/projects/{projectId}/integrations/jira/tasks/{taskId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JiraIntegrationService jiraIntegrationService;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    @BeforeEach
    void setUp() {
        JiraTaskSyncResponse success = new JiraTaskSyncResponse(
                TASK_ID,
                SyncStatus.SYNCED,
                "109102",
                "CNPM-9102",
                "https://example.atlassian.net/browse/CNPM-9102",
                1,
                false,
                Instant.parse("2026-09-02T01:00:00Z"),
                null,
                "Đồng bộ Jira thành công");

        when(jiraIntegrationService.syncTask(anyLong(), anyLong(), anyString()))
                .thenReturn(success);
        when(jiraIntegrationService.retryTaskSync(anyLong(), anyLong(), anyString()))
                .thenReturn(success);
    }

    @Test
    @DisplayName("Admin được phép sync và retry Task lên Jira")
    void adminCanSyncAndRetryTask() throws Exception {
        performAs("admin", "ADMIN", "/sync")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"));

        performAs("admin", "ADMIN", "/retry")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jiraIssueKey").value("CNPM-9102"));

        verify(jiraIntegrationService)
                .syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);
        verify(jiraIntegrationService)
                .retryTaskSync(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("Team Leader thuộc Project được phép sync và retry")
    void projectLeaderCanSyncAndRetryTask() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);

        performAs("leader", "TEAM_LEADER", "/sync")
                .andExpect(status().isOk());
        performAs("leader", "TEAM_LEADER", "/retry")
                .andExpect(status().isOk());

        verify(jiraIntegrationService)
                .syncTask(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);
        verify(jiraIntegrationService)
                .retryTaskSync(PROJECT_ID, TASK_ID, IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("Team Leader không thuộc Project bị chặn trước khi gọi service")
    void leaderOfDifferentProjectIsForbidden() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);

        performAs("other-leader", "TEAM_LEADER", "/sync")
                .andExpect(status().isForbidden());
        performAs("other-leader", "TEAM_LEADER", "/retry")
                .andExpect(status().isForbidden());

        verifyNoInteractions(jiraIntegrationService);
    }

    @Test
    @DisplayName("Team Member không được phép sync hoặc retry Jira")
    void memberCannotSyncOrRetryTask() throws Exception {
        performAs("member", "TEAM_MEMBER", "/sync")
                .andExpect(status().isForbidden());
        performAs("member", "TEAM_MEMBER", "/retry")
                .andExpect(status().isForbidden());

        verifyNoInteractions(jiraIntegrationService);
    }

    @Test
    @DisplayName("Lecturer không được phép sync hoặc retry Jira")
    void lecturerCannotSyncOrRetryTask() throws Exception {
        performAs("lecturer", "LECTURER", "/sync")
                .andExpect(status().isForbidden());
        performAs("lecturer", "LECTURER", "/retry")
                .andExpect(status().isForbidden());

        verifyNoInteractions(jiraIntegrationService);
    }

    private org.springframework.test.web.servlet.ResultActions performAs(
            String username,
            String role,
            String operation) throws Exception {
        return mockMvc.perform(post(BASE_URL + operation, PROJECT_ID, TASK_ID)
                .with(user(username).roles(role))
                .with(csrf())
                .header("Idempotency-Key", IDEMPOTENCY_KEY));
    }
}
