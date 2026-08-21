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
    private Instant deadline;

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
    public Instant getDeadline() { return deadline; }
}