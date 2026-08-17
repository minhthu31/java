package vn.edu.cnpm.projectsupport.requirement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;
import vn.edu.cnpm.projectsupport.project.domain.Project;

@Entity
@Table(name = "requirements")
public class Requirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "jira_issue_key", length = 50)
    private String jiraIssueKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String actor;

    @Column(length = 30)
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String precondition;

    @Column(name = "main_flow", columnDefinition = "TEXT")
    private String mainFlow;

    @Column(name = "alternative_flow", columnDefinition = "TEXT")
    private String alternativeFlow;

    @Column(name = "exception_flow", columnDefinition = "TEXT")
    private String exceptionFlow;

    @Column(columnDefinition = "TEXT")
    private String postcondition;

    @Column(nullable = false, length = 30)
    private String status;

    public Requirement() {
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getJiraIssueKey() {
        return jiraIssueKey;
    }

    public void setJiraIssueKey(String jiraIssueKey) {
        this.jiraIssueKey = jiraIssueKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
    }

    public String getMainFlow() {
        return mainFlow;
    }

    public void setMainFlow(String mainFlow) {
        this.mainFlow = mainFlow;
    }

    public String getAlternativeFlow() {
        return alternativeFlow;
    }

    public void setAlternativeFlow(String alternativeFlow) {
        this.alternativeFlow = alternativeFlow;
    }

    public String getExceptionFlow() {
        return exceptionFlow;
    }

    public void setExceptionFlow(String exceptionFlow) {
        this.exceptionFlow = exceptionFlow;
    }

    public String getPostcondition() {
        return postcondition;
    }

    public void setPostcondition(String postcondition) {
        this.postcondition = postcondition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}