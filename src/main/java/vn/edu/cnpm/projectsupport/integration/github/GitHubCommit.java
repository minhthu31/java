package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommit(
        String sha,
        CommitMetadata commit,
        GitHubUser author,
        @JsonProperty("html_url") String htmlUrl,
        Stats stats,
        List<CommitFile> files,
        List<Parent> parents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitMetadata(
            String message,
            GitAuthor author,
            GitAuthor committer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitAuthor(
            String name,
            String email,
            Instant date) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stats(
            Integer additions,
            Integer deletions,
            Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitFile(
            String sha,
            String filename,
            String status,
            Integer additions,
            Integer deletions,
            Integer changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Parent(String sha) {
    }

    /** Number of added lines reported by the commit stats object. */
    public Integer additions() {
        return stats == null ? 0 : defaultZero(stats.additions());
    }

    /** Number of deleted lines reported by the commit stats object. */
    public Integer deletions() {
        return stats == null ? 0 : defaultZero(stats.deletions());
    }

    /** Number of files reported by the commit files array. */
    public Integer filesChanged() {
        return files == null ? null : files.size();
    }

    /** SHA values from the commit parents array. */
    public List<String> parentShas() {
        if (parents == null || parents.isEmpty()) {
            return List.of();
        }
        return parents.stream()
                .map(Parent::sha)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
