package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommit(
        String sha,
        CommitMetadata commit,
        GitHubUser author,
        String htmlUrl,
        Integer additions,
        Integer deletions,
        Integer changedFiles) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitMetadata(
            String message,
            GitAuthor author) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitAuthor(
            String name,
            String email,
            Instant date) {
    }
}
