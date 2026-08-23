package vn.edu.cnpm.projectsupport.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskFilterRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskRequest;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class TaskControllerTest {

    private static final String BASE_URL = "/api/v1/projects/{projectId}/tasks";
    private static final Long PROJECT_ID = 101L;
    private static final Long TASK_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        taskResponse = new TaskResponse();
        taskResponse.setId(TASK_ID);
        taskResponse.setTitle("Implement Authentication Module");
    }

    @Test
    @DisplayName("401 Unauthorized - Khi chưa đăng nhập")
    void shouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 Bad Request - Validation thất bại khi thiếu thông tin bắt buộc")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void shouldReturn400_WhenInvalidTaskRequest() throws Exception {
        CreateTaskRequest invalidRequest = new CreateTaskRequest();
        invalidRequest.setTitle("");

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(taskService, never()).createTask(any(), any());
    }

    @Test
    @DisplayName("201 Created - TEAM_LEADER tạo Task thành công")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void leaderCreatesTask_Success() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Implement Authentication Module");

        when(taskService.createTask(eq(PROJECT_ID), any(CreateTaskRequest.class), any()))
                .thenReturn(taskResponse);

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(TASK_ID));
    }

    @Test
    @DisplayName("200 OK - TEAM_MEMBER cập nhật trạng thái Task")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void memberCanUpdateTaskStatus() throws Exception {
        TaskStatusUpdateRequest statusRequest = new TaskStatusUpdateRequest();

        when(taskService.updateTaskStatus(eq(PROJECT_ID), eq(TASK_ID), any(TaskStatusUpdateRequest.class)))
                .thenReturn(taskResponse);

        mockMvc.perform(patch(BASE_URL + "/{taskId}/status", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - TEAM_MEMBER không có quyền cập nhật toàn bộ Task (chỉ TEAM_LEADER)")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void memberCannotUpdateTask() throws Exception {
        UpdateTaskRequest updateRequest = new UpdateTaskRequest();

        mockMvc.perform(put(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - ADMIN không thao tác các tài nguyên học thuật")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotAccessTaskResource() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404 Not Found - Không tìm thấy Task")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void shouldReturn404_WhenTaskNotFound() throws Exception {
        when(taskService.getTaskById(PROJECT_ID, 999L))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(get(BASE_URL + "/{taskId}", PROJECT_ID, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
