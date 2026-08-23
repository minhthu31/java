package vn.edu.cnpm.projectsupport.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "tasks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tasks_idempotency_key",
                columnNames = "idempotency_key"))
public class Task extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "feature_id")
    private Long featureId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "acceptance_criteria", nullable = false, columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private TaskIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", length = 30)
    private TaskClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "deadline")
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.TO_DO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private SyncStatus syncStatus = SyncStatus.NOT_SYNCED;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    protected Task() {
    }

    public Task(
            Long projectId,
            String title,
            String acceptanceCriteria,
            TaskIssueType issueType,
            TaskPriority priority) {
        this.projectId = projectId;
        this.title = title;
        this.acceptanceCriteria = acceptanceCriteria;
        this.issueType = issueType;
        this.priority = priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public TaskIssueType getIssueType() {
        return issueType;
    }

    public TaskClassification getClassification() {
        return classification;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public void setFeatureId(Long featureId) {
        this.featureId = featureId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public void setIssueType(TaskIssueType issueType) {
        this.issueType = issueType;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public void setClassification(TaskClassification classification) {
        this.classification = classification;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
