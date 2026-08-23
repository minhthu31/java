package vn.edu.cnpm.projectsupport.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskAssigneeUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskFilterRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskRequest;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class TaskControllerTests {

    private static final Long PROJECT_ID = 1L;
    private static final Long TASK_ID = 61L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/tasks";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    private TaskResponse response;

    @BeforeEach
    void setUp() {
        response = new TaskResponse();
        response.setId(TASK_ID);
        response.setProjectId(PROJECT_ID);
        response.setTitle("Xây dựng Task API contract và Controller");
        response.setAcceptanceCriteria("Đủ CRUD, status và assignee");
        response.setIssueType(TaskIssueType.TASK);
        response.setPriority(TaskPriority.HIGH);
        response.setStatus(TaskStatus.TO_DO);
        response.setSyncStatus(SyncStatus.NOT_SYNCED);
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderCreatesTaskWithIdempotencyKey() throws Exception {
        when(taskService.createTask(eq(PROJECT_ID), any(CreateTaskRequest.class), eq("cnpm-61-create")))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .header("Idempotency-Key", "cnpm-61-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(TASK_ID))
                .andExpect(jsonPath("$.data.status").value("TO_DO"));
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerListsTasksWithFilters() throws Exception {
        PageResponse<TaskResponse> page = new PageResponse<>(
                List.of(response), 0, 20, 1, 1, true, true);
        when(taskService.getTasks(eq(PROJECT_ID), any(TaskFilterRequest.class))).thenReturn(page);

        mockMvc.perform(get(BASE_URL, PROJECT_ID)
                        .param("status", "TO_DO")
                        .param("priority", "HIGH")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(TASK_ID))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCanListAssignedTasks() throws Exception {
        when(taskService.getTasks(eq(PROJECT_ID), any(TaskFilterRequest.class)))
                .thenReturn(new PageResponse<>(List.of(response), 0, 20, 1, 1, true, true));

        mockMvc.perform(get(BASE_URL, PROJECT_ID)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderGetsTaskDetail() throws Exception {
        when(taskService.getTaskById(PROJECT_ID, TASK_ID)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TASK_ID));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderUpdatesTask() throws Exception {
        when(taskService.updateTask(eq(PROJECT_ID), eq(TASK_ID), any(UpdateTaskRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TASK_ID));
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberUpdatesAssignedTaskStatus() throws Exception {
        response.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.updateTaskStatus(
                eq(PROJECT_ID), eq(TASK_ID), any(TaskStatusUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/{taskId}/status", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderUpdatesTaskAssignee() throws Exception {
        response.setAssigneeUserId(200L);
        when(taskService.updateTaskAssignee(
                eq(PROJECT_ID), eq(TASK_ID), any(TaskAssigneeUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/{taskId}/assignee", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeUserId\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUserId").value(200));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderDeletesEligibleTask() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(PROJECT_ID, TASK_ID);
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void invalidCreateReturnsBadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(taskService, never()).createTask(
                any(Long.class), any(CreateTaskRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void missingTaskReturnsNotFound() throws Exception {
        when(taskService.getTaskById(PROJECT_ID, 999L))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(get(BASE_URL + "/{taskId}", PROJECT_ID, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotCreateTask() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerCannotUpdateTaskStatus() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{taskId}/status", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotUpdateTaskContent() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());

        verify(taskService, never()).updateTask(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotReassignTask() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/{taskId}/assignee", PROJECT_ID, TASK_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeUserId\":200}"))
                .andExpect(status().isForbidden());

        verify(taskService, never()).updateTaskAssignee(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotDeleteTask() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{taskId}", PROJECT_ID, TASK_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(taskService, never()).deleteTask(any(), any());
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerCannotCreateTask() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());

        verify(taskService, never()).createTask(
                any(Long.class), any(CreateTaskRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotCreateTask() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isForbidden());

        verify(taskService, never()).createTask(
                any(Long.class), any(CreateTaskRequest.class), any(String.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotListTasks() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID)).andExpect(status().isForbidden());
    }

    private String validCreateBody() {
        return """
                {
                  "title":"Xây dựng Task API contract và Controller",
                  "acceptanceCriteria":"Đủ CRUD, status và assignee",
                  "issueType":"TASK",
                  "priority":"HIGH"
                }
                """;
    }
}
