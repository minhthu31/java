package vn.edu.cnpm.projectsupport.requirement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RequirementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequirementService requirementService;

    private final String BASE_URL = "/api/v1/projects/1/requirements";

    @Test
    @DisplayName("GET List - Successful when user is LECTURER")
    @WithMockUser(roles = "LECTURER")
    void getRequirements_Success_WhenLecturer() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }
}
