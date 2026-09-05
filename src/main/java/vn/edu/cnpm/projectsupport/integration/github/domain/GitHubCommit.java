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

    @Column(name = "author_github_user_id")
    private Long authorGithubUserId;

    @Column(name = "author_login", length = 100)
    private String authorLogin;

    @Column(name = "git_author_name", length = 255)
    private String gitAuthorName;

    @Column(name = "git_author_email", length = 320)
    private String gitAuthorEmail;

    @Column(name = "git_committer_name", length = 255)
    private String gitCommitterName;

    @Column(name = "git_committer_email", length = 320)
    private String gitCommitterEmail;

    @Column(name = "committer_at")
    private Instant committerAt;

    @Column(name = "sha", nullable = false, length = 64)
    private String sha;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "additions", nullable = false)
    private Integer additions;

    @Column(name = "deletions", nullable = false)
    private Integer deletions;

    @Column(name = "files_changed")
    private Integer filesChanged;

    @Column(name = "parent_shas", columnDefinition = "TEXT")
    private String parentShas;

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
        this.additions = 0;
        this.deletions = 0;
    }

    public Long getId() { return id; }
    public Long getRepositoryId() { return repositoryId; }
    public Long getAuthorExternalAccountId() { return authorExternalAccountId; }
    public Long getAuthorGithubUserId() { return authorGithubUserId; }
    public String getAuthorLogin() { return authorLogin; }
    public String getGitAuthorName() { return gitAuthorName; }
    public String getGitAuthorEmail() { return gitAuthorEmail; }
    public String getGitCommitterName() { return gitCommitterName; }
    public String getGitCommitterEmail() { return gitCommitterEmail; }
    public Instant getCommitterAt() { return committerAt; }
    public String getSha() { return sha; }
    public String getMessage() { return message; }
    public Instant getCommittedAt() { return committedAt; }
    public String getHtmlUrl() { return htmlUrl; }
    public Integer getAdditions() { return additions; }
    public Integer getDeletions() { return deletions; }
    public Integer getFilesChanged() { return filesChanged; }
    public String getParentShas() { return parentShas; }
    public boolean isReverted() { return reverted; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAuthorExternalAccountId(Long value) { this.authorExternalAccountId = value; }
    public void setMessage(String value) { this.message = value; }
    public void setCommittedAt(Instant value) { this.committedAt = value; }
    public void setHtmlUrl(String value) { this.htmlUrl = value; }
    public void setAuthorGithubUserId(Long value) { this.authorGithubUserId = value; }
    public void setAuthorLogin(String value) { this.authorLogin = value; }
    public void setGitAuthorName(String value) { this.gitAuthorName = value; }
    public void setGitAuthorEmail(String value) { this.gitAuthorEmail = value; }
    public void setGitCommitterName(String value) { this.gitCommitterName = value; }
    public void setGitCommitterEmail(String value) { this.gitCommitterEmail = value; }
    public void setCommitterAt(Instant value) { this.committerAt = value; }
    public void setAdditions(Integer additions) { this.additions = additions == null ? 0 : additions; }
    public void setDeletions(Integer deletions) { this.deletions = deletions == null ? 0 : deletions; }
    public void setFilesChanged(Integer filesChanged) { this.filesChanged = filesChanged; }
    public void setParentShas(String parentShas) { this.parentShas = parentShas; }
    public void setReverted(boolean reverted) { this.reverted = reverted; }
}
