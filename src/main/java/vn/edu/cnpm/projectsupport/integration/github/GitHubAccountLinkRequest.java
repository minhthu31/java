package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GitHubAccountLinkRequest {

    @NotBlank(message = "externalAccountId must not be blank")
    @Pattern(regexp = "^[1-9][0-9]*$", message = "externalAccountId must be a positive GitHub user ID")
    private String externalAccountId;

    @NotBlank(message = "username must not be blank")
    @Size(min = 1, max = 100, message = "username length must be between 1 and 100 characters")
    private String username;

    public GitHubAccountLinkRequest() {
    }

    public GitHubAccountLinkRequest(String externalAccountId, String username) {
        this.externalAccountId = externalAccountId;
        this.username = username;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }
    public void setExternalAccountId(String externalAccountId) {
        this.externalAccountId = externalAccountId;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}
