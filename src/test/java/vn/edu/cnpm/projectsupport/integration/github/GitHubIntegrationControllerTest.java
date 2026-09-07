package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private MockMvc mockMvc;

    @Mock
    private GitHubConfigService gitHubConfigService;

    @InjectMocks
    private GitHubIntegrationController gitHubIntegrationController;

    private static final Long PROJECT_ID = 1L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/github";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(gitHubIntegrationController).build();
    }

    @Nested
    @DisplayName("1. Cấu hình GitHub & Bảo mật Token")
    class ConfigTests {

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

            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                    .andExpect(jsonPath("$.data.repositoryFullName").value("minhthu31/java"))
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("PUT /config thiếu repositoryOwner -> 400 Bad Request")
        void saveConfig_InvalidPayload_Returns400() throws Exception {
            String invalidJson = """
                {
                    "repositoryName": "java"
                }
                """;

            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("2. Test Connection & Ánh xạ mã lỗi GitHub (401, 403, 404)")
    class ExternalErrorTests {

        @Test
        @DisplayName("Token hết hạn / không hợp lệ -> 401 GITHUB_AUTHENTICATION_FAILED")
        void testConnection_ExpiredToken_Returns401() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", false, null, "Auth failed", null));

            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("GITHUB_AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("Token thiếu quyền truy cập -> 403 GITHUB_AUTHORIZATION_FAILED")
        void testConnection_Forbidden_Returns403() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.FORBIDDEN, "GITHUB_AUTHORIZATION_FAILED", false, null, "Forbidden", null));

            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("GITHUB_AUTHORIZATION_FAILED"));
        }

        @Test
        @DisplayName("Repository không tồn tại -> 404 GITHUB_REPOSITORY_NOT_FOUND")
        void testConnection_NotFound_Returns404() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.NOT_FOUND, "GITHUB_REPOSITORY_NOT_FOUND", false, null, "Repo not found", null));

            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GITHUB_REPOSITORY_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("3. Đồng bộ (Sync) & Header Idempotency-Key")
    class SyncTests {

        @Test
        @DisplayName("POST /sync thiếu Header Idempotency-Key -> 400 Bad Request")
        void sync_MissingIdempotencyKeyHeader_Returns400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/sync", PROJECT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("API filter issueKey sai regex pattern -> 400 Bad Request")
        void getActivities_InvalidIssueKeyFormat_Returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/activities", PROJECT_ID)
                            .param("issueKey", "invalid_key"))
                    .andExpect(status().isBadRequest());
        }
    }
}
