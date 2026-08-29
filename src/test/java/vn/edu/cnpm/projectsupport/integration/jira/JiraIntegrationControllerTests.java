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
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

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

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                INSERT INTO student_groups (id, code, name)
                VALUES (?, 'CNPM_CODE', 'CNPM Group')
                """, GROUP_ID);
        jdbcTemplate.update("""
                INSERT INTO projects (id, group_id, name)
                VALUES (?, ?, 'CNPM Project')
                """, PROJECT_ID, GROUP_ID);
    }

    @Test
    @DisplayName("Admin cấu hình Jira với projectKey='TEST' khác mã nhóm 'CNPM_CODE', lưu DB và GET config đọc lại đúng 'TEST'")
    void adminConfiguresJiraWithCustomProjectKeyDistinctFromGroupCode() throws Exception {
        String requestBody = """
                {
                  "siteUrl": "https://example.atlassian.net",
                  "projectKey": "TEST",
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
                .andExpect(jsonPath("$.data.projectKey").value("TEST"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.apiToken").doesNotExist())
                .andExpect(jsonPath("$.data.email").doesNotExist());

        // Kiểm tra lưu DB
        IntegrationConfig persisted = configRepository
                .findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA)
                .orElseThrow();
        assertThat(persisted.getBaseUrl()).isEqualTo("https://example.atlassian.net");
        assertThat(persisted.getAccountIdentifier()).isEqualTo("admin@example.com");
        assertThat(persisted.getEncryptedSecret()).startsWith("v1:");
        assertThat(secretService.decrypt(persisted.getEncryptedSecret())).isEqualTo("secret-token-12345");

        String savedProjectKey = jdbcTemplate.queryForObject(
                "SELECT jira_project_key FROM projects WHERE id = ?",
                String.class,
                PROJECT_ID);
        assertThat(savedProjectKey).isEqualTo("TEST");

        // GET config phải đọc lại đúng "TEST", không bị fallback về mã nhóm
        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectKey").value("TEST"))
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    @DisplayName("Test connection đọc đúng projectKey đã lưu từ bảng projects và truyền cho JiraClient")
    void adminTestsConnectionWithSavedProjectKey() throws Exception {
        String encrypted = secretService.encrypt("secret-token-12345");
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, encrypted);
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("admin@example.com");
        configRepository.save(config);

        jdbcTemplate.update("UPDATE projects SET jira_project_key = ? WHERE id = ?", "TEST", PROJECT_ID);

        when(jiraClient.testConnection(eq(PROJECT_ID), eq("TEST")))
                .thenReturn(new JiraConnectionResult(true, "10001", "TEST", "Project Support"));

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true))
                .andExpect(jsonPath("$.data.projectKey").value("TEST"))
                .andExpect(jsonPath("$.data.jiraProjectId").value("10001"))
                .andExpect(jsonPath("$.data.displayName").value("Project Support"));

        verify(jiraClient).testConnection(PROJECT_ID, "TEST");
    }

    @Test
    @DisplayName("Team Leader của dự án được xem cấu hình (200 OK)")
    void leaderOfProjectCanViewConfig() throws Exception {
        String encrypted = secretService.encrypt("secret-token-12345");
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.JIRA, encrypted);
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("admin@example.com");
        configRepository.save(config);

        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true));
    }

    @Test
    @DisplayName("Team Leader không thuộc dự án bị từ chối truy cập (403 Forbidden)")
    void leaderOfDifferentProjectIsForbidden() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("other_leader").roles("TEAM_LEADER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member không có quyền xem cấu hình (403 Forbidden)")
    void memberCannotViewConfig() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }
}