package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUser(
        Long id,
        String login,
        String name,
        String avatarUrl,
        String htmlUrl,
        String email) {
}
