package vn.edu.cnpm.projectsupport.task.dto;

import java.time.Instant;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

public class TaskResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String acceptanceCriteria;
    private TaskStatus status;
    private TaskIssueType issueType;
    private TaskPriority priority;
    private TaskClassification classification;
    private SyncStatus syncStatus;
    private Long requirementId;
    private Long featureId;
    private Long sprintId;
    private Long assigneeUserId;
    private TaskAssigneeResponse assignee;
    private String jiraIssueKey;
    private Instant deadline;
    private Instant createdAt;
    private Instant updatedAt;

    public static TaskResponse fromEntity(Task task) {
        TaskResponse res = new TaskResponse();
        res.id = task.getId();
        res.projectId = task.getProjectId();
        res.title = task.getTitle();
        res.description = task.getDescription();
        res.acceptanceCriteria = task.getAcceptanceCriteria();
        res.status = task.getStatus();
        res.issueType = task.getIssueType();
        res.priority = task.getPriority();
        res.classification = task.getClassification();
        res.syncStatus = task.getSyncStatus();
        res.requirementId = task.getRequirementId();
        res.featureId = task.getFeatureId();
        res.sprintId = task.getSprintId();
        res.assigneeUserId = task.getAssigneeUserId();
        res.deadline = task.getDeadline();
        res.createdAt = task.getCreatedAt();
        res.updatedAt = task.getUpdatedAt();
        return res;
    }

    // Getters
    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public TaskStatus getStatus() { return status; }
    public TaskIssueType getIssueType() { return issueType; }
    public TaskPriority getPriority() { return priority; }
    public TaskClassification getClassification() { return classification; }
    public SyncStatus getSyncStatus() { return syncStatus; }
    public Long getRequirementId() { return requirementId; }
    public Long getFeatureId() { return featureId; }
    public Long getSprintId() { return sprintId; }
    public Long getAssigneeUserId() { return assigneeUserId; }
    public TaskAssigneeResponse getAssignee() { return assignee; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public Instant getDeadline() { return deadline; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setIssueType(TaskIssueType issueType) { this.issueType = issueType; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setClassification(TaskClassification classification) { this.classification = classification; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public void setFeatureId(Long featureId) { this.featureId = featureId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }
    public void setAssignee(TaskAssigneeResponse assignee) { this.assignee = assignee; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
