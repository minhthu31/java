package vn.edu.cnpm.projectsupport.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Admin xem được báo cáo nhưng không thao tác tài nguyên học thuật")
    void adminCanViewReportsButCannotManageAcademicResources() throws Exception {
        JsonNode adminLogin = login("admin.test", "password");
        String adminToken = adminLogin.path("accessToken").asText();

        JsonNode leaderLogin = login("leader.test", "password");
        long projectId = leaderLogin.path("projectId").asLong();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/progress", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lecturer chỉ đọc Requirement, Task và báo cáo của nhóm được phân công")
    void lecturerHasReadOnlyAccessToAssignedProject() throws Exception {
        JsonNode login = login("lecturer.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", 999999999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Team Leader chỉ quản lý Requirement và Task của project mình")
    void leaderCanManageOnlyOwnedProject() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", 999999999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Team Member chỉ xem Task được giao và báo cáo cá nhân")
    void memberIsLimitedToAssignedTasksAndOwnContribution() throws Exception {
        JsonNode login = login("member.test", "password");
        long projectId = login.path("projectId").asLong();
        long memberId = login.path("id").asLong();
        String token = login.path("accessToken").asText();
        Long assignedTaskId = jdbcTemplate.queryForObject("""
                SELECT t.id
                  FROM tasks t
                 WHERE t.project_id = ?
                   AND t.assignee_user_id = ?
                 ORDER BY t.id
                 LIMIT 1
                """, Long.class, projectId, memberId);

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, assignedTaskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(
                        "/api/v1/projects/{projectId}/integrations/github/repositories/{repositoryId}/commits",
                        projectId, 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/{projectId}/integrations/github/check-runs", projectId)
                        .param("repositoryId", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", 999999999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("memberId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API được bảo vệ từ chối người dùng chưa đăng nhập")
    void unauthenticatedUserIsRejected() throws Exception {
        JsonNode leader = login("leader.test", "password");

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary",
                        leader.path("projectId").asLong()))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }
}
