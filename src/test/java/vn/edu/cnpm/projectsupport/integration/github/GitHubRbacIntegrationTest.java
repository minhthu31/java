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
    private static final Long TASK_ID = 1L;

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
    @DisplayName("Chưa xác thực (Unauthenticated) -> 401 Unauthorized")
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
        @DisplayName("ADMIN: Được phép cấu hình PUT /config và đọc GET /config")
        void adminCanConfigureAndRead() throws Exception {
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
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.accessToken").doesNotExist());

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin_user", roles = {"ADMIN"})
        @DisplayName("ADMIN: Được phép xem danh sách GitHub activities")
        void adminCanViewActivities() throws Exception {
            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("2. Vai trò TEAM_LEADER: Đúng project vs Khác project")
    class TeamLeaderRoleTests {

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER đúng project: Được phép GET /config và GET /activities")
        void teamLeaderInProject_CanRead() throws Exception {
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

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "leader_other", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER khác project: Bị chặn (403) khi truy cập config và activities")
        void teamLeaderOtherProject_Forbidden() throws Exception {
            when(projectAuthorization.canViewTasks(OTHER_PROJECT_ID)).thenReturn(false);
            when(projectAuthorization.isCurrentUserLeader(OTHER_PROJECT_ID)).thenReturn(false);

            mockMvc.perform(get(OTHER_PROJECT_URL + "/config"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get(OTHER_PROJECT_URL + "/activities"))
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

        @Test
        @WithMockUser(username = "leader_user", roles = {"TEAM_LEADER"})
        @DisplayName("TEAM_LEADER: Bị cấm gọi POST /test-connection -> 403 Forbidden")
        void teamLeaderCannotTestConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("3. Vai trò LECTURER: Đúng project vs Khác project")
    class LecturerRoleTests {

        @Test
        @WithMockUser(username = "lecturer_user", roles = {"LECTURER"})
        @DisplayName("LECTURER đúng project: Được xem activities, cấm xem/sửa GET & PUT /config")
        void lecturerInProject_Permissions() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CONFIG_BODY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "lecturer_other", roles = {"LECTURER"})
        @DisplayName("LECTURER khác project -> 403 Forbidden")
        void lecturerOtherProject_Forbidden() throws Exception {
            when(projectAuthorization.canViewTasks(OTHER_PROJECT_ID)).thenReturn(false);

            mockMvc.perform(get(OTHER_PROJECT_URL + "/activities"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("4. Vai trò TEAM_MEMBER")
    class TeamMemberRoleTests {

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER thuộc project: Được xem activities của project/task được phân quyền")
        void teamMemberInProject_CanViewPermittedActivities() throws Exception {
            when(projectAuthorization.canViewTasks(MY_PROJECT_ID)).thenReturn(true);
            when(projectAuthorization.canViewTask(MY_PROJECT_ID, TASK_ID)).thenReturn(true);

            mockMvc.perform(get(BASE_URL + "/activities"))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/tasks/" + TASK_ID + "/activities"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "member_other", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER không thuộc project -> 403 Forbidden")
        void teamMemberOtherProject_Forbidden() throws Exception {
            when(projectAuthorization.canViewTasks(OTHER_PROJECT_ID)).thenReturn(false);

            mockMvc.perform(get(OTHER_PROJECT_URL + "/activities"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm xem cấu hình GET /config -> 403 Forbidden")
        void teamMemberCannotGetConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "member_user", roles = {"TEAM_MEMBER"})
        @DisplayName("TEAM_MEMBER: Bị cấm sửa cấu hình PUT /config -> 403 Forbidden")
        void teamMemberCannotModifyConfig() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CONFIG_BODY))
                    .andExpect(status().isForbidden());
        }
    }
