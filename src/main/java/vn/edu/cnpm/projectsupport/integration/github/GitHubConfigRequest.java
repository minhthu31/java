package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GitHubConfigRequest {

    @NotBlank(message = "GitHub owner must not be blank")
    @Size(min = 1, max = 100, message = "Owner name length must be between 1 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Owner name contains invalid characters")
    private String repositoryOwner;

    @NotBlank(message = "GitHub repository name must not be blank")
    @Size(min = 1, max = 100, message = "Repository name length must be between 1 and 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$", message = "Repository name contains invalid characters")
    private String repositoryName;

    @Size(max = 2048, message = "Access token exceeds max length")
    private String accessToken;

    @Size(max = 50, message = "API version exceeds max length")
    private String apiVersion;

    public GitHubConfigRequest() {
    }

    public GitHubConfigRequest(String repositoryOwner, String repositoryName, String accessToken, String apiVersion) {
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.accessToken = accessToken;
        this.apiVersion = apiVersion != null ? apiVersion : "2026-03-10";
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRepositoryOwner() { return repositoryOwner; }
    public void setRepositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; }
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }

    public static class Builder {
        private String repositoryOwner;
        private String repositoryName;
        private String accessToken;
        private String apiVersion = "2026-03-10";

        public Builder repositoryOwner(String repositoryOwner) {
            this.repositoryOwner = repositoryOwner;
            return this;
        }

        public Builder repositoryName(String repositoryName) {
            this.repositoryName = repositoryName;
            return this;
        }

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public GitHubConfigRequest build() {
            return new GitHubConfigRequest(repositoryOwner, repositoryName, accessToken, apiVersion);
        }
    }
}