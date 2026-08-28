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
@Table(name = "jira_backlog_snapshots", uniqueConstraints =
        @UniqueConstraint(name = "uk_jira_backlog_project", columnNames = "project_id"))
public class JiraBacklogSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "jira_project_key", nullable = false, length = 30)
    private String jiraProjectKey;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Column(name = "snapshot_hash", length = 128)
    private String snapshotHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_snapshot", columnDefinition = "JSON", nullable = false)
    private Map<String, Object> rawSnapshot;

    protected JiraBacklogSnapshot() {
    }

    public JiraBacklogSnapshot(Long projectId, String jiraProjectKey) {
        this.projectId = projectId;
        this.jiraProjectKey = jiraProjectKey;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getJiraProjectKey() { return jiraProjectKey; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public Map<String, Object> getRawSnapshot() { return rawSnapshot; }

    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }
    public void setRawSnapshot(Map<String, Object> rawSnapshot) {
        this.rawSnapshot = rawSnapshot == null ? null : new LinkedHashMap<>(rawSnapshot);
    }
}
