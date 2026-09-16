package vn.edu.cnpm.projectsupport.sprint;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
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
class SprintApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void projectRolesCanListOnlySprintsFromTheirProject() throws Exception {
        JsonNode leader = login("leader.test", "password");
        long projectId = leader.path("projectId").asLong();

        mockMvc.perform(get("/api/v1/projects/{projectId}/sprints", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(leader.path("accessToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data[*].projectId", everyItem(is((int) projectId))))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data[0].state").isNotEmpty());

        for (String username : new String[] {"lecturer.test", "member.test"}) {
            JsonNode user = login(username, "password");
            mockMvc.perform(get("/api/v1/projects/{projectId}/sprints", projectId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(user.path("accessToken").asText())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Test
    void sprintListRequiresAuthenticationAndProjectAccess() throws Exception {
        JsonNode member = login("member.test", "password");
        long projectId = member.path("projectId").asLong();

        mockMvc.perform(get("/api/v1/projects/{projectId}/sprints", projectId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/projects/{projectId}/sprints", projectId + 999_999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member.path("accessToken").asText())))
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
