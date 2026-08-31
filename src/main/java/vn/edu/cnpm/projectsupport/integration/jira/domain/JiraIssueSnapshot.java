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
@Table(name = "jira_issue_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_jira_snapshot_project_id", columnNames = {"project_id", "jira_issue_id"}),
        @UniqueConstraint(name = "uk_jira_snapshot_project_key", columnNames = {"project_id", "jira_issue_key"})
})
public class JiraIssueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "jira_issue_id", nullable = false, length = 100)
    private String jiraIssueId;

    @Column(name = "jira_issue_key", nullable = false, length = 50)
    private String jiraIssueKey;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "issue_type", length = 100)
    private String issueType;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "url", length = 500)
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

    protected JiraIssueSnapshot() {
    }

    public JiraIssueSnapshot(Long projectId, String jiraIssueId, String jiraIssueKey) {
        this.projectId = projectId;
        this.jiraIssueId = jiraIssueId;
        this.jiraIssueKey = jiraIssueKey;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getJiraIssueId() { return jiraIssueId; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public String getSummary() { return summary; }
    public String getIssueType() { return issueType; }
    public String getStatus() { return status; }
    public String getUrl() { return url; }
    public Instant getRemoteUpdatedAt() { return remoteUpdatedAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public String getSnapshotHash() { return snapshotHash; }
    public Map<String, Object> getRawSnapshot() { return rawSnapshot; }

    public void setSummary(String summary) { this.summary = summary; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public void setStatus(String status) { this.status = status; }
    public void setUrl(String url) { this.url = url; }
    public void setRemoteUpdatedAt(Instant remoteUpdatedAt) { this.remoteUpdatedAt = remoteUpdatedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public void setSnapshotHash(String snapshotHash) { this.snapshotHash = snapshotHash; }
    public void setRawSnapshot(Map<String, Object> rawSnapshot) {
        this.rawSnapshot = rawSnapshot == null ? null : new LinkedHashMap<>(rawSnapshot);
    }
}
