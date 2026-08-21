package vn.edu.cnpm.projectsupport.task.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

public class TaskStatusUpdateRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private TaskStatus status;

    private String reason;

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}