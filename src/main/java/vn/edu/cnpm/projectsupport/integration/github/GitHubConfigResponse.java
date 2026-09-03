package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public class GitHubConfigResponse {

    private Long projectId;
    private String repositoryFullName;
    private Boolean configured;
    private String status;
    private String githubLogin;
    private Instant lastTestedAt;
    private Boolean lastTestSucceeded;

    public GitHubConfigResponse() {
    }

    public GitHubConfigResponse(Long projectId, String repositoryFullName, Boolean configured,
                                String status, String githubLogin, Instant lastTestedAt, Boolean lastTestSucceeded) {
        this.projectId = projectId;
        this.repositoryFullName = repositoryFullName;
        this.configured = configured;
        this.status = status;
        this.githubLogin = githubLogin;
        this.lastTestedAt = lastTestedAt;
        this.lastTestSucceeded = lastTestSucceeded;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public Boolean getConfigured() {
        return configured;
    }

    public void setConfigured(Boolean configured) {
        this.configured = configured;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGithubLogin() {
        return githubLogin;
    }

    public void setGithubLogin(String githubLogin) {
        this.githubLogin = githubLogin;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public void setLastTestedAt(Instant lastTestedAt) {
        this.lastTestedAt = lastTestedAt;
    }

    public Boolean getLastTestSucceeded() {
        return lastTestSucceeded;
    }

    public void setLastTestSucceeded(Boolean lastTestSucceeded) {
        this.lastTestSucceeded = lastTestSucceeded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long projectId;
        private String repositoryFullName;
        private Boolean configured;
        private String status;
        private String githubLogin;
        private Instant lastTestedAt;
        private Boolean lastTestSucceeded;

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder repositoryFullName(String repositoryFullName) {
            this.repositoryFullName = repositoryFullName;
            return this;
        }

        public Builder configured(Boolean configured) {
            this.configured = configured;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder githubLogin(String githubLogin) {
            this.githubLogin = githubLogin;
            return this;
        }

        public Builder lastTestedAt(Instant lastTestedAt) {
            this.lastTestedAt = lastTestedAt;
            return this;
        }

        public Builder lastTestSucceeded(Boolean lastTestSucceeded) {
            this.lastTestSucceeded = lastTestSucceeded;
            return this;
        }

        public GitHubConfigResponse build() {
            return new GitHubConfigResponse(projectId, repositoryFullName, configured, status, githubLogin, lastTestedAt, lastTestSucceeded);
        }
    }
}