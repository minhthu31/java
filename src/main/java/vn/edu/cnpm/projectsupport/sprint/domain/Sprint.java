package vn.edu.cnpm.projectsupport.sprint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(name = "sprints", uniqueConstraints = @UniqueConstraint(
        name = "uk_project_jira_sprint", columnNames = {"project_id", "jira_sprint_id"}))
public class Sprint extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "jira_sprint_id")
    private Long jiraSprintId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "state", nullable = false, length = 30)
    private String state;

    @Column(name = "goal", length = 1000)
    private String goal;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected Sprint() {
    }

    public Sprint(Long projectId, String name, String state) {
        this.projectId = projectId;
        this.name = name;
        this.state = state;
    }

    public Long getProjectId() { return projectId; }
    public Long getJiraSprintId() { return jiraSprintId; }
    public String getName() { return name; }
    public String getState() { return state; }
    public String getGoal() { return goal; }
    public Instant getStartDate() { return startDate; }
    public Instant getEndDate() { return endDate; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }

    public void setJiraSprintId(Long jiraSprintId) { this.jiraSprintId = jiraSprintId; }
    public void setName(String name) { this.name = name; }
    public void setState(String state) { this.state = state; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
