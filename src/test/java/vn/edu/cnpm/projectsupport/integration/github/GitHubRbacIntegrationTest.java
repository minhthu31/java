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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    private static final Long MEMBER_A_ID = 11L;
    private static final Long MEMBER_B_ID = 22L;
    private static final Long ASSIGNED_TASK_ID = 101L;
    private static final Long UNASSIGNED_TASK_ID = 202L;

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

        when(gitHubActivityService.listActivities(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true));

        when(gitHubActivityService.listTaskActivities(any(), any(), any()))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true));
    }

    @Test
    @DisplayName("Chưa xác thực -> 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/config"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_URL + "/activities"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(BASE_URL + "/test-connection"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("1. Vai trò ADMIN")
    class AdminRoleTests {

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Toàn quyền cấu hình và xem activities không giới hạn")
        void adminFullPermissions() throws Exception {
            GitHubConfigResponse configResponse = GitHubConfigResponse.builder()
                    .projectId(MY_PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("NOT_CHECKED")
                    .build();

            when(gitHubConfigService.saveConfig(eq(MY_PROJECT_ID), any())).thenReturn(configResponse);
            when(gitHubConfigService.getConfig(eq(MY_PROJECT_ID))).thenReturn(configResponse);

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CONFIG_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2. Vai trò TEAM_LEADER")
    class TeamLeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER đúng project: Xem được cấu hình và toàn bộ activities nhóm")
        void teamLeaderInProject_Success() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.canManageTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.isCurrentUserLeader(MY_PROJECT_ID)).thenReturn(true);

            GitHubConfigResponse configResponse = GitHubConfigResponse.builder()
                    .projectId(MY_PROJECT_ID)
                    .repositoryFullName("minhthu31/java")
                    .configured(true)
                    .status("CONNECTED")
                    .build();
            when(gitHubConfigService.getConfig(eq(MY_PROJECT_ID))).thenReturn(configResponse);

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "leader_other", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER khác project -> 403 Forbidden")
        void teamLeaderOtherProject_Forbidden() throws Exception {
            when(projectAuthorization.canViewTasks(OTHER_PROJECT_ID)).thenReturn(false);
            when(projectAuthorization.canManageTasks(OTHER_PROJECT_ID)).thenReturn(false);
            when(projectAuthorization.isCurrentUserLeader(OTHER_PROJECT_ID)).thenReturn(false);

            mockMvc.perform(get(OTHER_PROJECT_URL + "/config"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(OTHER_PROJECT_URL + "/activities"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("3. Vai trò LECTURER")
    class LecturerRoleTests {

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER đúng project: Được xem activities, cấm GET /config")
        void lecturerInProject_Permissions() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.canManageTasks(MY_PROJECT_ID)).thenReturn(true);

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Vai trò TEAM_MEMBER: Kiểm soát chặt chẽ phạm vi activities cá nhân")
    class TeamMemberScopeTests {

        @BeforeEach
        void setupMemberA() {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.canManageTasks(MY_PROJECT_ID)).thenReturn(false); // Member không có quyền quản lý task
            when(projectAuthorization.isCurrentUser(MEMBER_A_ID)).thenReturn(true);
            when(projectAuthorization.isCurrentUser(MEMBER_B_ID)).thenReturn(false);
            when(projectAuthorization.isCurrentUser(null)).thenReturn(false);

            when(projectAuthorization.canViewTask(MY_PROJECT_ID, ASSIGNED_TASK_ID)).thenReturn(true);
            when(projectAuthorization.canViewTask(MY_PROJECT_ID, UNASSIGNED_TASK_ID)).thenReturn(false);
        }

        @Test
        @WithMockUser(username = "member_a", roles = {"TEAM_MEMBER"})
        @DisplayName("Member A xem commit/hoạt động của chính mình -> 200 OK")
        void memberA_ViewsOwnActivities_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/activities")
                            .param("actorUserId", String.valueOf(MEMBER_A_ID)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member_a", roles = {"TEAM_MEMBER"})
        @DisplayName("Member A cố truyền actorUserId của thành viên khác -> 403 Forbidden")
        void memberA_ViewsMemberBActivities_Forbidden() throws Exception {
            mockMvc.perform(get(BASE_URL + "/activities")
                            .param("actorUserId", String.valueOf(MEMBER_B_ID)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_a", roles = {"TEAM_MEMBER"})
        @DisplayName("Member A bỏ actorUserId để cố xem hoạt động toàn nhóm -> 403 Forbidden")
        void memberA_OmitsActorUserIdToViewAll_Forbidden() throws Exception {
            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_a", roles = {"TEAM_MEMBER"})
        @DisplayName("Member A xem hoạt động task được giao -> 200 OK")
        void memberA_ViewsAssignedTask_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/tasks/" + ASSIGNED_TASK_ID + "/activities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member_a", roles = {"TEAM_MEMBER"})
        @DisplayName("Member A xem hoạt động task chưa được giao -> 403 Forbidden")
        void memberA_ViewsUnassignedTask_Forbidden() throws Exception {
            mockMvc.perform(get(BASE_URL + "/tasks/" + UNASSIGNED_TASK_ID + "/activities"))
                    .andExpect(status().isForbidden());
        }
    }
}
