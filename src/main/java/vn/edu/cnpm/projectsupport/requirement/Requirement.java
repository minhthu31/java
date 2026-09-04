package vn.edu.cnpm.projectsupport.requirement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "requirements")
public class Requirement extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "jira_issue_key", length = 50)
    private String jiraIssueKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "actor", length = 255)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private Priority priority;

    @Column(name = "precondition", columnDefinition = "TEXT")
    private String precondition;

    @Column(name = "main_flow", columnDefinition = "TEXT")
    private String mainFlow;

    @Column(name = "alternative_flow", columnDefinition = "TEXT")
    private String alternativeFlow;

    @Column(name = "exception_flow", columnDefinition = "TEXT")
    private String exceptionFlow;

    @Column(name = "postcondition", columnDefinition = "TEXT")
    private String postcondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RequirementStatus status = RequirementStatus.DRAFT;

    protected Requirement() {
    }

    public Requirement(Long projectId, String title) {
        this.projectId = projectId;
        this.title = title;
    }

    public Long getProjectId() { return projectId; }
    public String getJiraIssueKey() { return jiraIssueKey; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActor() { return actor; }
    public Priority getPriority() { return priority; }
    public String getPrecondition() { return precondition; }
    public String getMainFlow() { return mainFlow; }
    public String getAlternativeFlow() { return alternativeFlow; }
    public String getExceptionFlow() { return exceptionFlow; }
    public String getPostcondition() { return postcondition; }
    public RequirementStatus getStatus() { return status; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setActor(String actor) { this.actor = actor; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setPrecondition(String precondition) { this.precondition = precondition; }
    public void setMainFlow(String mainFlow) { this.mainFlow = mainFlow; }
    public void setAlternativeFlow(String alternativeFlow) { this.alternativeFlow = alternativeFlow; }
    public void setExceptionFlow(String exceptionFlow) { this.exceptionFlow = exceptionFlow; }
    public void setPostcondition(String postcondition) { this.postcondition = postcondition; }
    public void setStatus(RequirementStatus status) { this.status = status; }
}
