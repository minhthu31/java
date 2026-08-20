package vn.edu.cnpm.projectsupport.feature.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "features", uniqueConstraints = @UniqueConstraint(
        name = "uk_feature_jira_epic", columnNames = {"project_id", "jira_epic_key"}))
public class Feature extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "parent_feature_id")
    private Long parentFeatureId;

    @Column(name = "jira_epic_key", length = 50)
    private String jiraEpicKey;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    protected Feature() {
    }

    public Feature(Long projectId, String name) {
        this.projectId = projectId;
        this.name = name;
    }

    public Long getProjectId() { return projectId; }
    public Long getParentFeatureId() { return parentFeatureId; }
    public String getJiraEpicKey() { return jiraEpicKey; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setParentFeatureId(Long parentFeatureId) { this.parentFeatureId = parentFeatureId; }
    public void setJiraEpicKey(String jiraEpicKey) { this.jiraEpicKey = jiraEpicKey; }
    public void setDescription(String description) { this.description = description; }
}
