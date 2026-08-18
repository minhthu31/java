package vn.edu.cnpm.projectsupport.task;

import java.time.Instant;
import vn.edu.cnpm.projectsupport.requirement.Priority;

public class TaskResponse {
    private Long id;
    private Long projectId;
    private Long requirementId;
    private Long featureId;
    private Long sprintId;
    private TaskAssigneeResponse assignee;
    private String title;
    private String description;
    private String acceptanceCriteria;
    private IssueType issueType;
    private TaskClassification classification;
    private Priority priority;
    private Instant deadline;
    private TaskStatus status;
    private SyncStatus syncStatus;
    private String jiraIssueKey;
    private Instant createdAt;
    private Instant updatedAt;

    public TaskResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getFeatureId() { return featureId; }
    public void setFeatureId(Long featureId) { this.featureId = featureId; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    public TaskAssigneeResponse getAssignee() { return assignee; }
    public void setAssignee(TaskAssigneeResponse assignee) { this.assignee = assignee; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }
    public TaskClassification getClassification() { return classification; }
    public void setClassification(TaskClassification classification) { this.classification = classification; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}