package vn.edu.cnpm.projectsupport.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.MDC;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "JSON")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "JSON")
    private Map<String, Object> newValue;

    @Column(name = "result", nullable = false, length = 30)
    private String result;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ActivityLog() {
    }

    public static ActivityLog taskCreated(Long groupId, Long taskId, Long actorUserId) {
        ActivityLog log = base(groupId, taskId, actorUserId, "CREATE");
        log.newValue = Map.of("status", "TO_DO");
        return log;
    }

    public static ActivityLog taskStatusChanged(
            Long groupId,
            Long taskId,
            Long actorUserId,
            String oldStatus,
            String newStatus,
            String reason) {
        ActivityLog log = base(groupId, taskId, actorUserId, "STATUS_CHANGED");
        log.oldValue = Map.of("status", oldStatus);
        log.newValue = reason == null || reason.isBlank()
                ? Map.of("status", newStatus)
                : Map.of("status", newStatus, "reason", reason);
        return log;
    }

    private static ActivityLog base(Long groupId, Long taskId, Long actorUserId, String action) {
        ActivityLog log = new ActivityLog();
        log.groupId = groupId;
        log.entityType = "TASK";
        log.entityId = String.valueOf(taskId);
        log.actorUserId = actorUserId;
        log.action = action;
        log.result = "SUCCESS";
        String currentCorrelationId = MDC.get("correlationId");
        log.correlationId = currentCorrelationId == null || currentCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
                : currentCorrelationId;
        return log;
    }

    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public Long getGroupId() { return groupId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public Map<String, Object> getOldValue() { return oldValue; }
    public Map<String, Object> getNewValue() { return newValue; }
    public String getResult() { return result; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
}
