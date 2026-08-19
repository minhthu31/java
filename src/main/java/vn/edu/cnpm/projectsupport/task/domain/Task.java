package vn.edu.cnpm.projectsupport.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.project.domain.Feature;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.domain.Sprint;
import vn.edu.cnpm.projectsupport.requirement.domain.Requirement;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id")
    private Feature feature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id")
    private User assignee;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "acceptance_criteria", nullable = false, columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 30)
    private TaskIssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TaskClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column
    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status = TaskStatus.TO_DO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private SyncStatus syncStatus = SyncStatus.NOT_SYNCED;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    public Task() {
    }

    @PrePersist
    @PreUpdate
    private void validateProjectRelations() {
        if (project == null) {
            return;
        }
        validateSameProject(requirement == null ? null : requirement.getProject(), "requirement");
        validateSameProject(feature == null ? null : feature.getProject(), "feature");
        validateSameProject(sprint == null ? null : sprint.getProject(), "sprint");
    }

    private void validateSameProject(Project relatedProject, String relationName) {
        if (relatedProject == null) {
            throw new IllegalStateException(relationName + " must belong to the same project as task");
        }
        Long taskProjectId = project.getId();
        Long relatedProjectId = relatedProject.getId();
        boolean sameProject = taskProjectId != null && relatedProjectId != null
                ? taskProjectId.equals(relatedProjectId)
                : project == relatedProject;
        if (!sameProject) {
            throw new IllegalStateException(
                    relationName + " must belong to the same project as task");
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(Requirement requirement) {
        this.requirement = requirement;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

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

    public TaskClassification getClassification() {
        return classification;
    }

    public void setClassification(TaskClassification classification) {
        this.classification = classification;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
