package vn.edu.cnpm.projectsupport.integration.github;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    private static final Long GROUP_ID = 100L;
    private static final Long PROJECT_ID = 100L;
    private static final String BASE_URL = "/api/v1/projects/" + PROJECT_ID + "/integrations/github";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                MERGE INTO student_groups (id, code, name)
                KEY(id)
                VALUES (?, ?, ?)
                """, GROUP_ID, "CNPM-100-TEST", "CNPM 100 Test Group");

        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 100 Project");
    }

    @Test
    @DisplayName("Chưa xác thực (No Auth) -> 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_URL + "/repositories"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("1. Vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Có quyền PUT /config")
        void adminCanConfigureGitHub() throws Exception {
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
        @DisplayName("ADMIN: Có quyền POST /test-connection")
        void adminCanTestConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().is(org.hamcrest.Matchers.oneOf(200, 400, 404)));
        }
    }

    @Nested
    @DisplayName("2. Vai trò LEADER")
    class LeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: Được phép GET /config")
        void leaderCanGetConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: CẤM (403) sửa PUT /config")
        void leaderForbiddenFromModifyingConfig() throws Exception {
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
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: CẤM (403) POST /test-connection")
        void leaderForbiddenFromTestingConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("3. Vai trò LECTURER")
    class LecturerRoleTests {

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: Được phép xem danh sách repositories snapshot")
        void lecturerCanReadRepositories() throws Exception {
            mockMvc.perform(get(BASE_URL + "/repositories"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: CẤM (403) kích hoạt POST /sync")
        void lecturerForbiddenFromTriggeringSync() throws Exception {
            mockMvc.perform(post(BASE_URL + "/sync")
                            .header("Idempotency-Key", "lecturer-sync-key"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Vai trò MEMBER")
    class MemberRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"MEMBER"})
        @DisplayName("MEMBER: Được phép đọc activities của task")
        void memberCanReadTaskActivities() throws Exception {
            mockMvc.perform(get(BASE_URL + "/tasks/1/activities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"MEMBER"})
        @DisplayName("MEMBER: CẤM (403) khi GET /config")
        void memberForbiddenFromViewingConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"MEMBER"})
        @DisplayName("MEMBER: CẤM (403) khi POST /sync")
        void memberForbiddenFromTriggeringSync() throws Exception {
            mockMvc.perform(post(BASE_URL + "/sync")
                            .header("Idempotency-Key", "member-sync-key"))
                    .andExpect(status().isForbidden());
        }
    }
}
