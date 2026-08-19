package vn.edu.cnpm.projectsupport.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;
import vn.edu.cnpm.projectsupport.group.domain.StudentGroup;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "jira_site_url", length = 500)
    private String jiraSiteUrl;

    @Column(name = "jira_project_id", length = 100)
    private String jiraProjectId;

    @Column(name = "jira_project_key", length = 30)
    private String jiraProjectKey;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status;

    public Project() {
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJiraSiteUrl() {
        return jiraSiteUrl;
    }

    public void setJiraSiteUrl(String jiraSiteUrl) {
        this.jiraSiteUrl = jiraSiteUrl;
    }

    public String getJiraProjectId() {
        return jiraProjectId;
    }

    public void setJiraProjectId(String jiraProjectId) {
        this.jiraProjectId = jiraProjectId;
    }

    public String getJiraProjectKey() {
        return jiraProjectKey;
    }

    public void setJiraProjectKey(String jiraProjectKey) {
        this.jiraProjectKey = jiraProjectKey;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}