package vn.edu.cnpm.projectsupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.requirement.Priority;
import vn.edu.cnpm.projectsupport.task.IssueType;
import vn.edu.cnpm.projectsupport.task.SyncStatus;
import vn.edu.cnpm.projectsupport.task.TaskAssigneeResponse;
import vn.edu.cnpm.projectsupport.task.TaskAssigneeUpdateRequest;
import vn.edu.cnpm.projectsupport.task.TaskClassification;
import vn.edu.cnpm.projectsupport.task.TaskController;
import vn.edu.cnpm.projectsupport.task.TaskCreateRequest;
import vn.edu.cnpm.projectsupport.task.TaskFilterRequest;
import vn.edu.cnpm.projectsupport.task.TaskResponse;
import vn.edu.cnpm.projectsupport.task.TaskService;
import vn.edu.cnpm.projectsupport.task.TaskStatus;
import vn.edu.cnpm.projectsupport.task.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.TaskUpdateRequest;

@ExtendWith(MockitoExtension.class)
class TaskControllerTests {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private Long projectId;
    private Long taskId;
    private TaskResponse sampleResponse;
    private PageResponse<TaskResponse> samplePageResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        projectId = 10L;
        taskId = 501L;

        sampleResponse = new TaskResponse();
        sampleResponse.setId(taskId);
        sampleResponse.setProjectId(projectId);
        sampleResponse.setRequirementId(101L);
        sampleResponse.setFeatureId(12L);
        sampleResponse.setSprintId(2L);
        sampleResponse.setTitle("Xây dựng Login API");
        sampleResponse.setDescription("Tạo endpoint đăng nhập theo contract");
        sampleResponse.setAcceptanceCriteria("Đăng nhập hợp lệ trả token; sai thông tin trả 401");
        sampleResponse.setIssueType(IssueType.TASK);
        sampleResponse.setClassification(TaskClassification.FEATURE_RELATED);
        sampleResponse.setPriority(Priority.HIGH);
        sampleResponse.setStatus(TaskStatus.TO_DO);
        sampleResponse.setSyncStatus(SyncStatus.NOT_SYNCED);
        sampleResponse.setCreatedAt(Instant.now());
        sampleResponse.setUpdatedAt(Instant.now());
        sampleResponse.setAssignee(new TaskAssigneeResponse(4L, "member.test", "Test Member"));

        samplePageResponse = new PageResponse<>(
                List.of(sampleResponse),
                0,
                20,
                1L,
                1,
                true,
                true
        );
    }

    // ==========================================
    // 1. HAPPY PATH TESTS
    // ==========================================
    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("POST /api/v1/projects/{projectId}/tasks -> 201 Created")
        void createTask_Success_Returns201() throws Exception {
            String requestJson = """
                    {
                        "requirementId": 101,
                        "featureId": 12,
                        "sprintId": 2,
                        "assigneeUserId": 4,
                        "title": "Xây dựng Login API",
                        "description": "Tạo endpoint đăng nhập theo contract",
                        "acceptanceCriteria": "Đăng nhập hợp lệ trả token; sai thông tin trả 401",
                        "issueType": "TASK",
                        "classification": "FEATURE_RELATED",
                        "priority": "HIGH"
                    }
                    """;

            when(taskService.createTask(eq(projectId), any(TaskCreateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(taskId))
                    .andExpect(jsonPath("$.data.title").value("Xây dựng Login API"));

            verify(taskService).createTask(eq(projectId), any(TaskCreateRequest.class));
        }

        @Test
        @DisplayName("GET /api/v1/projects/{projectId}/tasks -> 200 OK (PageResponse)")
        void getTasks_Success_Returns200() throws Exception {
            when(taskService.getTasks(eq(projectId), any(TaskFilterRequest.class)))
                    .thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                            .param("status", "TO_DO")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(taskId))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(taskService).getTasks(eq(projectId), any(TaskFilterRequest.class));
        }

        @Test
        @DisplayName("GET /api/v1/projects/{projectId}/tasks/{taskId} -> 200 OK")
        void getTaskById_Success_Returns200() throws Exception {
            when(taskService.getTaskById(projectId, taskId))
                    .thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(taskId))
                    .andExpect(jsonPath("$.data.title").value("Xây dựng Login API"));

            verify(taskService).getTaskById(projectId, taskId);
        }

        @Test
        @DisplayName("PUT /api/v1/projects/{projectId}/tasks/{taskId} -> 200 OK")
        void updateTask_Success_Returns200() throws Exception {
            String updateJson = """
                    {
                        "title": "Cập nhật Login API",
                        "acceptanceCriteria": "Tiêu chí mới",
                        "issueType": "TASK",
                        "priority": "MEDIUM"
                    }
                    """;

            when(taskService.updateTask(eq(projectId), eq(taskId), any(TaskUpdateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(taskId));

            verify(taskService).updateTask(eq(projectId), eq(taskId), any(TaskUpdateRequest.class));
        }

        @Test
        @DisplayName("PATCH /api/v1/projects/{projectId}/tasks/{taskId}/status -> 200 OK")
        void updateTaskStatus_Success_Returns200() throws Exception {
            String patchJson = """
                    {
                        "status": "IN_PROGRESS",
                        "reason": "Bắt đầu triển khai"
                    }
                    """;

            when(taskService.updateStatus(eq(projectId), eq(taskId), any(TaskStatusUpdateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/status", projectId, taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isOk());

            verify(taskService).updateStatus(eq(projectId), eq(taskId), any(TaskStatusUpdateRequest.class));
        }

        @Test
        @DisplayName("PATCH /api/v1/projects/{projectId}/tasks/{taskId}/assignee -> 200 OK")
        void updateTaskAssignee_Success_Returns200() throws Exception {
            String patchAssigneeJson = """
                    {
                        "assigneeUserId": 4
                    }
                    """;

            when(taskService.updateAssignee(eq(projectId), eq(taskId), any(TaskAssigneeUpdateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/assignee", projectId, taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchAssigneeJson))
                    .andExpect(status().isOk());

            verify(taskService).updateAssignee(eq(projectId), eq(taskId), any(TaskAssigneeUpdateRequest.class));
        }

        @Test
        @DisplayName("DELETE /api/v1/projects/{projectId}/tasks/{taskId} -> 204 No Content")
        void deleteTask_Success_Returns204() throws Exception {
            doNothing().when(taskService).deleteTask(projectId, taskId);

            mockMvc.perform(delete("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId))
                    .andExpect(status().isNoContent());

            verify(taskService).deleteTask(projectId, taskId);
        }
    }

    // ==========================================
    // 2. VALIDATION & EXCEPTION TESTS
    // ==========================================
    @Nested
    @DisplayName("Validation and Exception Tests")
    class ValidationAndExceptionTests {

        @Test
        @DisplayName("POST /api/v1/projects/{projectId}/tasks (Blank Title) -> 400 Bad Request")
        void createTask_BlankTitle_Returns400() throws Exception {
            String invalidJson = """
                    {
                        "title": "",
                        "acceptanceCriteria": "AC",
                        "issueType": "TASK",
                        "priority": "HIGH"
                    }
                    """;

            mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(taskService, never()).createTask(any(), any());
        }

        @Test
        @DisplayName("GET /api/v1/projects/{projectId}/tasks/{taskId} (Not Found) -> 404 Not Found")
        void getTaskById_NotFound_Returns404() throws Exception {
            when(taskService.getTaskById(projectId, 999L))
                    .thenThrow(new ResourceNotFoundException("Task not found"));

            mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, 999L))
                    .andExpect(status().isNotFound());

            verify(taskService).getTaskById(projectId, 999L);
        }

        @Test
        @DisplayName("DELETE /api/v1/projects/{projectId}/tasks/{taskId} (Not Found) -> 404 Not Found")
        void deleteTask_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Task not found"))
                    .when(taskService).deleteTask(projectId, 999L);

            mockMvc.perform(delete("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, 999L))
                    .andExpect(status().isNotFound());

            verify(taskService).deleteTask(projectId, 999L);
        }
    }
}