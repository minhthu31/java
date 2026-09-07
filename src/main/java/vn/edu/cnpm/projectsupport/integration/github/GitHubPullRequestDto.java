package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class GitHubPullRequestDto {
    private Long id;
    private Integer number;
    private String title;
    private String body;
    private String state;
    private Boolean draft;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
    @JsonProperty("closed_at")
    private Instant closedAt;
    @JsonProperty("merged_at")
    private Instant mergedAt;
    @JsonProperty("merge_commit_sha")
    private String mergeCommitSha;
    private Integer commits;
    private Integer additions;
    private Integer deletions;
    @JsonProperty("changed_files")
    private Integer changedFiles;

    private HeadBase head;
    private HeadBase base;
    private User user;

    public static class HeadBase {
        private String ref;
        private String sha;
        public String getRef() { return ref; }
        public void setRef(String ref) { this.ref = ref; }
        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
    }

    public static class User {
        private Long id;
        private String login;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Boolean getDraft() { return draft; }
    public void setDraft(Boolean draft) { this.draft = draft; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Instant getMergedAt() { return mergedAt; }
    public void setMergedAt(Instant mergedAt) { this.mergedAt = mergedAt; }
    public String getMergeCommitSha() { return mergeCommitSha; }
    public void setMergeCommitSha(String mergeCommitSha) { this.mergeCommitSha = mergeCommitSha; }
    public Integer getCommits() { return commits; }
    public void setCommits(Integer commits) { this.commits = commits; }
    public Integer getAdditions() { return additions; }
    public void setAdditions(Integer additions) { this.additions = additions; }
    public Integer getDeletions() { return deletions; }
    public void setDeletions(Integer deletions) { this.deletions = deletions; }
    public Integer getChangedFiles() { return changedFiles; }
    public void setChangedFiles(Integer changedFiles) { this.changedFiles = changedFiles; }
    public HeadBase getHead() { return head; }
    public void setHead(HeadBase head) { this.head = head; }
    public HeadBase getBase() { return base; }
    public void setBase(HeadBase base) { this.base = base; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}