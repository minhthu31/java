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
        name = "github_pull_requests",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_repository_pr", columnNames = {"repository_id", "number"}),
            @UniqueConstraint(name = "uk_github_pull_request_id", columnNames = "github_pull_request_id")
        })
public class GitHubPullRequest extends BaseEntity {

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(name = "github_pull_request_id")
    private Long githubPullRequestId;

    @Column(name = "author_external_account_id")
    private Long authorExternalAccountId;

    @Column(name = "author_github_user_id")
    private Long authorGithubUserId;

    @Column(name = "author_login", length = 100)
    private String authorLogin;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "head_ref", nullable = false, length = 255)
    private String headRef;

    @Column(name = "head_sha", length = 64)
    private String headSha;

    @Column(name = "base_ref", nullable = false, length = 255)
    private String baseRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 30)
    private GitHubPullRequestState state;

    @Column(name = "draft", nullable = false)
    private boolean draft;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merge_commit_sha", length = 64)
    private String mergeCommitSha;

    @Column(name = "commit_count")
    private Integer commitCount;

    @Column(name = "additions", nullable = false)
    private Integer additions;

    @Column(name = "deletions", nullable = false)
    private Integer deletions;

    @Column(name = "changed_files")
    private Integer changedFiles;

    @Column(name = "closed_at")
    private Instant closedAt;

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
        this(repositoryId, null, number, title, null, headRef, null, baseRef,
                parseState(state), false, null, null, 0, 0, 0, null, null, htmlUrl);
    }

    public GitHubPullRequest(
            Long repositoryId,
            Long githubPullRequestId,
            Integer number,
            String title,
            String body,
            String headRef,
            String headSha,
            String baseRef,
            GitHubPullRequestState state,
            boolean draft,
            Instant mergedAt,
            String mergeCommitSha,
            Integer commitCount,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            Instant closedAt,
            String htmlUrl) {
        this.repositoryId = repositoryId;
        this.githubPullRequestId = githubPullRequestId;
        this.number = number;
        this.title = title;
        this.body = body;
        this.headRef = headRef;
        this.headSha = headSha;
        this.baseRef = baseRef;
        this.state = state;
        this.draft = draft;
        this.mergedAt = mergedAt;
        this.mergeCommitSha = mergeCommitSha;
        this.commitCount = commitCount;
        this.additions = additions == null ? 0 : additions;
        this.deletions = deletions == null ? 0 : deletions;
        this.changedFiles = changedFiles;
        this.closedAt = closedAt;
        this.htmlUrl = htmlUrl;
    }

    private static GitHubPullRequestState parseState(String state) {
        if (state == null) {
            throw new IllegalArgumentException("Pull request state must not be null");
        }
        return GitHubPullRequestState.valueOf(state.trim().toUpperCase());
    }

    public Long getRepositoryId() { 
        return repositoryId; 
    }
    public Long getGithubPullRequestId() { 
        return githubPullRequestId; 
    }
    public Long getAuthorExternalAccountId() { 
        return authorExternalAccountId; 
    }
    public Long getAuthorGithubUserId() { 
        return authorGithubUserId; 
    }
    public String getAuthorLogin() { 
        return authorLogin; 
    }
    public Integer getNumber() { 
        return number; 
    }
    public String getTitle() { 
        return title; 
    }
    public String getBody() { 
        return body; 
    }
    public String getHeadRef() { 
        return headRef; 
    }
    public String getHeadSha() { 
        return headSha; 
    }
    public String getBaseRef() { 
        return baseRef; 
    }
    public GitHubPullRequestState getState() { 
        return state; 
    }
    public boolean isDraft() { 
        return draft; 
    }
    public Instant getMergedAt() { 
        return mergedAt; 
    }
    public String getMergeCommitSha() { 
        return mergeCommitSha; 
    }
    public Integer getCommitCount() { 
        return commitCount; 
    }
    public Integer getAdditions() { 
        return additions; 
    }
    public Integer getDeletions() { 
        return deletions; 
    }
    public Integer getChangedFiles() { 
        return changedFiles; 
    }
    public Instant getClosedAt() { 
        return closedAt; 
    }
    public String getHtmlUrl() { 
        return htmlUrl; 
    }

    public void setAuthorExternalAccountId(Long value) { 
        this.authorExternalAccountId = value; 
    }
    public void setAuthorGithubUserId(Long value) { 
        this.authorGithubUserId = value; 
    }
    public void setAuthorLogin(String value) { 
        this.authorLogin = value; 
    }
    public void setState(GitHubPullRequestState state) { 
        this.state = state; 
    }
    public void setState(String state) { 
        this.state = parseState(state); 
    }
    public void setBody(String body) { 
        this.body = body; 
    }
    public void setHeadSha(String headSha) { 
        this.headSha = headSha; 
    }
    public void setDraft(boolean draft) { 
        this.draft = draft; 
    }
    public void setMergedAt(Instant mergedAt) { 
        this.mergedAt = mergedAt; 
    }
    public void setMergeCommitSha(String mergeCommitSha) { 
        this.mergeCommitSha = mergeCommitSha; 
    }
    public void setCommitCount(Integer commitCount) { 
        this.commitCount = commitCount; 
    }
    public void setAdditions(Integer additions) { 
        this.additions = additions == null ? 0 : additions; 
    }
    public void setDeletions(Integer deletions) { 
        this.deletions = deletions == null ? 0 : deletions; 
    }
    public void setChangedFiles(Integer changedFiles) { 
        this.changedFiles = changedFiles; 
    }
    public void setClosedAt(Instant closedAt) { 
        this.closedAt = closedAt; 
    }

    public void applyMergedAtState() {
        if (mergedAt != null) {
            state = GitHubPullRequestState.MERGED;
        }
    }
}
