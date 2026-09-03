package vn.edu.cnpm.projectsupport.integration.github;

import java.util.List;

public record GitHubPage<T>(
        List<T> items,
        String nextUrl,
        GitHubRateLimitInfo rateLimit) {
}
