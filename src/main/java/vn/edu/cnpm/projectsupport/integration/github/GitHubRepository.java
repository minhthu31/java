package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepository(
        Long id,
        @JsonProperty("node_id") String nodeId,
        String name,
        @JsonProperty("full_name") String fullName,
        Owner owner,
        @JsonProperty("private") boolean privateRepository,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("html_url") String htmlUrl,
        boolean archived,
        @JsonProperty("updated_at") Instant updatedAt) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(Long id, String login) {
    }
}
