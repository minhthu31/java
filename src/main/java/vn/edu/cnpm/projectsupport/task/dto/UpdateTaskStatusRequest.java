package vn.edu.cnpm.projectsupport.task.dto;

import vn.edu.cnpm.projectsupport.task.enums.TaskStatus;

public class UpdateTaskStatusRequest {
    private TaskStatus status;

    public UpdateTaskStatusRequest() {}

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
}