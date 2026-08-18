package vn.edu.cnpm.projectsupport.task;

import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.cnpm.projectsupport.requirement.Priority;

public class TaskCreateRequest {

    private Long requirementId;
    private Long featureId;
    private Long sprintId;
    private Long assigneeUserId;

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private String description;

    @NotBlank(message = "Acceptance criteria must not be blank")
    private String acceptanceCriteria;

    @NotNull(message = "Issue type is required")
    private IssueType issueType;

    private TaskClassification classification;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private Instant deadline;

    public TaskCreateRequest() {}

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getFeatureId() { return featureId; }
    public void setFeatureId(Long featureId) { this.featureId = featureId; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    public Long getAssigneeUserId() { return assigneeUserId; }
    public void setAssigneeUserId(Long assigneeUserId) { this.assigneeUserId = assigneeUserId; }
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
}