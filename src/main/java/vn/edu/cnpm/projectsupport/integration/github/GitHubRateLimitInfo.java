package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

public record GitHubRateLimitInfo(
        Long remaining,
        Instant resetAt) {
}
