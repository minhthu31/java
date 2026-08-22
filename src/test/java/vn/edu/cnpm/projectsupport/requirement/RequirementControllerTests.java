package vn.edu.cnpm.projectsupport.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequirementController.class)
class RequirementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequirementService requirementService;

    private final String BASE_URL = "/api/v1/projects/1/requirements";

    @Test
    @DisplayName("POST - Successful when user is TEAM_LEADER")
    @WithMockUser(roles = "TEAM_LEADER")
    void createRequirement_Success_WhenTeamLeader() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();

        when(requirementService.createRequirement(eq(1L), any())).thenReturn(null);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST - Forbidden (403) when user is LECTURER")
    @WithMockUser(roles = "LECTURER")
    void createRequirement_Forbidden_WhenLecturer() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET List - Successful when user is LECTURER")
    @WithMockUser(roles = "LECTURER")
    void getRequirements_Success_WhenLecturer() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE - Successful (204 No Content) when user is TEAM_LEADER")
    @WithMockUser(roles = "TEAM_LEADER")
    void deleteRequirement_Success_WhenTeamLeader() throws Exception {
        doNothing().when(requirementService).deleteRequirement(1L, 10L);

        mockMvc.perform(delete(BASE_URL + "/10"))
                .andExpect(status().isNoContent());
    }
}
