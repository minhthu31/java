package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class GitHubIntegrationControllerTest {

    private MockMvc configMockMvc;

    @Mock
    private GitHubConfigService gitHubConfigService;

    @InjectMocks
    private GitHubConfigController gitHubConfigController;

    private static final Long PROJECT_ID = 1L;
    private static final String BASE_URL = "/api/v1/projects/" + PROJECT_ID + "/integrations/github";

    @BeforeEach
    void setUp() {
        configMockMvc = MockMvcBuilders.standaloneSetup(gitHubConfigController).build();
    }

    @Nested
    @DisplayName("1. Cấu hình GitHub & Bảo mật Token (CNPM-91)")
    class ConfigEndpointTests {

        @Test
        @DisplayName("PUT /config hợp lệ -> 200 OK và không bao giờ chứa accessToken")
        void saveConfig_Valid_ReturnsOkWithoutToken() throws Exception {
            GitHubConfigResponse response = GitHubConfigResponse.builder()
                    .projectId(PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("NOT_CHECKED")
                    .build();

            when(gitHubConfigService.saveConfig(eq(PROJECT_ID), any(GitHubConfigRequest.class)))
                    .thenReturn(response);

            String requestBody = """
                {
                    "repositoryOwner": "minhthu31",
                    "repositoryName": "java",
                    "accessToken": "ghp_mockSecretToken",
                    "apiVersion": "2026-03-10"
                }
                """;

            configMockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                    .andExpect(jsonPath("$.data.repositoryFullName").value("minhthu31/java"))
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("GET /config thành công -> Trả về 200 OK")
        void getConfig_ReturnsOk() throws Exception {
            GitHubConfigResponse response = GitHubConfigResponse.builder()
                    .projectId(PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("CONNECTED")
                    .build();

            when(gitHubConfigService.getConfig(eq(PROJECT_ID))).thenReturn(response);

            configMockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.status").value("CONNECTED"));
        }
    }

    @Nested
    @DisplayName("2. Test Connection & Ngoại vi GitHub (401, 403, 404)")
    class TestConnectionErrorTests {

        @Test
        @DisplayName("POST /test-connection thành công -> 200 OK")
        void testConnection_Success() throws Exception {
            GitHubConnectionTestResponse response = GitHubConnectionTestResponse.builder()
                    .projectId(PROJECT_ID)
                    .connected(true)
                    .login("minhthu31")
                    .repositoryFullName("minhthu31/java")
                    .testedAt(Instant.now())
                    .build();

            when(gitHubConfigService.testConnection(eq(PROJECT_ID))).thenReturn(response);

            configMockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.connected").value(true))
                    .andExpect(jsonPath("$.data.login").value("minhthu31"));
        }

        @Test
        @DisplayName("Token hết hạn / sai -> ném 401 GITHUB_AUTHENTICATION_FAILED")
        void testConnection_InvalidToken_Throws401() {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", false, null, "Auth failed", null));

            assertThatThrownBy(() -> gitHubConfigController.testConnection(PROJECT_ID))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(error -> {
                        GitHubApiException ex = (GitHubApiException) error;
                        assertThat(ex.getErrorCode()).isEqualTo("GITHUB_AUTHENTICATION_FAILED");
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    });
        }

        @Test
        @DisplayName("Token thiếu quyền truy cập -> ném 403 GITHUB_AUTHORIZATION_FAILED")
        void testConnection_Forbidden_Throws403() {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.FORBIDDEN, "GITHUB_AUTHORIZATION_FAILED", false, null, "Forbidden", null));

            assertThatThrownBy(() -> gitHubConfigController.testConnection(PROJECT_ID))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(error -> {
                        GitHubApiException ex = (GitHubApiException) error;
                        assertThat(ex.getErrorCode()).isEqualTo("GITHUB_AUTHORIZATION_FAILED");
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    });
        }

        @Test
        @DisplayName("Repository không tồn tại -> ném 404 GITHUB_REPOSITORY_NOT_FOUND")
        void testConnection_NotFound_Throws404() {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.NOT_FOUND, "GITHUB_REPOSITORY_NOT_FOUND", false, null, "Repo not found", null));

            assertThatThrownBy(() -> gitHubConfigController.testConnection(PROJECT_ID))
                    .isInstanceOf(GitHubApiException.class)
                    .satisfies(error -> {
                        GitHubApiException ex = (GitHubApiException) error;
                        assertThat(ex.getErrorCode()).isEqualTo("GITHUB_REPOSITORY_NOT_FOUND");
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}
