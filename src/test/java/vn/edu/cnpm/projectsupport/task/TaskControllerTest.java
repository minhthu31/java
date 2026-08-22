package vn.edu.cnpm.projectsupport.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskStatusRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("POST - Forbidden (403) when user is TEAM_MEMBER")
    @WithMockUser(roles = "TEAM_MEMBER")
    void createTask_Forbidden_WhenTeamMember() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH Status - Successful when user is TEAM_MEMBER")
    @WithMockUser(roles = "TEAM_MEMBER")
    void updateTaskStatus_Success_WhenTeamMember() throws Exception {
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();

        when(taskService.updateStatus(eq(1L), eq(10L), any())).thenReturn(null);

        mockMvc.perform(patch(BASE_URL + "/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
