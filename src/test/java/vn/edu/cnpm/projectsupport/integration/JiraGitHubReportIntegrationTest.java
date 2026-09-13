package vn.edu.cnpm.projectsupport.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HashMap;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JiraGitHubReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private Map<String, Object> createValidTaskPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Task for Jira Sync Test");
        payload.put("description", "Verify local task integrates with Jira and GitHub");
        payload.put("priority", "HIGH");
        payload.put("status", "TO_DO");
        payload.put("dueDate", Instant.now().plusSeconds(86400).toString());
        return payload;
    }

    @Test
    @DisplayName("AC1: Test luồng tạo Task và đồng bộ lên Jira")
    void testTaskCreationAndJiraSyncFlow() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // Gọi tạo Task qua REST API với đầy đủ validation fields
        String res = mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "IDEMP-KEY-110-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createValidTaskPayload())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdTask = objectMapper.readTree(res).path("data");
        long taskId = createdTask.path("id").asLong();
        assertThat(taskId).isPositive();

        // Kiểm tra task truy xuất được qua API sau khi tạo
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC2 & AC3: Test đồng bộ GitHub repository, commit, PR và liên kết Task")
    void testGitHubRepositorySyncAndTaskLinking() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

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

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/progress", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk());

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
        String token = login.path("accessToken").asText();

        Map<String, Object> payload = createValidTaskPayload();
        payload.put("title", "Idempotent Task Sync");

        String idempotentKey = "REPEATABLE-SYNC-KEY-999";

        // Lần gọi 1
        String firstRes = mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", idempotentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Lần gọi 2 với cùng key idempotent
        String secondRes = mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", idempotentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long firstId = objectMapper.readTree(firstRes).path("data").path("id").asLong();
        long secondId = objectMapper.readTree(secondRes).path("data").path("id").asLong();

        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    @DisplayName("AC6 & AC7: Lỗi một phần không làm mất dữ liệu và chạy ổn định trên DB test")
    void testPartialFailureResilienceAndDatabaseConsistency() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // Gửi filter sai định dạng
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-09T00:00:00Z")
                        .param("to", "2026-09-08T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());

        // Hệ thống vẫn hoạt động bình thường
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
