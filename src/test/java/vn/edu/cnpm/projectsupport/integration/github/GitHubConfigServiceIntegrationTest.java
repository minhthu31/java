package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@ActiveProfiles("test")
class GitHubConfigServiceIntegrationTest {

    private static final long GROUP_ID = 9890L;
    private static final long PROJECT_ID = 9891L;
    private static final long NON_EXISTENT_PROJECT_ID = 99999L;

    @Autowired
    private GitHubConfigService gitHubConfigService;

    @Autowired
    private GitHubIntegrationConfigRepository configRepository;

    @Autowired
    private IntegrationSecretService secretService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                MERGE INTO student_groups (id, code, name)
                KEY(id)
                VALUES (?, ?, ?)
                """, GROUP_ID, "CNPM-91-TEST", "CNPM 91 Test Group");

        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 91 Project");
    }

    @Test
    @DisplayName("1. Tạo cấu hình mới thành công, trạng thái NOT_CHECKED và lastTestedAt, lastTestSucceeded là null")
    void createNewConfig_shouldSucceed() {
        GitHubConfigRequest request = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_secretToken123")
                .apiVersion("2026-03-10")
                .build();

        GitHubConfigResponse response = gitHubConfigService.saveConfig(PROJECT_ID, request);

        assertThat(response.isConfigured()).isTrue();
        assertThat(response.getRepositoryFullName()).isEqualTo("minhthu31/backend-repo");
        assertThat(response.getStatus()).isEqualTo("NOT_CHECKED");
        assertThat(response.getLastTestedAt()).isNull();
        assertThat(response.getLastTestSucceeded()).isNull();

        IntegrationConfig entity = configRepository.findGitHubConfigByProjectId(PROJECT_ID).orElseThrow();
        assertThat(secretService.decrypt(entity.getEncryptedSecret())).isEqualTo("ghp_secretToken123");
        assertThat(entity.getAccountIdentifier()).isEqualTo("minhthu31/backend-repo");
        assertThat(entity.getLastCheckedAt()).isNull();
        assertThat(entity.getLastErrorCode()).isNull();
    }

    @Test
    @DisplayName("2. Tạo mới lần đầu nhưng thiếu token hoặc truyền blank phải ném IllegalArgumentException")
    void createNewConfig_withoutToken_shouldThrowException() {
        GitHubConfigRequest requestNull = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken(null)
                .build();

        GitHubConfigRequest requestBlank = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("   ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> gitHubConfigService.saveConfig(PROJECT_ID, requestNull));
        assertThrows(IllegalArgumentException.class, () -> gitHubConfigService.saveConfig(PROJECT_ID, requestBlank));
    }

    @Test
    @DisplayName("3. Cập nhật khi accessToken=null phải giữ nguyên token cũ, reset NOT_CHECKED và xóa lastCheckedAt, lastErrorCode")
    void updateConfig_withNullAccessToken_shouldKeepOldTokenAndResetCheckData() {
        GitHubConfigRequest createRequest = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_originalSecretToken")
                .apiVersion("2026-03-10")
                .build();
        gitHubConfigService.saveConfig(PROJECT_ID, createRequest);

        GitHubConfigRequest updateRequest = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("updated-repo")
                .accessToken(null)
                .apiVersion("2026-03-10")
                .build();
        GitHubConfigResponse updateResponse = gitHubConfigService.saveConfig(PROJECT_ID, updateRequest);

        assertThat(updateResponse.getRepositoryFullName()).isEqualTo("minhthu31/updated-repo");
        assertThat(updateResponse.getStatus()).isEqualTo("NOT_CHECKED");
        assertThat(updateResponse.getLastTestedAt()).isNull();
        assertThat(updateResponse.getLastTestSucceeded()).isNull();

        IntegrationConfig entity = configRepository.findGitHubConfigByProjectId(PROJECT_ID).orElseThrow();
        assertThat(entity.getAccountIdentifier()).isEqualTo("minhthu31/updated-repo");
        assertThat(secretService.decrypt(entity.getEncryptedSecret())).isEqualTo("ghp_originalSecretToken");
        assertThat(entity.getLastCheckedAt()).isNull();
        assertThat(entity.getLastErrorCode()).isNull();
    }

    @Test
    @DisplayName("4. GET cấu hình chưa test thì lastTestedAt và lastTestSucceeded phải là null")
    void getConfig_whenNotTested_shouldReturnNullForTestFields() {
        GitHubConfigRequest request = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_secretToken123")
                .build();
        gitHubConfigService.saveConfig(PROJECT_ID, request);

        GitHubConfigResponse response = gitHubConfigService.getConfig(PROJECT_ID);
        assertThat(response.isConfigured()).isTrue();
        assertThat(response.getRepositoryFullName()).isEqualTo("minhthu31/backend-repo");
        assertThat(response.getStatus()).isEqualTo("NOT_CHECKED");
        assertThat(response.getLastTestedAt()).isNull();
        assertThat(response.getLastTestSucceeded()).isNull();
    }

    @Test
    @DisplayName("5. Test connection thành công cập nhật CONNECTED và lastCheckedAt vào DB")
    void testConnection_success() {
        GitHubConfigRequest request = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_secretToken123")
                .build();
        gitHubConfigService.saveConfig(PROJECT_ID, request);

        when(gitHubRestClient.testConnection(any(GitHubClientConfig.class))).thenReturn(
                new GitHubConnectionResult(
                        true,
                        1001L,
                        "minhthu31",
                        2001L,
                        "minhthu31/backend-repo",
                        "admin",
                        4999L,
                        Instant.now().plusSeconds(3600),
                        Instant.now())
        );

        GitHubConnectionTestResponse testResponse = gitHubConfigService.testConnection(PROJECT_ID);
        assertThat(testResponse.isConnected()).isTrue();
        assertThat(testResponse.getLogin()).isEqualTo("minhthu31");

        IntegrationConfig entity = configRepository.findGitHubConfigByProjectId(PROJECT_ID).orElseThrow();
        assertThat(String.valueOf(entity.getStatus())).isEqualTo("CONNECTED");
        assertThat(entity.getLastCheckedAt()).isNotNull();
        assertThat(entity.getLastErrorCode()).isNull();
    }

    @Test
    @DisplayName("6. Test connection thất bại: DB không bị rollback, lưu CONNECTION_FAILED và lastErrorCode khi exception thoát khỏi service")
    void testConnection_failure_shouldPersistErrorInDatabaseWithoutRollback() {
        GitHubConfigRequest request = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_invalidToken")
                .build();
        gitHubConfigService.saveConfig(PROJECT_ID, request);

        when(gitHubRestClient.testConnection(any(GitHubClientConfig.class)))
                .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", false, null, "Auth failed", null));

        assertThrows(GitHubApiException.class, () -> gitHubConfigService.testConnection(PROJECT_ID));

        // Đọc lại từ DB mà không bọc Transactional test để xác nhận DB đã commit
        IntegrationConfig entity = configRepository.findGitHubConfigByProjectId(PROJECT_ID).orElseThrow();
        assertThat(String.valueOf(entity.getStatus())).isEqualTo("CONNECTION_FAILED");
        assertThat(entity.getLastErrorCode()).isEqualTo("GITHUB_AUTHENTICATION_FAILED");
        assertThat(entity.getLastCheckedAt()).isNotNull();
        assertThat(secretService.decrypt(entity.getEncryptedSecret())).isEqualTo("ghp_invalidToken");

        GitHubConfigResponse configResponse = gitHubConfigService.getConfig(PROJECT_ID);
        assertThat(configResponse.getStatus()).isEqualTo("CONNECTION_FAILED");
        assertThat(configResponse.getLastTestedAt()).isNotNull();
        assertThat(configResponse.getLastTestSucceeded()).isFalse();
    }

    @Test
    @DisplayName("7. Project không tồn tại ném NoSuchElementException khi gọi API")
    void whenProjectDoesNotExist_shouldThrowException() {
        GitHubConfigRequest request = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("backend-repo")
                .accessToken("ghp_token")
                .build();

        assertThrows(NoSuchElementException.class, () -> gitHubConfigService.getConfig(NON_EXISTENT_PROJECT_ID));
        assertThrows(NoSuchElementException.class, () -> gitHubConfigService.saveConfig(NON_EXISTENT_PROJECT_ID, request));
        assertThrows(NoSuchElementException.class, () -> gitHubConfigService.testConnection(NON_EXISTENT_PROJECT_ID));
    }
}