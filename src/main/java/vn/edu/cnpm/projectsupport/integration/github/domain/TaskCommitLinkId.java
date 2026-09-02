package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskCommitLinkId implements Serializable {
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "commit_id")
    private Long commitId;

    protected TaskCommitLinkId() {
    }

    public TaskCommitLinkId(Long taskId, Long commitId) {
        this.taskId = taskId;
        this.commitId = commitId;
    }

    public Long getTaskId() { return taskId; }
    public Long getCommitId() { return commitId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaskCommitLinkId that)) return false;
        return Objects.equals(taskId, that.taskId) && Objects.equals(commitId, that.commitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, commitId);
    }
}
