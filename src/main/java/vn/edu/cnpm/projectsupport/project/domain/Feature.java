package vn.edu.cnpm.projectsupport.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "features")
public class Feature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_feature_id")
    private Feature parentFeature;

    @Column(name = "jira_epic_key", length = 50)
    private String jiraEpicKey;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Feature() {
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Feature getParentFeature() {
        return parentFeature;
    }

    public void setParentFeature(Feature parentFeature) {
        this.parentFeature = parentFeature;
    }

    public String getJiraEpicKey() {
        return jiraEpicKey;
    }

    public void setJiraEpicKey(String jiraEpicKey) {
        this.jiraEpicKey = jiraEpicKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}