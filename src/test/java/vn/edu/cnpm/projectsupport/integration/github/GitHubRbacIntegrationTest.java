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
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@ActiveProfiles("test")
class GitHubRbacIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private GitHubConfigService gitHubConfigService;

    @MockitoBean
    private GitHubActivityService gitHubActivityService;

    @MockitoBean
    private GitHubRestClient gitHubRestClient;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    private static final Long MY_PROJECT_ID = 100L;
    private static final Long OTHER_PROJECT_ID = 999L;
    private static final Long MY_USER_ID = 50L;
    private static final Long OTHER_USER_ID = 99L;

    private static final String BASE_URL = "/api/v1/projects/" + MY_PROJECT_ID + "/integrations/github";
    private static final String OTHER_PROJECT_URL = "/api/v1/projects/" + OTHER_PROJECT_ID + "/integrations/github";

    private static final String VALID_CONFIG_BODY = """
        {
            "repositoryOwner": "minhthu31",
            "repositoryName": "java",
            "accessToken": "ghp_secretTokenExample123",
            "apiVersion": "2026-03-10"
        }
        """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Chưa xác thực (Không gửi Token) -> 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_URL + "/activities"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("1. Vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép cấu hình, test connection và xem activities")
        void adminFullPermissions() throws Exception {
            GitHubConfigResponse configResponse = GitHubConfigResponse.builder()
                    .projectId(MY_PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("NOT_CHECKED")
                    .build();

            when(gitHubConfigService.saveConfig(eq(MY_PROJECT_ID), any())).thenReturn(configResponse);
            when(gitHubConfigService.getConfig(eq(MY_PROJECT_ID))).thenReturn(configResponse);
            when(gitHubActivityService.listActivities(eq(MY_PROJECT_ID), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true));

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CONFIG_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2. Vai trò TEAM_LEADER: Đúng project vs Khác project")
    class TeamLeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER thuộc đúng project: Được phép GET /config")
        void teamLeaderInProject_CanGetConfig() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.isCurrentUserLeader(MY_PROJECT_ID)).thenReturn(true);

            GitHubConfigResponse configResponse = GitHubConfigResponse.builder()
                    .projectId(MY_PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("CONNECTED")
                    .build();

            when(gitHubConfigService.getConfig(eq(MY_PROJECT_ID))).thenReturn(configResponse);

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());
        }

        @Test
        @WithMockUser(username = "leader_other", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER project khác gọi GET /config -> 403 Forbidden")
        void teamLeaderOtherProject_Forbidden() throws Exception {
            when(projectAuthorization.canViewTasks(OTHER_PROJECT_ID)).thenReturn(false);
            when(projectAuthorization.isCurrentUserLeader(OTHER_PROJECT_ID)).thenReturn(false);

            mockMvc.perform(get(OTHER_PROJECT_URL + "/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER: Bị cấm sửa cấu hình (PUT /config) -> 403 Forbidden")
        void teamLeaderCannotModifyConfig() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CONFIG_BODY))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("3. Vai trò LECTURER")
    class LecturerRoleTests {

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER thuộc project: Được xem activities nhưng bị cấm GET /config")
        void lecturerPermissions() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(gitHubActivityService.listActivities(eq(MY_PROJECT_ID), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true));

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Vai trò TEAM_MEMBER: Xem activities của chính mình vs người khác")
    class TeamMemberRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Được xem activities khi truyền actorUserId của chính mình")
        void teamMemberCanViewOwnActivities() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.currentUserId()).thenReturn(MY_USER_ID);

            when(gitHubActivityService.listActivities(eq(MY_PROJECT_ID), eq(MY_USER_ID), any(), any(), any(), any(), any()))
                    .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true));

            mockMvc.perform(get(BASE_URL + "/activities")
                            .param("actorUserId", String.valueOf(MY_USER_ID)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm khi xem activities của người khác hoặc bỏ actorUserId")
        void teamMemberCannotViewOthersOrAllActivities() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.currentUserId()).thenReturn(MY_USER_ID);

            when(gitHubActivityService.listActivities(eq(MY_PROJECT_ID), eq(OTHER_USER_ID), any(), any(), any(), any(), any()))
                    .thenThrow(new AccessDeniedException("Thành viên chỉ được xem hoạt động của chính mình"));

            when(gitHubActivityService.listActivities(eq(MY_PROJECT_ID), eq(null), any(), any(), any(), any(), any()))
                    .thenThrow(new AccessDeniedException("Thành viên phải chỉ định actorUserId của chính mình"));

            mockMvc.perform(get(BASE_URL + "/activities")
                            .param("actorUserId", String.valueOf(OTHER_USER_ID)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm đọc cấu hình (GET /config) -> 403 Forbidden")
        void teamMemberCannotGetConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }
    }
}
