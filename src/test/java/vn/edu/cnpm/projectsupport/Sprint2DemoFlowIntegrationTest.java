package vn.edu.cnpm.projectsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class Sprint2DemoFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void leaderCreatesRequirementAssignsTaskAndMemberSeesAssignedTask() throws Exception {
        JsonNode leader = login("leader.test", "password");
        JsonNode member = login("member.test", "password");

        long projectId = leader.path("projectId").asLong();
        long memberId = member.path("id").asLong();
        assertThat(projectId).isPositive();
        assertThat(member.path("projectId").asLong()).isEqualTo(projectId);
        assertThat(memberId).isPositive();

        String leaderToken = leader.path("accessToken").asText();
        String memberToken = member.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(memberId))
                .andExpect(jsonPath("$.data[0].username").value("member.test"));
        mockMvc.perform(get("/api/v1/projects/{projectId}/members", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        String requirementResponse = mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leaderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Requirement demo CNPM-72",
                                "actor", "Team Leader",
                                "description", "Dữ liệu dùng cho luồng demo Sprint 2",
                                "priority", "HIGH",
                                "mainFlow", "Leader tạo Requirement và Task"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long requirementId = objectMapper.readTree(requirementResponse)
                .path("data")
                .path("id")
                .asLong();

        String taskResponse = mockMvc.perform(post(
                        "/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leaderToken))
                        .header("Idempotency-Key", "CNPM-72-DEMO-FLOW")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Task được giao cho member.test",
                                "description", "Kiểm tra danh sách công việc được giao",
                                "acceptanceCriteria", "Member đăng nhập và nhìn thấy task",
                                "issueType", "TASK",
                                "priority", "HIGH",
                                "requirementId", requirementId,
                                "assigneeUserId", memberId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignee.username").value("member.test"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long taskId = objectMapper.readTree(taskResponse).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == %s)]", taskId).exists());

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[?(@.id == %s)]", taskId).exists());

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leaderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == %s)]", requirementId).exists());
    }

    private JsonNode login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.projectId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
