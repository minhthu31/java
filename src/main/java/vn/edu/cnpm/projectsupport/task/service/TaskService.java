package vn.edu.cnpm.projectsupport.task.service;

import java.util.List;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;

public interface TaskService {
    TaskResponse createTask(Long projectId, CreateTaskRequest request);
    TaskResponse createTask(Long projectId, Long leaderUserId, CreateTaskRequest request);
    TaskResponse updateTaskStatus(Long projectId, Long taskId, TaskStatusUpdateRequest request);
    TaskResponse updateTaskStatusByMember(Long projectId, Long memberUserId, Long taskId, TaskStatusUpdateRequest request);
    TaskResponse getTaskById(Long projectId, Long taskId);
    List<TaskResponse> getTasksByProject(Long projectId);
}
