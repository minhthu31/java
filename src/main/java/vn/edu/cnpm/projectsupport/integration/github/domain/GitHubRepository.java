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

    @Column(name = "node_id", length = 255)
    private String nodeId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "owner_github_user_id")
    private Long ownerGithubUserId;

    @Column(name = "owner_login", nullable = false, length = 100)
    private String ownerLogin;

    @Column(name = "private_repository", nullable = false)
    private boolean privateRepository;

    @Column(name = "default_branch", nullable = false, length = 255)
    private String defaultBranch;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "remote_updated_at")
    private Instant remoteUpdatedAt;

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
        this(projectId, githubRepositoryId, null,
                fullName == null ? null : fullName.substring(fullName.lastIndexOf('/') + 1),
                fullName == null ? null : fullName.substring(0, fullName.lastIndexOf('/')),
                fullName, false, defaultBranch, htmlUrl, false, null);
    }

    public GitHubRepository(
            Long projectId,
            Long githubRepositoryId,
            String nodeId,
            String name,
            String ownerLogin,
            String fullName,
            boolean privateRepository,
            String defaultBranch,
            String htmlUrl,
            boolean archived,
            Instant remoteUpdatedAt) {
        this.projectId = projectId;
        this.githubRepositoryId = githubRepositoryId;
        this.nodeId = nodeId;
        this.name = name;
        this.ownerLogin = ownerLogin;
        this.fullName = fullName;
        this.privateRepository = privateRepository;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
        this.archived = archived;
        this.remoteUpdatedAt = remoteUpdatedAt;
    }

    public Long getProjectId() { return projectId; }
    public Long getGithubRepositoryId() { return githubRepositoryId; }
    public String getNodeId() { return nodeId; }
    public String getName() { return name; }
    public String getFullName() { return fullName; }
    public Long getOwnerGithubUserId() { return ownerGithubUserId; }
    public String getOwnerLogin() { return ownerLogin; }
    public boolean isPrivateRepository() { return privateRepository; }
    public String getDefaultBranch() { return defaultBranch; }
    public String getHtmlUrl() { return htmlUrl; }
    public boolean isArchived() { return archived; }
    public Instant getRemoteUpdatedAt() { return remoteUpdatedAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public void setName(String name) { this.name = name; }
    public void setOwnerGithubUserId(Long ownerGithubUserId) { this.ownerGithubUserId = ownerGithubUserId; }
    public void setOwnerLogin(String ownerLogin) { this.ownerLogin = ownerLogin; }
    public void setPrivateRepository(boolean privateRepository) { this.privateRepository = privateRepository; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setRemoteUpdatedAt(Instant remoteUpdatedAt) { this.remoteUpdatedAt = remoteUpdatedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
