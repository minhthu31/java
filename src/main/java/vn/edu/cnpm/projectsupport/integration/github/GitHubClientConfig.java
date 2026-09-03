package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Duration;

/** Per-request GitHub client configuration. The token is deliberately kept out of DTO serialization/toString. */
public final class GitHubClientConfig {

    public static final String DEFAULT_API_VERSION = "2026-03-10";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String owner;
    private final String repository;
    private final String accessToken;
    private final String apiVersion;
    private final Duration timeout;

    public GitHubClientConfig(
            String owner,
            String repository,
            String accessToken,
            String apiVersion,
            Duration timeout) {
        this.owner = requireSegment(owner, "GitHub owner");
        this.repository = requireSegment(repository, "GitHub repository");
        this.accessToken = requireToken(accessToken);
        this.apiVersion = apiVersion == null || apiVersion.isBlank()
                ? DEFAULT_API_VERSION
                : apiVersion.trim();
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        if (this.timeout.isZero() || this.timeout.isNegative()) {
            throw new IllegalArgumentException("GitHub timeout must be positive");
        }
    }

    public String owner() {
        return owner;
    }

    public String repository() {
        return repository;
    }

    public String accessToken() {
        return accessToken;
    }

    public String apiVersion() {
        return apiVersion;
    }

    public Duration timeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return "GitHubClientConfig{" +
                "owner='" + owner + '\'' +
                ", repository='" + repository + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", timeout=" + timeout +
                ", accessToken='<redacted>'}";
    }

    private static String requireSegment(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (!value.trim().matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(field + " contains invalid characters");
        }
        return value.trim();
    }

    private static String requireToken(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitHub access token must not be blank");
        }
        return value;
    }
}
