package vn.edu.cnpm.projectsupport.task.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskStatusRequest;
import vn.edu.cnpm.projectsupport.task.entity.Task;
import vn.edu.cnpm.projectsupport.task.service.TaskService;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestHeader("X-User-Id") String leaderId,
            @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(leaderId, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @RequestHeader("X-User-Id") String memberId,
            @PathVariable("id") String taskId,
            @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatusByMember(memberId, taskId, request.getStatus()));
    }
}