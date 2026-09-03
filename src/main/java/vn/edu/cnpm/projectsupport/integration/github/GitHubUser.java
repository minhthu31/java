package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUser(
        Long id,
        String login,
        String name,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("html_url") String htmlUrl,
        String email) {
}
