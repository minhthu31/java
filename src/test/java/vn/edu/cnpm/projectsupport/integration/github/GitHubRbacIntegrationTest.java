package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;

@SpringBootTest
@ActiveProfiles("test")
class GitHubRbacIntegrationTest {

    @Autowired
    private GitHubConfigService gitHubConfigService;

    @Autowired
    private GitHubIntegrationConfigRepository configRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    private static final Long GROUP_ID = 100L;
    private static final Long PROJECT_ID = 100L;

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

    @Nested
    @DisplayName("1. Vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép cấu hình, lưu và cập nhật trạng thái NOT_CHECKED")
        void admin_CanSaveConfigAndResetStatus() {
            GitHubConfigRequest request = GitHubConfigRequest.builder()
                    .repositoryOwner("minhthu31")
                    .repositoryName("java")
                    .accessToken("ghp_secretTokenAdmin123")
                    .apiVersion("2026-03-10")
                    .build();

            GitHubConfigResponse response = gitHubConfigService.saveConfig(PROJECT_ID, request);
            assertThat(response).isNotNull();
            assertThat(response.getRepositoryFullName()).isEqualTo("minhthu31/java");
            assertThat(response.isConfigured()).isTrue();
            assertThat(response.getStatus()).isEqualTo("NOT_CHECKED");

            IntegrationConfig savedEntity = configRepository.findGitHubConfigByProjectId(PROJECT_ID).orElseThrow();
            assertThat(savedEntity.getAccountIdentifier()).isEqualTo("minhthu31/java");
        }
    }

    @Nested
    @DisplayName("2. Vai trò LEADER & LECTURER")
    class LeaderAndLecturerRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: Đọc cấu hình đã lưu thành công")
        void leader_CanGetConfig() {
            GitHubConfigRequest request = GitHubConfigRequest.builder()
                    .repositoryOwner("minhthu31")
                    .repositoryName("java")
                    .accessToken("ghp_token")
                    .build();
            gitHubConfigService.saveConfig(PROJECT_ID, request);

            GitHubConfigResponse response = gitHubConfigService.getConfig(PROJECT_ID);
            assertThat(response).isNotNull();
            assertThat(response.getRepositoryFullName()).isEqualTo("minhthu31/java");
        }

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER: Đọc cấu hình không bị lộ accessToken")
        void lecturer_CanGetConfigWithoutToken() {
            GitHubConfigRequest request = GitHubConfigRequest.builder()
                    .repositoryOwner("minhthu31")
                    .repositoryName("java")
                    .accessToken("ghp_token")
                    .build();
            gitHubConfigService.saveConfig(PROJECT_ID, request);

            GitHubConfigResponse response = gitHubConfigService.getConfig(PROJECT_ID);
            assertThat(response).isNotNull();
            assertThat(response.isConfigured()).isTrue();
        }
    }

    @Nested
    @DisplayName("3. Vai trò MEMBER")
    class MemberRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"MEMBER"})
        @DisplayName("MEMBER: Kiểm tra snapshot cấu hình rỗng khi chưa tạo")
        void member_GetConfigWhenNotConfigured() {
            GitHubConfigResponse response = gitHubConfigService.getConfig(PROJECT_ID);
            assertThat(response).isNotNull();
            assertThat(response.isConfigured()).isFalse();
            assertThat(response.getStatus()).isEqualTo("NOT_CONFIGURED");
        }
    }
}
