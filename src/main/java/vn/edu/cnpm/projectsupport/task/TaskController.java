package vn.edu.cnpm.projectsupport.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskAssigneeUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskFilterRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskRequest;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@Validated
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<PageResponse<TaskResponse>> getTasks(
            @PathVariable Long projectId,
            @ModelAttribute TaskFilterRequest filter) {
        return ApiResponse.success(taskService.getTasks(projectId, filter));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> createTask(
            @PathVariable Long projectId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(projectId, request, idempotencyKey));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<TaskResponse> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTaskById(projectId, taskId));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.success(taskService.updateTask(projectId, taskId, request));
    }

    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ApiResponse.success(taskService.updateTaskStatus(projectId, taskId, request));
    }

    @PatchMapping("/{taskId}/assignee")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> updateTaskAssignee(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssigneeUpdateRequest request) {
        return ApiResponse.success(taskService.updateTaskAssignee(projectId, taskId, request));
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        taskService.deleteTask(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}
