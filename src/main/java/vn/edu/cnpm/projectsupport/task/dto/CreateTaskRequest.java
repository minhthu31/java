package vn.edu.cnpm.projectsupport.task.dto;

import java.time.Instant;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;

public class CreateTaskRequest {
    private String title;
    private String description;
    private String acceptanceCriteria;
    private TaskIssueType issueType;
    private TaskPriority priority;
    private TaskClassification classification;
    private Long requirementId;
    private Long featureId;
    private Long sprintId;
    private Long assigneeUserId;
    private Instant deadline;

    public CreateTaskRequest() {}

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public TaskIssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(TaskIssueType issueType) {
        this.issueType = issueType;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskClassification getClassification() {
        return classification;
    }

    public void setClassification(TaskClassification classification) {
        this.classification = classification;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public void setFeatureId(Long featureId) {
        this.featureId = featureId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }
}