package vn.edu.cnpm.projectsupport.task.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "task_activity_logs")
public class TaskActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    private TaskStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private TaskStatus newStatus;

    private String reason;
    private Instant createdAt = Instant.now();

    public TaskActivityLog() {}

    public TaskActivityLog(Long taskId, Long actorUserId, TaskStatus oldStatus, TaskStatus newStatus, String reason) {
        this.taskId = taskId;
        this.actorUserId = actorUserId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public Long getActorUserId() { return actorUserId; }
    public TaskStatus getOldStatus() { return oldStatus; }
    public TaskStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}