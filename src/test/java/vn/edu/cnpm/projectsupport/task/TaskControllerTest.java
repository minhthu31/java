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
    @DisplayName("200 OK - TEAM_MEMBER chỉ xem Task được giao")
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

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 OK - MEMBER cập nhật Task được giao cho chính mình thành công")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void updateTaskStatus_Success_WhenAssignee() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();

        when(taskService.updateStatus(eq(1L), eq(10L), any())).thenReturn(null);

        mockMvc.perform(patch(BASE_URL + "/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - MEMBER không được phép cập nhật Task của người khác")
    @WithMockUser(username = "member2", roles = "TEAM_MEMBER")
    void updateTaskStatus_Forbidden_WhenNotAssignee() throws Exception {
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();

        when(taskService.updateStatus(eq(1L), eq(10L), any()))
                .thenThrow(new AccessDeniedException("User is not the assignee of this task"));

        mockMvc.perform(patch(BASE_URL + "/10/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
