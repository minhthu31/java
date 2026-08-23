package vn.edu.cnpm.projectsupport.task.service;

import java.util.List;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskAssigneeUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskFilterRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.UpdateTaskRequest;

public interface TaskService {
    PageResponse<TaskResponse> getTasks(Long projectId, TaskFilterRequest filter);
    TaskResponse createTask(Long projectId, CreateTaskRequest request);
    TaskResponse createTask(Long projectId, CreateTaskRequest request, String idempotencyKey);
    TaskResponse createTask(Long projectId, Long leaderUserId, CreateTaskRequest request);
    TaskResponse updateTask(Long projectId, Long taskId, UpdateTaskRequest request);
    TaskResponse updateTaskStatus(Long projectId, Long taskId, TaskStatusUpdateRequest request);
    TaskResponse updateTaskStatusByMember(Long projectId, Long memberUserId, Long taskId, TaskStatusUpdateRequest request);
    TaskResponse updateTaskAssignee(Long projectId, Long taskId, TaskAssigneeUpdateRequest request);
    TaskResponse getTaskById(Long projectId, Long taskId);
    List<TaskResponse> getTasksByProject(Long projectId);
    void deleteTask(Long projectId, Long taskId);
}
