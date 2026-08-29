package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfigStatus;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JiraIntegrationControllerTests {

    private static final long GROUP_ID = 7800L;
    private static final long PROJECT_ID = 7801L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/jira";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationConfigRepository configRepository;

    @Autowired
    private IntegrationSecretService secretService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JiraClient jiraClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                INSERT INTO student_groups (id, code, name)
                VALUES (?, 'CNPM', 'CNPM 78 Test Group')
                """, GROUP_ID);
        jdbcTemplate.update("""
                INSERT INTO projects (id, group_id, name)
                VALUES (?, ?, 'CNPM 78 Project')
                """, PROJECT_ID, GROUP_ID);
    }

    @Test
    @DisplayName("Admin cấu hình Jira, DB lưu AES-GCM và GET config trả lại đúng projectKey")
    void adminConfiguresJiraSuccessfullyAndReadsProjectKey() throws Exception {
        String requestBody = """
                {
                  "siteUrl": "https://example.atlassian.net",
                  "projectKey": "CNPM",
                  "email": "admin@example.com",
                  "apiToken": "secret-token-12345",
                  "authType": "API_TOKEN"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.siteUrl").value("https://example.atlassian.net"))
                .andExpect(jsonPath("$.data.projectKey").value("CNPM"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.apiToken").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist());

        // Kiểm tra database
        IntegrationConfig persisted = configRepository
                .findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA)
                .orElseThrow();
        assertThat(persisted.getBaseUrl()).isEqualTo("https://example.atlassian.net");
        assertThat(persisted.getAccountIdentifier()).isEqualTo("admin@example.com");
        assertThat(persisted.getEncryptedSecret()).startsWith("v1:");
        assertThat(secretService.decrypt(persisted.getEncryptedSecret())).isEqualTo("secret-token-12345");

        // GET config phải đọc lại đúng projectKey từ DB
        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectKey").value("CNPM"))
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    @DisplayName("Test connection truyền đúng projectKey cho JiraClient và verify chính xác")
    void adminTestsConnectionWithExactProjectKeyVerification() throws Exception {
        String encrypted = secretService.encrypt("secret-token-12345");
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, encrypted);
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("admin@example.com");
        configRepository.save(config);

        when(jiraClient.testConnection(eq(PROJECT_ID), eq("CNPM")))
                .thenReturn(new JiraConnectionResult(true, "10001", "CNPM", "Project Support"));

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.projectKey").value("CNPM"))
                .andExpect(jsonPath("$.data.jiraProjectId").value("10001"))
                .andExpect(jsonPath("$.data.displayName").value("Project Support"));

        // Xác nhận chính xác jiraClient nhận đúng projectKey "CNPM"
        verify(jiraClient).testConnection(PROJECT_ID, "CNPM");
    }

    @Test
    @DisplayName("Lỗi xác thực Jira trả về HTTP 200 connected=false")
    void authenticationErrorReturnsConnectedFalse() throws Exception {
        String encrypted = secretService.encrypt("secret-token-12345");
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, encrypted);
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("admin@example.com");
        configRepository.save(config);

        when(jiraClient.testConnection(eq(PROJECT_ID), eq("CNPM")))
                .thenThrow(new JiraAuthenticationException("Invalid credentials"));

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(false))
                .andExpect(jsonPath("$.data.errorCode").value("JIRA_AUTHENTICATION_FAILED"));

        IntegrationConfig updated = configRepository.findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(IntegrationConfigStatus.CONNECTION_FAILED);
    }

    @Test
    @DisplayName("Lỗi mạng phía Jira trả về HTTP 502 theo OpenAPI contract")
    void networkErrorReturnsHttp502() throws Exception {
        String encrypted = secretService.encrypt("secret-token-12345");
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, encrypted);
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("admin@example.com");
        configRepository.save(config);

        when(jiraClient.testConnection(eq(PROJECT_ID), eq("CNPM")))
                .thenThrow(new JiraConnectionException("Jira Cloud unreachable"));

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("JIRA_CONNECTION_FAILED"));
    }

    @Test
    @DisplayName("Member không có quyền xem cấu hình (403 Forbidden)")
    void memberCannotViewConfig() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }
}