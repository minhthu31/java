package vn.edu.cnpm.projectsupport.task;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@Validated
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // 1. Danh sách Task - LECTURER, TEAM_LEADER, TEAM_MEMBER
    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<PageResponse<TaskResponse>> getTasks(
            @PathVariable Long projectId,
            @ModelAttribute TaskFilterRequest filterRequest) {
        PageResponse<TaskResponse> responses = taskService.getTasks(projectId, filterRequest);
        return ApiResponse.success(responses);
    }

    // 2. Tạo Task mới - Chỉ TEAM_LEADER
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.createTask(projectId, request);
        return ApiResponse.success(response);
    }

    // 3. Lấy chi tiết Task - LECTURER, TEAM_LEADER, TEAM_MEMBER
    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<TaskResponse> getTaskById(
            @PathVariable Long projectId,
            @PathVariable Long taskId) {
        TaskResponse response = taskService.getTaskById(projectId, taskId);
        return ApiResponse.success(response);
    }

    // 4. Cập nhật Task - Chỉ TEAM_LEADER
    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        TaskResponse response = taskService.updateTask(projectId, taskId, request);
        return ApiResponse.success(response);
    }

    // 5. Chuyển trạng thái Task - TEAM_LEADER và TEAM_MEMBER
    @PatchMapping("/{taskId}/status")
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    public ApiResponse<TaskResponse> updateTaskStatus(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request) {
        TaskResponse response = taskService.updateStatus(projectId, taskId, request);
        return ApiResponse.success(response);
    }

    // 6. Gán / Bỏ gán Task - Chỉ TEAM_LEADER
    @PatchMapping("/{taskId}/assignee")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<TaskResponse> updateTaskAssignee(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestBody TaskAssigneeUpdateRequest request) {
        TaskResponse response = taskService.updateAssignee(projectId, taskId, request);
        return ApiResponse.success(response);
    }

    // 7. Xóa Task - Chỉ TEAM_LEADER
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