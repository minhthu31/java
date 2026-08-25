package vn.edu.cnpm.projectsupport.integration.jira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "jira_issues",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_jira_issue_task", columnNames = "task_id"),
            @UniqueConstraint(name = "uk_jira_issue_id", columnNames = "jira_issue_id"),
            @UniqueConstraint(name = "uk_jira_issue_key", columnNames = "jira_issue_key")
        })
public class JiraIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true)
    private Long taskId;

    @Column(name = "jira_issue_id", nullable = false, unique = true, length = 100)
    private String jiraIssueId;

    @Column(name = "jira_issue_key", nullable = false, unique = true, length = 50)
    private String jiraIssueKey;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "remote_updated_at")
    private Instant remoteUpdatedAt;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Column(name = "snapshot_hash", length = 128)
    private String snapshotHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_snapshot", columnDefinition = "JSON")
    private Map<String, Object> rawSnapshot;

    protected JiraIssue() {
    }

    public JiraIssue(
            Long taskId,
            String jiraIssueId,
            String jiraIssueKey,
            String url,
            Instant lastSyncedAt) {
        this.taskId = taskId;
        this.jiraIssueId = jiraIssueId;
        this.jiraIssueKey = jiraIssueKey;
        this.url = url;
        this.lastSyncedAt = lastSyncedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getJiraIssueId() {
        return jiraIssueId;
    }

    public String getJiraIssueKey() {
        return jiraIssueKey;
    }

    public String getUrl() {
        return url;
    }

    public Instant getRemoteUpdatedAt() {
        return remoteUpdatedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public Map<String, Object> getRawSnapshot() {
        return rawSnapshot;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRemoteUpdatedAt(Instant remoteUpdatedAt) {
        this.remoteUpdatedAt = remoteUpdatedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public void setRawSnapshot(Map<String, Object> rawSnapshot) {
        this.rawSnapshot = rawSnapshot == null ? null : new LinkedHashMap<>(rawSnapshot);
    }
}
