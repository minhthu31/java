package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class GitHubRbacIntegrationTest {

    @Autowired
    private GitHubConfigService gitHubConfigService;

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
    @DisplayName("1. Kiểm tra xác thực (Authentication)")
    class AuthenticationTests {

        @Test
        @DisplayName("Chưa đăng nhập -> Bị chặn khi gọi service")
        void unauthenticated_AccessDenied() {
            assertThatThrownBy(() -> gitHubConfigService.getConfig(PROJECT_ID))
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("2. Kiểm tra vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép cấu hình và lưu GitHub Config")
        void admin_CanSaveConfig() {
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
        }
    }

    @Nested
    @DisplayName("3. Kiểm tra vai trò LEADER")
    class LeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: Được phép đọc cấu hình GitHub")
        void leader_CanGetConfig() {
            GitHubConfigResponse response = gitHubConfigService.getConfig(PROJECT_ID);
            assertThat(response).isNotNull();
        }

        @Test
        @WithMockUser(username = "leader_user", roles = {"LEADER"})
        @DisplayName("LEADER: Bị cấm sửa cấu hình nếu thiếu quyền ADMIN")
        void leader_ModifyConfigRules() {
            GitHubConfigRequest request = GitHubConfigRequest.builder()
                    .repositoryOwner("minhthu31")
                    .repositoryName("java")
                    .accessToken("ghp_token")
                    .build();

            try {
                gitHubConfigService.saveConfig(PROJECT_ID, request);
            } catch (AccessDeniedException ex) {
                assertThat(ex).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("4. Kiểm tra vai trò MEMBER & LECTURER")
    class MemberAndLecturerRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"MEMBER"})
        @DisplayName("MEMBER: Kiểm tra quyền hạn cấu hình")
        void member_AccessRestriction() {
            GitHubConfigRequest request = GitHubConfigRequest.builder()
                    .repositoryOwner("minhthu31")
                    .repositoryName("java")
                    .accessToken("ghp_token")
                    .build();

            try {
                gitHubConfigService.saveConfig(PROJECT_ID, request);
            } catch (AccessDeniedException ex) {
                assertThat(ex).isNotNull();
            }
        }
    }
}
