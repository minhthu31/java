package vn.edu.cnpm.projectsupport.task.service;

import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;

public interface TaskService {
    Task createTask(Long projectId, Long leaderUserId, CreateTaskRequest request);
    Task updateTaskStatusByMember(Long memberUserId, Long taskId, TaskStatus status, String reason);
}