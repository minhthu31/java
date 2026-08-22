package vn.edu.cnpm.projectsupport.task;

public class TaskAssigneeUpdateRequest {

    private Long assigneeUserId;

    public TaskAssigneeUpdateRequest() {}

    public TaskAssigneeUpdateRequest(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public Long getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }
}