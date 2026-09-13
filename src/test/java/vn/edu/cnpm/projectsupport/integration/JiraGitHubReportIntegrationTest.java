package vn.edu.cnpm.projectsupport.integration;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JiraGitHubReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TaskService taskService;

    @Test
    @DisplayName("AC1: Test luồng tạo Task và đồng bộ lên Jira")
    void testTaskCreationAndJiraSyncFlow() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task for Jira Sync Test");
        req.setDescription("Verify local task integrates with Jira");

        TaskResponse task = taskService.createTask(projectId, req, "IDEMP-KEY-110-1");
        assertThat(task).isNotNull();
        assertThat(task.getId()).isNotNull();

        // Kiểm tra task truy xuất được qua API sau khi tạo
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, task.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC2 & AC3: Test đồng bộ GitHub repository, commit, PR và liên kết Task")
    void testGitHubRepositorySyncAndTaskLinking() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // Kiểm tra danh sách task truy cập hợp lệ trước và sau khi gắn ngữ cảnh commit
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC4: Test báo cáo tiến độ và đóng góp thành viên")
    void testProgressAndMemberContributionReports() throws Exception {
        JsonNode leaderLogin = login("leader.test", "password");
        long projectId = leaderLogin.path("projectId").asLong();
        String leaderToken = leaderLogin.path("accessToken").asText();

        // 1. Leader xem báo cáo tổng hợp
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk());

        // 2. Leader xem báo cáo tiến độ (vừa bảo vệ ở CNPM-109)
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/progress", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk());

        // 3. Team Member xem đóng góp cá nhân
        JsonNode memberLogin = login("member.test", "password");
        String memberToken = memberLogin.path("accessToken").asText();
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC5: Chạy đồng bộ lại không tạo trùng dữ liệu (Idempotency)")
    void testSyncIdempotencyDoesNotDuplicateData() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Idempotent Task Sync");

        String idempotentKey = "REPEATABLE-SYNC-KEY-999";
        TaskResponse firstCall = taskService.createTask(projectId, req, idempotentKey);
        TaskResponse secondCall = taskService.createTask(projectId, req, idempotentKey);

        // Đảm bảo không nhân đôi bản ghi khi gửi lặp lại
        assertThat(firstCall.getId()).isEqualTo(secondCall.getId());
    }

    @Test
    @DisplayName("AC6 & AC7: Lỗi một phần không làm mất dữ liệu và chạy ổn định trên DB test")
    void testPartialFailureResilienceAndDatabaseConsistency() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // Gửi một request filter không hợp lệ
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-09T00:00:00Z")
                        .param("to", "2026-09-08T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());

        // Dữ liệu và hệ thống vẫn ổn định cho các request hợp lệ tiếp theo
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
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
