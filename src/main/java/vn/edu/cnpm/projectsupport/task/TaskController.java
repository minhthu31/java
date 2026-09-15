package vn.edu.cnpm.projectsupport.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.*;
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
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'TEAM_MEMBER')"
            + " and @projectAuthorization.canViewTasks(#projectId)")
    public ApiResponse<PageResponse<TaskResponse>> getTasks(
            @PathVariable("projectId") Long projectId,
            @ModelAttribute TaskFilterRequest filter) {
        return ApiResponse.success(taskService.getTasks(projectId, filter));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_LEADER') and @projectAuthorization.canManageTasks(#projectId)")
    public ApiResponse<TaskResponse> createTask(
            @PathVariable("projectId") Long projectId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(projectId, request, idempotencyKey));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'LECTURER', 'TEAM_MEMBER')"
            + " and @projectAuthorization.canViewTask(#projectId, #taskId)")
    public ApiResponse<TaskResponse> getTaskById(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId) {
        return ApiResponse.success(taskService.getTaskById(projectId, taskId));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('TEAM_LEADER') and @projectAuthorization.canManageTasks(#projectId)")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ApiResponse.success(taskService.updateTask(projectId, taskId, request));
    }

    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')"
            + " and @projectAuthorization.canUpdateTask(#projectId, #taskId)")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ApiResponse.success(taskService.updateTaskStatus(projectId, taskId, request));
    }

    @PatchMapping("/{taskId}/assignee")
    @PreAuthorize("hasRole('TEAM_LEADER') and @projectAuthorization.canManageTasks(#projectId)")
    public ApiResponse<TaskResponse> updateTaskAssignee(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody TaskAssigneeUpdateRequest request) {
        return ApiResponse.success(taskService.updateTaskAssignee(projectId, taskId, request));
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_LEADER') and @projectAuthorization.canManageTasks(#projectId)")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId) {
        taskService.deleteTask(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}
