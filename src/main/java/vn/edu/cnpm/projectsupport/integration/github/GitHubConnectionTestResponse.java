package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public class GitHubConnectionTestResponse {

    private Long projectId;
    private Boolean connected;
    private Long githubUserId;
    private String login;
    private Long githubRepositoryId;
    private String repositoryFullName;
    private String permission;
    private Long rateLimitRemaining;
    private Instant rateLimitResetAt;
    private Instant testedAt;

    public GitHubConnectionTestResponse() {
    }

    public GitHubConnectionTestResponse(Long projectId, Boolean connected, Long githubUserId, String login,
                                        Long githubRepositoryId, String repositoryFullName, String permission,
                                        Long rateLimitRemaining, Instant rateLimitResetAt, Instant testedAt) {
        this.projectId = projectId;
        this.connected = connected;
        this.githubUserId = githubUserId;
        this.login = login;
        this.githubRepositoryId = githubRepositoryId;
        this.repositoryFullName = repositoryFullName;
        this.permission = permission;
        this.rateLimitRemaining = rateLimitRemaining;
        this.rateLimitResetAt = rateLimitResetAt;
        this.testedAt = testedAt;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public Long getGithubUserId() {
        return githubUserId;
    }

    public void setGithubUserId(Long githubUserId) {
        this.githubUserId = githubUserId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public void setGithubRepositoryId(Long githubRepositoryId) {
        this.githubRepositoryId = githubRepositoryId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Long getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public void setRateLimitRemaining(Long rateLimitRemaining) {
        this.rateLimitRemaining = rateLimitRemaining;
    }

    public Instant getRateLimitResetAt() {
        return rateLimitResetAt;
    }

    public void setRateLimitResetAt(Instant rateLimitResetAt) {
        this.rateLimitResetAt = rateLimitResetAt;
    }

    public Instant getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(Instant testedAt) {
        this.testedAt = testedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long projectId;
        private Boolean connected;
        private Long githubUserId;
        private String login;
        private Long githubRepositoryId;
        private String repositoryFullName;
        private String permission;
        private Long rateLimitRemaining;
        private Instant rateLimitResetAt;
        private Instant testedAt;

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder connected(Boolean connected) {
            this.connected = connected;
            return this;
        }

        public Builder githubUserId(Long githubUserId) {
            this.githubUserId = githubUserId;
            return this;
        }

        public Builder login(String login) {
            this.login = login;
            return this;
        }

        public Builder githubRepositoryId(Long githubRepositoryId) {
            this.githubRepositoryId = githubRepositoryId;
            return this;
        }

        public Builder repositoryFullName(String repositoryFullName) {
            this.repositoryFullName = repositoryFullName;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder rateLimitRemaining(Long rateLimitRemaining) {
            this.rateLimitRemaining = rateLimitRemaining;
            return this;
        }

        public Builder rateLimitResetAt(Instant rateLimitResetAt) {
            this.rateLimitResetAt = rateLimitResetAt;
            return this;
        }

        public Builder testedAt(Instant testedAt) {
            this.testedAt = testedAt;
            return this;
        }

        public GitHubConnectionTestResponse build() {
            return new GitHubConnectionTestResponse(projectId, connected, githubUserId, login, githubRepositoryId,
                    repositoryFullName, permission, rateLimitRemaining, rateLimitResetAt, testedAt);
        }
    }
}