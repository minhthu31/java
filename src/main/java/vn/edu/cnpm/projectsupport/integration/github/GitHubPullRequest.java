package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequest(
        Long id,
        Integer number,
        String title,
        String body,
        GitHubUser user,
        Ref head,
        Ref base,
        String state,
        Boolean draft,
        @JsonProperty("merged_at") Instant mergedAt,
        @JsonProperty("merge_commit_sha") String mergeCommitSha,
        Integer commits,
        Integer additions,
        Integer deletions,
        @JsonProperty("changed_files") Integer changedFiles,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("closed_at") Instant closedAt) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ref(String ref, String sha) {
    }

    public String localState() {
        if (mergedAt != null) {
            return "MERGED";
        }
        return "closed".equalsIgnoreCase(state) ? "CLOSED" : "OPEN";
    }
}
