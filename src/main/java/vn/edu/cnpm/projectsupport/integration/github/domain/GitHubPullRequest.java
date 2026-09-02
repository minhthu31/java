package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "github_pull_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_repository_pr",
                columnNames = {"repository_id", "number"}))
public class GitHubPullRequest extends BaseEntity {

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "author_external_account_id")
    private Long authorExternalAccountId;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "head_ref", nullable = false, length = 255)
    private String headRef;

    @Column(name = "base_ref", nullable = false, length = 255)
    private String baseRef;

    @Column(name = "state", nullable = false, length = 30)
    private String state;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    protected GitHubPullRequest() {
    }

    public GitHubPullRequest(
            Long repositoryId,
            Integer number,
            String title,
            String headRef,
            String baseRef,
            String state,
            String htmlUrl) {
        this.repositoryId = repositoryId;
        this.number = number;
        this.title = title;
        this.headRef = headRef;
        this.baseRef = baseRef;
        this.state = state;
        this.htmlUrl = htmlUrl;
    }

    public Long getRepositoryId() { return repositoryId; }
    public Long getAuthorExternalAccountId() { return authorExternalAccountId; }
    public Integer getNumber() { return number; }
    public String getTitle() { return title; }
    public String getHeadRef() { return headRef; }
    public String getBaseRef() { return baseRef; }
    public String getState() { return state; }
    public Instant getMergedAt() { return mergedAt; }
    public String getHtmlUrl() { return htmlUrl; }

    public void setAuthorExternalAccountId(Long value) { this.authorExternalAccountId = value; }
    public void setState(String state) { this.state = state; }
    public void setMergedAt(Instant mergedAt) { this.mergedAt = mergedAt; }
}
