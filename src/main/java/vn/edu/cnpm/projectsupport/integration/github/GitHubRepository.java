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
        @JsonProperty("updated_at") Instant updatedAt,
        Permissions permissions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(Long id, String login) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Permissions(
            Boolean admin,
            Boolean maintain,
            Boolean push,
            Boolean triage,
            Boolean pull) {

        /**
         * Returns the highest effective repository permission represented by GitHub.
         */
        public String effectivePermission() {
            if (Boolean.TRUE.equals(admin)) {
                return "admin";
            }
            if (Boolean.TRUE.equals(maintain)) {
                return "maintain";
            }
            if (Boolean.TRUE.equals(push)) {
                return "push";
            }
            if (Boolean.TRUE.equals(triage)) {
                return "triage";
            }
            if (Boolean.TRUE.equals(pull)) {
                return "pull";
            }
            return null;
        }
    }
}
