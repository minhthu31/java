package vn.edu.cnpm.projectsupport.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "jira_site_url", length = 500)
    private String jiraSiteUrl;

    @Column(name = "jira_project_id", length = 100)
    private String jiraProjectId;

    @Column(name = "jira_project_key", length = 30)
    private String jiraProjectKey;

    @Column(name = "jira_last_synced_at")
    private java.time.Instant jiraLastSyncedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    protected Project() {
    }

    public Project(Long groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getJiraSiteUrl() { return jiraSiteUrl; }
    public String getJiraProjectId() { return jiraProjectId; }
    public String getJiraProjectKey() { return jiraProjectKey; }
    public java.time.Instant getJiraLastSyncedAt() { return jiraLastSyncedAt; }

    public void setJiraSiteUrl(String jiraSiteUrl) { this.jiraSiteUrl = jiraSiteUrl; }
    public void setJiraProjectId(String jiraProjectId) { this.jiraProjectId = jiraProjectId; }
    public void setJiraProjectKey(String jiraProjectKey) { this.jiraProjectKey = jiraProjectKey; }
    public void setJiraLastSyncedAt(java.time.Instant jiraLastSyncedAt) { this.jiraLastSyncedAt = jiraLastSyncedAt; }
}
