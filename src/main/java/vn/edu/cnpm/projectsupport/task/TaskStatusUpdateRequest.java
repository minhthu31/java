package vn.edu.cnpm.projectsupport.task;

import jakarta.validation.constraints.NotNull;

public class TaskStatusUpdateRequest {

    @NotNull(message = "Status must not be null")
    private TaskStatus status;

    private String reason;

    public TaskStatusUpdateRequest() {}

    public TaskStatusUpdateRequest(TaskStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}