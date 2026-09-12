package vn.edu.cnpm.projectsupport.reporting;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void teamMemberSeesOnlyOwnContribution() throws Exception {
        JsonNode login = login("member.test", "password");
        long projectId = login.path("projectId").asLong();
        long memberId = login.path("id").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(memberId))
                .andExpect(jsonPath("$.data.memberContributions", hasSize(1)))
                .andExpect(jsonPath("$.data.memberContributions[0].memberId").value(memberId));
    }

    @Test
    void teamMemberCannotRequestAnotherMember() throws Exception {
        JsonNode login = login("member.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("memberId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }


    @Test
    void reportReturnsSprintNotFoundCode() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("sprintId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPRINT_NOT_FOUND"));
    }

    @Test
    void reportReturnsMemberNotFoundCode() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("memberId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void reportRejectsEqualFromAndTo() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-08T00:00:00Z")
                        .param("to", "2026-09-08T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_FILTER_INVALID"));
    }

    @Test
    void reportRejectsFromAfterTo() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-09T00:00:00Z")
                        .param("to", "2026-09-08T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_FILTER_INVALID"));
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
