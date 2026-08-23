package vn.edu.cnpm.projectsupport.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    @DisplayName("401 Unauthorized - Khi truy cập API Task mà chưa đăng nhập")
    void shouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 Bad Request - Validation thất bại khi thiếu nội dung Task")
    @WithMockUser(roles = "TEAM_LEADER")
    void shouldReturn400_WhenInvalidTaskRequest() throws Exception {
        TaskCreateRequest invalidRequest = new TaskCreateRequest();

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("200 OK - TEAM_MEMBER chỉ xem Task được giao trong dự án")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void getTasks_Success_WhenTeamMember() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - TEAM_MEMBER không có quyền khởi tạo Task")
    @WithMockUser(roles = "TEAM_MEMBER")
    void createTask_Forbidden_WhenTeamMember() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("Task for Leader");

        when(taskService.createTask(eq(1L), any()))
                .thenThrow(new AccessDeniedException("Team members cannot create tasks"));

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 OK - TEAM_MEMBER cập nhật Task được giao cho chính mình")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void updateTaskStatus_Success_WhenAssignee() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("IN_PROGRESS");

        when(taskService.updateStatus(eq(1L), eq(10L), any())).thenReturn(new TaskResponse());

        mockMvc.perform(patch(BASE_URL + "/10/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - TEAM_MEMBER không được phép cập nhật Task của người khác")
    @WithMockUser(username = "member2", roles = "TEAM_MEMBER")
    void updateTaskStatus_Forbidden_WhenNotAssignee() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("DONE");

        when(taskService.updateStatus(eq(1L), eq(10L), any()))
                .thenThrow(new AccessDeniedException("User is not the assignee of this task"));

        mockMvc.perform(patch(BASE_URL + "/10/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
