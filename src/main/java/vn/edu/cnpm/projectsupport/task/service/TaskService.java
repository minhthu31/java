package vn.edu.cnpm.projectsupport.task.service;

import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.entity.Task;
import vn.edu.cnpm.projectsupport.task.enums.TaskStatus;

public interface TaskService {
    Task createTask(String leaderId, CreateTaskRequest request);
    Task updateTaskStatusByMember(String memberId, String taskId, TaskStatus status);
}