package vn.edu.cnpm.projectsupport.task.entity;

import jakarta.persistence.*;
import vn.edu.cnpm.projectsupport.task.enums.*;

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    private String projectId;
    private String featureId;
    private String createdById;
    private String assigneeId;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus = SyncStatus.NOT_SYNCED;

    public Task() {}

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getFeatureId() { return featureId; }
    public void setFeatureId(String featureId) { this.featureId = featureId; }

    public String getCreatedById() { return createdById; }
    public void setCreatedById(String createdById) { this.createdById = createdById; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
}