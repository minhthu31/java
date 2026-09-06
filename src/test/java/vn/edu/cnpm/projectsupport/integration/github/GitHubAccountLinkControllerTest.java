package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitHubAccountLinkControllerTest {

    private static final long PROJECT_ID = 101L;
    private static final long USER_ID = 202L;

    @Autowired private MockMvc mockMvc;
    @MockitoBean private GitHubAccountLinkService service;
    @MockitoBean(name = "projectAuthorization") private ProjectAuthorizationService projectAuthorization;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);
    }

    @Test
    void adminCanLink() throws Exception {
        GitHubAccountLinkResponse response = new GitHubAccountLinkResponse(
                USER_ID, "9001", "octocat", "avatar", "profile", Instant.parse("2026-09-06T10:00:00Z"));
        when(service.linkAccount(eq(PROJECT_ID), eq(USER_ID), any(GitHubAccountLinkRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/{projectId}/integrations/github/members/{userId}/account-link", PROJECT_ID, USER_ID)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GitHubAccountLinkRequest("9001", "octocat"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalAccountId").value("9001"));

        verify(service).linkAccount(eq(PROJECT_ID), eq(USER_ID), any(GitHubAccountLinkRequest.class));
    }

    @Test
    void projectLeaderCanLink() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);
        when(service.linkAccount(eq(PROJECT_ID), eq(USER_ID), any(GitHubAccountLinkRequest.class)))
                .thenReturn(new GitHubAccountLinkResponse(USER_ID, "9001", "octocat", null, null, Instant.now()));

        mockMvc.perform(put("/api/v1/projects/{projectId}/integrations/github/members/{userId}/account-link", PROJECT_ID, USER_ID)
                        .with(user("leader").roles("TEAM_LEADER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GitHubAccountLinkRequest("9001", "octocat"))))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotLink() throws Exception {
        mockMvc.perform(put("/api/v1/projects/{projectId}/integrations/github/members/{userId}/account-link", PROJECT_ID, USER_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GitHubAccountLinkRequest("9001", "octocat"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void leaderOfAnotherProjectCannotLink() throws Exception {
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(put("/api/v1/projects/{projectId}/integrations/github/members/{userId}/account-link", PROJECT_ID, USER_ID)
                        .with(user("other-leader").roles("TEAM_LEADER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GitHubAccountLinkRequest("9001", "octocat"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
