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

    @Test
    @DisplayName("Admin có quyền xem báo cáo tổng hợp và tiến độ")
    void adminCanViewReports() throws Exception {
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
    }

    @Test
    @DisplayName("Thành viên không được xem báo cáo của Project khác mà mình không thuộc về")
    void memberCannotViewAnotherProjectReport() throws Exception {
        JsonNode login = login("member.test", "password");
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", 999999999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Thành viên không được yêu cầu xem dữ liệu báo cáo của người khác")
    void memberCannotRequestAnotherMemberData() throws Exception {
        JsonNode login = login("member.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("memberId", "999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
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
