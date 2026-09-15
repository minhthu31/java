package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "github_pull_request_commits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_github_pr_commit",
                columnNames = {"pull_request_id", "commit_id"}))
public class GitHubPullRequestCommit extends BaseEntity {

    @Column(name = "pull_request_id", nullable = false)
    private Long pullRequestId;

    @Column(name = "commit_id", nullable = false)
    private Long commitId;

    @Column(name = "commit_order", nullable = false)
    private Integer commitOrder;

    protected GitHubPullRequestCommit() {
    }

    public GitHubPullRequestCommit(Long pullRequestId, Long commitId, Integer commitOrder) {
        this.pullRequestId = pullRequestId;
        this.commitId = commitId;
        this.commitOrder = commitOrder;
    }

    public Long getPullRequestId() {
        return pullRequestId;
    }

    public Long getCommitId() {
        return commitId;
    }

    public Integer getCommitOrder() {
        return commitOrder;
    }

    public void setCommitOrder(Integer commitOrder) {
        this.commitOrder = commitOrder;
    }
}
