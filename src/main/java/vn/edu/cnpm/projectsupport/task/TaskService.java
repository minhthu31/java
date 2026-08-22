package vn.edu.cnpm.projectsupport.task;

import vn.edu.cnpm.projectsupport.common.api.PageResponse;

public interface TaskService {

    PageResponse<TaskResponse> getTasks(Long projectId, TaskFilterRequest filter);

    TaskResponse createTask(Long projectId, TaskCreateRequest request);

    TaskResponse getTaskById(Long projectId, Long taskId);

    TaskResponse updateTask(Long projectId, Long taskId, TaskUpdateRequest request);

    TaskResponse updateStatus(Long projectId, Long taskId, TaskStatusUpdateRequest request);

    TaskResponse updateAssignee(Long projectId, Long taskId, TaskAssigneeUpdateRequest request);

    void deleteTask(Long projectId, Long taskId);
}