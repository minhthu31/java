package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class GitHubIntegrationControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private GitHubConfigService gitHubConfigService;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    private static final Long PROJECT_ID = 1L;
    private static final String BASE_URL = "/api/v1/projects/" + PROJECT_ID + "/integrations/github";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Nested
    @DisplayName("1. Test Cấu hình và An toàn Token (Write-only)")
    class ConfigEndpointTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
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
                    "accessToken": "ghp_mockSecretTokenWriteOnly12345",
                    "apiVersion": "2026-03-10"
                }
                """;

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                    .andExpect(jsonPath("$.data.repositoryFullName").value("minhthu31/java"))
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("GET /config thành công -> 200 OK và không chứa token")
        void getConfig_ReturnsOkWithoutToken() throws Exception {
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
    }

    @Nested
    @DisplayName("2. Test External Errors qua MockMvc (401, 403, 404, 429)")
    class ExternalErrorTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
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

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.connected").value(true))
                    .andExpect(jsonPath("$.data.login").value("minhthu31"));
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("GitHub trả 401 -> API trả HTTP 401 và error code GITHUB_AUTHENTICATION_FAILED")
        void testConnection_TokenExpired_Returns401() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", false, null, "Bad credentials", null));

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("GITHUB_AUTHENTICATION_FAILED"));
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("GitHub trả 403 -> API trả HTTP 403 và error code GITHUB_AUTHORIZATION_FAILED")
        void testConnection_ForbiddenRepo_Returns403() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.FORBIDDEN, "GITHUB_AUTHORIZATION_FAILED", false, null, "Forbidden", null));

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("GITHUB_AUTHORIZATION_FAILED"));
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("GitHub trả 404 -> API trả HTTP 404 và error code GITHUB_REPOSITORY_NOT_FOUND")
        void testConnection_NotFound_Returns404() throws Exception {
            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(new GitHubApiException(HttpStatus.NOT_FOUND, "GITHUB_REPOSITORY_NOT_FOUND", false, null, "Repo not found", null));

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GITHUB_REPOSITORY_NOT_FOUND"));
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("GitHub trả 429 -> API trả HTTP 429, GITHUB_RATE_LIMITED kèm Retry-After")
        void testConnection_RateLimited_Returns429() throws Exception {
            GitHubApiException rateLimitEx = new GitHubApiException(HttpStatus.TOO_MANY_REQUESTS, "GITHUB_RATE_LIMITED", true, 60L, "Rate limit exceeded", null);

            when(gitHubConfigService.testConnection(eq(PROJECT_ID)))
                    .thenThrow(rateLimitEx);

            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("GITHUB_RATE_LIMITED"))
                    .andExpect(header().string("Retry-After", "60"));
        }
    }
}
