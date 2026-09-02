package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "github_repositories",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_github_repository_id", columnNames = "github_repository_id"),
            @UniqueConstraint(name = "uk_github_repository_full_name", columnNames = "full_name")
        })
public class GitHubRepository extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "github_repository_id", nullable = false)
    private Long githubRepositoryId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "default_branch", nullable = false, length = 255)
    private String defaultBranch;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected GitHubRepository() {
    }

    public GitHubRepository(
            Long projectId,
            Long githubRepositoryId,
            String fullName,
            String defaultBranch,
            String htmlUrl) {
        this.projectId = projectId;
        this.githubRepositoryId = githubRepositoryId;
        this.fullName = fullName;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
    }

    public Long getProjectId() { return projectId; }
    public Long getGithubRepositoryId() { return githubRepositoryId; }
    public String getFullName() { return fullName; }
    public String getDefaultBranch() { return defaultBranch; }
    public String getHtmlUrl() { return htmlUrl; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }

    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
