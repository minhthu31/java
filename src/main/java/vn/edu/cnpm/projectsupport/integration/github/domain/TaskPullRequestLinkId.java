package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskPullRequestLinkId implements Serializable {
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "pull_request_id")
    private Long pullRequestId;

    protected TaskPullRequestLinkId() {
    }

    public TaskPullRequestLinkId(Long taskId, Long pullRequestId) {
        this.taskId = taskId;
        this.pullRequestId = pullRequestId;
    }

    public Long getTaskId() { return taskId; }
    public Long getPullRequestId() { return pullRequestId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaskPullRequestLinkId that)) return false;
        return Objects.equals(taskId, that.taskId) && Objects.equals(pullRequestId, that.pullRequestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, pullRequestId);
    }
}
