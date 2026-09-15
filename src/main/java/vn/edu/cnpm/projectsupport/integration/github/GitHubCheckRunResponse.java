package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCheckRunResponse(
        Long id,
        String name,
        String status,
        String conclusion,
        @JsonProperty("head_sha") String headSha,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("completed_at") Instant completedAt) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubCheckRunsPageResponse(
        @JsonProperty("total_count") Integer totalCount,
        @JsonProperty("check_runs") List<GitHubCheckRunResponse> checkRuns) {
}
