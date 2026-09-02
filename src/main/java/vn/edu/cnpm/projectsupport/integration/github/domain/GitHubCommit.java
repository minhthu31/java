package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "github_commits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_repository_commit",
                columnNames = {"repository_id", "sha"}))
public class GitHubCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "author_external_account_id")
    private Long authorExternalAccountId;

    @Column(name = "sha", nullable = false, length = 64)
    private String sha;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "is_reverted", nullable = false)
    private boolean reverted;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected GitHubCommit() {
    }

    public GitHubCommit(
            Long repositoryId,
            String sha,
            String message,
            Instant committedAt,
            String htmlUrl) {
        this.repositoryId = repositoryId;
        this.sha = sha;
        this.message = message;
        this.committedAt = committedAt;
        this.htmlUrl = htmlUrl;
    }

    public Long getId() { return id; }
    public Long getRepositoryId() { return repositoryId; }
    public Long getAuthorExternalAccountId() { return authorExternalAccountId; }
    public String getSha() { return sha; }
    public String getMessage() { return message; }
    public Instant getCommittedAt() { return committedAt; }
    public String getHtmlUrl() { return htmlUrl; }
    public Integer getAdditions() { return additions; }
    public Integer getDeletions() { return deletions; }
    public Integer getFilesChanged() { return filesChanged; }
    public boolean isReverted() { return reverted; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAuthorExternalAccountId(Long value) { this.authorExternalAccountId = value; }
    public void setAdditions(Integer additions) { this.additions = additions; }
    public void setDeletions(Integer deletions) { this.deletions = deletions; }
    public void setFilesChanged(Integer filesChanged) { this.filesChanged = filesChanged; }
    public void setReverted(boolean reverted) { this.reverted = reverted; }
}
