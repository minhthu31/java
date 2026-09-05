package vn.edu.cnpm.projectsupport.integration.github;

import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public final class GitHubErrorHandler {

    private GitHubErrorHandler() {
    }

    public static GitHubApiException fromResponse(GitHubHttpResponse response) {
        int status = response.statusCode();
        String retryAfter = header(response, "retry-after");
        String remaining = header(response, "x-ratelimit-remaining");
        String reset = header(response, "x-ratelimit-reset");

        if (status == 400 || status == 422) {
            return failure(HttpStatus.BAD_REQUEST, "GITHUB_CONFIG_INVALID", false, null,
                    "GitHub request is invalid", null);
        }
        if (status == 401) {
            return failure(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", false, null,
                    "GitHub authentication failed", null);
        }
        if (status == 403 && isRateLimited(retryAfter, remaining)) {
            return failure(HttpStatus.TOO_MANY_REQUESTS, "GITHUB_RATE_LIMITED", true,
                    retryAfterSeconds(retryAfter, reset), "GitHub rate limit exceeded", null);
        }
        if (status == 403) {
            return failure(HttpStatus.FORBIDDEN, "GITHUB_AUTHORIZATION_FAILED", false, null,
                    "GitHub authorization failed", null);
        }
        if (status == 404) {
            return failure(HttpStatus.NOT_FOUND, "GITHUB_REPOSITORY_NOT_FOUND", false, null,
                    "GitHub repository was not found or is not visible", null);
        }
        if (status == 429) {
            return failure(HttpStatus.TOO_MANY_REQUESTS, "GITHUB_RATE_LIMITED", true,
                    retryAfterSeconds(retryAfter, reset), "GitHub rate limit exceeded", null);
        }
        if (status >= 500) {
            return failure(HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE", true, null,
                    "GitHub is temporarily unavailable", null);
        }
        return failure(HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE", false, null,
                "GitHub API request failed", null);
    }

    public static GitHubApiException fromThrowable(Throwable cause) {
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return failure(HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE", true, null,
                    "GitHub provider request was interrupted", cause);
        }
        if (cause instanceof IOException || cause instanceof java.net.http.HttpTimeoutException) {
            return failure(HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE", true, null,
                    "GitHub provider is unavailable", cause);
        }
        return failure(HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE", false, null,
                "GitHub API request failed", cause);
    }

    static Long retryAfterSeconds(String retryAfter, String resetEpochSeconds) {
        if (retryAfter != null && !retryAfter.isBlank()) {
            try {
                long value = Long.parseLong(retryAfter.trim());
                return value >= 0 ? value : null;
            } catch (NumberFormatException ignored) {
                // Fall through to X-RateLimit-Reset.
            }
        }
        if (resetEpochSeconds != null && !resetEpochSeconds.isBlank()) {
            try {
                long reset = Long.parseLong(resetEpochSeconds.trim());
                long now = Instant.now().getEpochSecond();
                return Math.max(0L, reset - now);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean isRateLimited(String retryAfter, String remaining) {
        if (retryAfter != null && !retryAfter.isBlank()) {
            return true;
        }
        if (remaining == null || remaining.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(remaining.trim()) == 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static String header(GitHubHttpResponse response, String name) {
        if (response.headers() == null) {
            return null;
        }
        return response.headers().entrySet().stream()
                .filter(entry -> entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(name))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static GitHubApiException failure(
            HttpStatus status,
            String code,
            boolean retryable,
            Long retryAfterSeconds,
            String message,
            Throwable cause) {
        return new GitHubApiException(status, code, retryable, retryAfterSeconds, message, cause);
    }
}
