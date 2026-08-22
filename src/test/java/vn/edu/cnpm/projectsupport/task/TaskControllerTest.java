package vn.edu.cnpm.projectsupport.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    private final String BASE_URL = "/api/v1/projects/1/tasks";

    @Test
    @DisplayName("GET List - Successful when user is TEAM_MEMBER")
    @WithMockUser(roles = "TEAM_MEMBER")
    void getTasks_Success_WhenTeamMember() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }
}
