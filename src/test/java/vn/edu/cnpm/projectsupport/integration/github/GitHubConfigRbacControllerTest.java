package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubConfigRbacControllerTest {

    private static final long PROJECT_ID = 9891L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/github";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private GitHubConfigService gitHubConfigService;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    private GitHubConfigRequest validRequest;
    private GitHubConfigResponse configResponse;
    private GitHubConnectionTestResponse testResponse;

    @BeforeEach
    void setUp() {
        validRequest = GitHubConfigRequest.builder()
                .repositoryOwner("minhthu31")
                .repositoryName("java-backend")
                .accessToken("ghp_secretTokenExample123")
                .apiVersion("2026-03-10")
                .build();

        configResponse = GitHubConfigResponse.builder()
                .projectId(PROJECT_ID)
                .repositoryFullName("minhthu31/java-backend")
                .configured(true)
                .status("CONNECTED")
                .githubLogin("minhthu31")
                .lastTestedAt(Instant.parse("2026-09-02T01:00:00Z"))
                .lastTestSucceeded(true)
                .build();

        testResponse = GitHubConnectionTestResponse.builder()
                .projectId(PROJECT_ID)
                .connected(true)
                .githubUserId(1001L)
                .login("minhthu31")
                .githubRepositoryId(2001L)
                .repositoryFullName("minhthu31/java-backend")
                .permission("ADMIN")
                .rateLimitRemaining(4999L)
                .rateLimitResetAt(Instant.parse("2026-09-02T02:00:00Z"))
                .testedAt(Instant.parse("2026-09-02T01:00:00Z"))
                .build();

        when(gitHubConfigService.getConfig(PROJECT_ID)).thenReturn(configResponse);
        when(gitHubConfigService.saveConfig(eq(PROJECT_ID), any(GitHubConfigRequest.class))).thenReturn(configResponse);
        when(gitHubConfigService.testConnection(PROJECT_ID)).thenReturn(testResponse);
    }

    @Test
    @DisplayName("Admin được phép cấu hình, kiểm tra kết nối và xem cấu hình GitHub")
    void adminCanManageGitHubConfig() throws Exception {
        mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                .andExpect(jsonPath("$.data.repositoryFullName").value("minhthu31/java-backend"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist());

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(true));

        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true));

        verify(gitHubConfigService).saveConfig(eq(PROJECT_ID), any(GitHubConfigRequest.class));
        verify(gitHubConfigService).testConnection(PROJECT_ID);
        verify(gitHubConfigService).getConfig(PROJECT_ID);
    }

    @Test
    @DisplayName("Team Leader thuộc Project được phép xem cấu hình")
    void projectLeaderCanGetConfig() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true));

        verify(gitHubConfigService).getConfig(PROJECT_ID);
    }

    @Test
    @DisplayName("Team Leader không thuộc Project bị chặn 403")
    void leaderOfDifferentProjectIsForbidden() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID)
                        .with(user("other-leader").roles("TEAM_LEADER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gitHubConfigService);
    }

    @Test
    @DisplayName("Team Member không được phép cấu hình hoặc test kết nối")
    void memberCannotConfigureOrTestConnection() throws Exception {
        mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(gitHubConfigService);
    }

    @Test
    @DisplayName("Admin gửi dữ liệu thiếu trả về 400 Bad Request")
    void saveConfig_shouldReturnBadRequest_whenMissingRequiredFields() throws Exception {
        GitHubConfigRequest invalid = GitHubConfigRequest.builder()
                .repositoryOwner("")
                .repositoryName("")
                .build();

        mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}