package vn.edu.cnpm.projectsupport.integration.jira.contract;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JiraConnectionRequest(
        @NotBlank
        @Size(max = 500)
        @Pattern(
                regexp = "^https://[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?/?$",
                message = "Jira site URL phải là HTTPS origin, không chứa path, query hoặc user-info")
        String siteUrl,
        @NotBlank
        @Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}", message = "Jira project key không hợp lệ")
        String projectKey,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 2048) String apiToken,
        @NotNull JiraAuthType authType) {
}
