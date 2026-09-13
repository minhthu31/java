package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "github_check_runs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_github_check_run_repository_external",
                columnNames = {"repository_id", "external_id"}))
public class GitHubCheckRun extends BaseEntity {

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "external_id", nullable = false)
    private Long externalId;

    @Column(name = "commit_sha", nullable = false, length = 64)
    private String commitSha;

    @Column(name = "pull_request_id")
    private Long pullRequestId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private GitHubCheckRunStatus status;

    @Column(name = "html_url", length = 500)
    private String htmlUrl;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected GitHubCheckRun() {
    }

    public GitHubCheckRun(
            Long repositoryId,
            Long externalId,
            String commitSha,
            String name,
            GitHubCheckRunStatus status,
            String htmlUrl,
            Instant startedAt,
            Instant completedAt) {
        this.repositoryId = repositoryId;
        this.externalId = externalId;
        this.commitSha = commitSha;
        this.name = name;
        this.status = status;
        this.htmlUrl = htmlUrl;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long getRepositoryId() { return repositoryId; }
    public Long getExternalId() { return externalId; }
    public String getCommitSha() { return commitSha; }
    public Long getPullRequestId() { return pullRequestId; }
    public String getName() { return name; }
    public GitHubCheckRunStatus getStatus() { return status; }
    public String getHtmlUrl() { return htmlUrl; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setPullRequestId(Long pullRequestId) { this.pullRequestId = pullRequestId; }

    public void update(
            String commitSha,
            Long pullRequestId,
            String name,
            GitHubCheckRunStatus status,
            String htmlUrl,
            Instant startedAt,
            Instant completedAt) {
        this.commitSha = commitSha;
        this.pullRequestId = pullRequestId;
        this.name = name;
        this.status = status;
        this.htmlUrl = htmlUrl;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }
}
