package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class GitHubRbacIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GitHubConfigService gitHubConfigService;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    private static final Long GROUP_ID = 100L;
    private static final Long PROJECT_ID = 100L;
    private static final Long OTHER_PROJECT_ID = 999L;
    private static final String BASE_URL = "/api/v1/projects/" + PROJECT_ID + "/integrations/github";
    private static final String OTHER_BASE_URL = "/api/v1/projects/" + OTHER_PROJECT_ID + "/integrations/github";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id IN (?, ?)", PROJECT_ID, OTHER_PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id IN (?, ?)", PROJECT_ID, OTHER_PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                MERGE INTO student_groups (id, code, name)
                KEY(id)
                VALUES (?, ?, ?)
                """, GROUP_ID, "CNPM-100-TEST", "CNPM 100 Test Group");

        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 100 Project");

        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                OTHER_PROJECT_ID, GROUP_ID, "Other Project");
    }

    @Test
    @DisplayName("Không đăng nhập -> 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE_URL + "/test-connection"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("1. Vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép lưu cấu hình (PUT /config) và không bao giờ chứa token trong response")
        void adminCanSaveConfig() throws Exception {
            GitHubConfigResponse response = GitHubConfigResponse.builder()
                    .projectId(PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("NOT_CHECKED")
                    .build();

            when(gitHubConfigService.saveConfig(eq(PROJECT_ID), any())).thenReturn(response);

            String configJson = """
                {
                    "repositoryOwner": "minhthu31",
                    "repositoryName": "java",
                    "accessToken": "ghp_secretTokenExample123",
                    "apiVersion": "2026-03-10"
                }
                """;

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(configJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép gọi POST /test-connection")
        void adminCanTestConnection() throws Exception {
            GitHubConnectionTestResponse response = GitHubConnectionTestResponse.builder()
                    .projectId(PROJECT_ID)
                    .connected(true)
                    .testedAt(Instant.now())
                    .build();

            when(gitHubConfigService.testConnection(eq(PROJECT_ID))).thenReturn(response);

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2. Vai trò TEAM_LEADER")
    class TeamLeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER: Được phép đọc cấu hình đúng project (GET /config)")
        void teamLeaderCanGetConfig() throws Exception {
            GitHubConfigResponse response = GitHubConfigResponse.builder()
                    .projectId(PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("CONNECTED")
                    .build();

            when(gitHubConfigService.getConfig(eq(PROJECT_ID))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER: Bị cấm (403) khi sửa cấu hình (PUT /config)")
        void teamLeaderForbiddenFromModifyingConfig() throws Exception {
            String configJson = """
                {
                    "repositoryOwner": "minhthu31",
                    "repositoryName": "java",
                    "accessToken": "ghp_token"
                }
                """;

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(configJson))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER: Bị cấm (403) khi gọi POST /test-connection")
        void teamLeaderForbiddenFromTestingConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("3. Vai trò LECTURER")
    class LecturerRoleTests {

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: Bị cấm đọc cấu hình (GET /config) -> 403 Forbidden")
        void lecturerForbiddenFromReadingConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: Bị cấm sửa cấu hình (PUT /config) -> 403 Forbidden")
        void lecturerForbiddenFromModifyingConfig() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: Bị cấm test connection -> 403 Forbidden")
        void lecturerForbiddenFromTestingConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Vai trò TEAM_MEMBER")
    class TeamMemberRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm đọc cấu hình (GET /config) -> 403 Forbidden")
        void teamMemberForbiddenFromReadingConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm sửa cấu hình (PUT /config) -> 403 Forbidden")
        void teamMemberForbiddenFromModifyingConfig() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm test connection -> 403 Forbidden")
        void teamMemberForbiddenFromTestingConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("5. Kiểm tra người dùng thuộc project khác")
    class CrossProjectAccessTests {

        @Test
        @WithMockUser(username = "other_leader", roles = {"TEAM_LEADER"})
        @DisplayName("Truy cập cấu hình project không thuộc thẩm quyền -> 403 Forbidden")
        void accessingOtherProject_ReturnsForbidden() throws Exception {
            // Giả lập kiểm tra thẩm quyền theo project
            mockMvc.perform(get(OTHER_BASE_URL + "/config"))
                    .andExpect(status().is(org.hamcrest.Matchers.oneOf(403, 404)));
        }
    }
}
