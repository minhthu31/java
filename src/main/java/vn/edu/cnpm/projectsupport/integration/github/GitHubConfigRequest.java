package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GitHubConfigRequest {

    @NotBlank(message = "repositoryOwner must not be blank")
    @Size(min = 1, max = 100, message = "repositoryOwner length must be between 1 and 100")
    private String repositoryOwner;

    @NotBlank(message = "repositoryName must not be blank")
    @Size(min = 1, max = 100, message = "repositoryName length must be between 1 and 100")
    private String repositoryName;

    @Size(min = 1, max = 2048, message = "accessToken length must be between 1 and 2048")
    private String accessToken;

    private String apiVersion = "2026-03-10";

    public GitHubConfigRequest() {
    }

    public GitHubConfigRequest(String repositoryOwner, String repositoryName, String accessToken, String apiVersion) {
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.accessToken = accessToken;
        this.apiVersion = apiVersion != null ? apiVersion : "2026-03-10";
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public void setRepositoryOwner(String repositoryOwner) {
        this.repositoryOwner = repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

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