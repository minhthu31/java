package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                INSERT INTO student_groups (id, code, name)
                VALUES (?, 'CNPM-78-TEST', 'CNPM 78 Test Group')
                """, GROUP_ID);
        jdbcTemplate.update("""
                INSERT INTO projects (id, group_id, name)
                VALUES (?, ?, 'CNPM 78 Project')
                """, PROJECT_ID, GROUP_ID);
    }

    @Test
    @DisplayName("Admin cấu hình Jira qua endpoint thật, dữ liệu được mã hóa và lưu vào database thật")
    void adminSavesConfigAndPersistsInRealDatabase() throws Exception {
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
                .andExpect(jsonPath("$.timestamp").exists());


        IntegrationConfig persisted = configRepository
                .findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA)
                .orElseThrow(() -> new AssertionError("Dữ liệu cấu hình chưa được lưu xuống DB!"));

        assertThat(persisted.getBaseUrl()).isEqualTo("https://example.atlassian.net");
        assertThat(persisted.getEncryptedSecret()).startsWith("enc:v1:");
        assertThat(persisted.getEncryptedSecret()).doesNotContain("secret-token-12345");


        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.projectKey").value("CNPM"));
    }

    @Test
    @DisplayName("Member không có quyền xem cấu hình (403 Forbidden)")
    void memberCannotViewConfig() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }
}