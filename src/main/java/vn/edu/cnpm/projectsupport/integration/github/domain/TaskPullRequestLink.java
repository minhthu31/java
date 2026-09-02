package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "task_pr_links")
public class TaskPullRequestLink {

    @EmbeddedId
    private TaskPullRequestLinkId id;

    @Column(name = "link_source", nullable = false, length = 20)
    private String linkSource;

    @Column(name = "linked_by_user_id")
    private Long linkedByUserId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "linked_at", nullable = false, insertable = false, updatable = false)
    private Instant linkedAt;

    protected TaskPullRequestLink() {
    }

    public TaskPullRequestLink(TaskPullRequestLinkId id, String linkSource) {
        this.id = id;
        this.linkSource = linkSource;
    }

    public TaskPullRequestLinkId getId() { return id; }
    public String getLinkSource() { return linkSource; }
    public Long getLinkedByUserId() { return linkedByUserId; }
    public String getReason() { return reason; }
    public Instant getLinkedAt() { return linkedAt; }

    public void setLinkedByUserId(Long value) { this.linkedByUserId = value; }
    public void setReason(String reason) { this.reason = reason; }
}
