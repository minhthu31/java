package vn.edu.cnpm.projectsupport.integration.github;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.ObjectMapper;

/** Client for the GitHub REST endpoints required by CNPM-90. */
public class GitHubRestClient {

    private static final String BASE_URL = "https://api.github.com";
    private static final String ACCEPT = "application/vnd.github+json";
    private static final String USER_AGENT = "ProjectSupport-Backend";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern NEXT_LINK_PATTERN =
            Pattern.compile("<([^>]+)>\\s*;\\s*rel=\\\"next\\\"", Pattern.CASE_INSENSITIVE);

    private final GitHubHttpTransport transport;
    private final ObjectMapper objectMapper;

    public GitHubRestClient(GitHubHttpTransport transport, ObjectMapper objectMapper) {
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    public GitHubUser getAuthenticatedUser(GitHubClientConfig config) {
        return get(config, BASE_URL + "/user", GitHubUser.class);
    }

    public GitHubRepository getRepository(GitHubClientConfig config) {
        String path = "/repos/" + pathSegment(config.owner()) + "/" + pathSegment(config.repository());
        return get(config, BASE_URL + path, GitHubRepository.class);
    }

    public GitHubUser getUser(GitHubClientConfig config, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("GitHub username must not be blank");
        }
        String path = "/users/" + pathSegment(username.trim());
        return get(config, BASE_URL + path, GitHubUser.class);
    }

    /** Fetches the complete commit payload for a SHA, including stats and files. */
    public GitHubCommit getCommit(GitHubClientConfig config, String sha) {
        if (sha == null || sha.isBlank() || !sha.trim().matches("[0-9a-fA-F]{7,64}")) {
            throw new IllegalArgumentException("GitHub commit SHA is invalid");
        }
        String path = "/repos/" + pathSegment(config.owner()) + "/"
                + pathSegment(config.repository()) + "/commits/" + pathSegment(sha.trim());
        return get(config, BASE_URL + path, GitHubCommit.class);
    }

    public GitHubPage<GitHubCommit> getCommitsPage(GitHubClientConfig config, int page) {
        validatePage(page);
        String url = BASE_URL + "/repos/" + pathSegment(config.owner()) + "/"
                + pathSegment(config.repository()) + "/commits?per_page=" + MAX_PAGE_SIZE + "&page=" + page;
        return getPage(config, url, GitHubCommit[].class);
    }

    public GitHubPage<GitHubPullRequest> getPullRequestsPage(
            GitHubClientConfig config,
            String state,
            int page) {
        validatePage(page);
        String normalizedState = state == null || state.isBlank() ? "open" : state.trim().toLowerCase();
        if (!List.of("open", "closed", "all").contains(normalizedState)) {
            throw new IllegalArgumentException("GitHub pull request state must be open, closed or all");
        }
        String url = BASE_URL + "/repos/" + pathSegment(config.owner()) + "/"
                + pathSegment(config.repository()) + "/pulls?state=" + normalizedState
                + "&per_page=" + MAX_PAGE_SIZE + "&page=" + page;
        return getPage(config, url, GitHubPullRequest[].class);
    }

    /** Fetches every GitHub pagination page using the provider's Link rel=next header. */
    public List<GitHubCommit> getAllCommits(GitHubClientConfig config) {
        return collectPages(config, BASE_URL + "/repos/" + pathSegment(config.owner()) + "/"
                + pathSegment(config.repository()) + "/commits?per_page=" + MAX_PAGE_SIZE + "&page=1",
                GitHubCommit[].class);
    }

    /** Fetches every GitHub pagination page using the provider's Link rel=next header. */
    public List<GitHubPullRequest> getAllPullRequests(GitHubClientConfig config, String state) {
        String normalizedState = state == null || state.isBlank() ? "open" : state.trim().toLowerCase();
        if (!List.of("open", "closed", "all").contains(normalizedState)) {
            throw new IllegalArgumentException("GitHub pull request state must be open, closed or all");
        }
        String url = BASE_URL + "/repos/" + pathSegment(config.owner()) + "/"
                + pathSegment(config.repository()) + "/pulls?state=" + normalizedState
                + "&per_page=" + MAX_PAGE_SIZE + "&page=1";
        return collectPages(config, url, GitHubPullRequest[].class);
    }

    public GitHubConnectionResult testConnection(GitHubClientConfig config) {
        Instant testedAt = Instant.now();
        GitHubPageRateAccumulator rate = new GitHubPageRateAccumulator();
        GitHubHttpResponse userResponse = execute(config, BASE_URL + "/user");
        GitHubUser user = parse(userResponse, GitHubUser.class);
        rate.capture(userResponse);

        GitHubHttpResponse repositoryResponse = execute(config, BASE_URL + "/repos/"
                    + pathSegment(config.owner()) + "/" + pathSegment(config.repository()));
        GitHubRepository repository = parse(repositoryResponse, GitHubRepository.class);
        rate.capture(repositoryResponse);

        String permission = repository.permissions() == null
                ? null
                : repository.permissions().effectivePermission();

        return new GitHubConnectionResult(
                true,
                user.id(),
                user.login(),
                repository.id(),
                repository.fullName(),
                permission,
                rate.remaining,
                rate.resetAt,
                testedAt);
    }

    private <T> T get(GitHubClientConfig config, String url, Class<T> type) {
        return parse(execute(config, url), type);
    }

    private <T> GitHubPage<T> getPage(GitHubClientConfig config, String url, Class<T[]> arrayType) {
        GitHubHttpResponse response = execute(config, url);
        T[] array = parse(response, arrayType);
        return new GitHubPage<>(List.of(array),
                parseNextUrl(GitHubErrorHandler.header(response, "link")),
                rateLimitInfo(response));
    }

    private <T> List<T> collectPages(
            GitHubClientConfig config,
            String firstUrl,
            Class<T[]> arrayType) {
        List<T> result = new ArrayList<>();
        String nextUrl = firstUrl;
        int safetyPageCounter = 0;

        while (nextUrl != null) {
            if (++safetyPageCounter > 10_000) {
                throw new GitHubApiException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "GITHUB_PROVIDER_UNAVAILABLE",
                        false,
                        null,
                        "GitHub pagination exceeded the safety limit",
                        null);
            }
            GitHubHttpResponse response = execute(config, nextUrl);
            T[] page = parse(response, arrayType);
            for (T item : page) {
                result.add(item);
            }
            nextUrl = parseNextUrl(GitHubErrorHandler.header(response, "link"));
        }
        return List.copyOf(result);
    }

    private GitHubHttpResponse execute(GitHubClientConfig config, String url) {
        URI uri = URI.create(url);
        if (!BASE_URL.equalsIgnoreCase(uri.getScheme() + "://" + uri.getHost())) {
            throw new IllegalArgumentException("Only the official GitHub API origin is allowed");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + config.accessToken());
        headers.put("Accept", ACCEPT);
        headers.put("X-GitHub-Api-Version", config.apiVersion());
        headers.put("User-Agent", USER_AGENT);

        try {
            GitHubHttpResponse response = transport.get(url, headers, config.timeout());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw GitHubErrorHandler.fromResponse(response);
            }
            return response;
        } catch (GitHubApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw GitHubErrorHandler.fromThrowable(exception);
        } catch (IOException exception) {
            throw GitHubErrorHandler.fromThrowable(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof GitHubApiException githubApiException) {
                throw githubApiException;
            }
            throw exception;
        }
    }

    private <T> T parse(GitHubHttpResponse response, Class<T> type) {
        try {
            if (response.body() == null || response.body().isBlank()) {
                throw new IllegalStateException("GitHub returned an empty response");
            }
            return objectMapper.readValue(response.body(), type);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GitHubApiException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "GITHUB_PROVIDER_UNAVAILABLE",
                    false,
                    null,
                    "GitHub returned an invalid response",
                    exception);
        }
    }

    private static <T> GitHubRateLimitInfo rateLimitInfo(GitHubHttpResponse response) {
        String remaining = GitHubErrorHandler.header(response, "x-ratelimit-remaining");
        String reset = GitHubErrorHandler.header(response, "x-ratelimit-reset");
        Long remainingValue = parseLong(remaining);
        Instant resetAt = parseEpochInstant(reset);
        return new GitHubRateLimitInfo(remainingValue, resetAt);
    }

    private static String parseNextUrl(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return null;
        }
        Matcher matcher = NEXT_LINK_PATTERN.matcher(linkHeader);
        if (!matcher.find()) {
            return null;
        }
        String next = matcher.group(1);
        URI nextUri = URI.create(next);
        if (!"https".equalsIgnoreCase(nextUri.getScheme())
                || !"api.github.com".equalsIgnoreCase(nextUri.getHost())
                || nextUri.getUserInfo() != null
                || nextUri.getFragment() != null) {
            throw new IllegalArgumentException("GitHub pagination URL is not trusted");
        }
        return next;
    }

    private static String pathSegment(String value) {
        return value;
    }

    private static void validatePage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("GitHub page must be >= 1");
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Instant parseEpochInstant(String value) {
        Long epoch = parseLong(value);
        return epoch == null ? null : Instant.ofEpochSecond(epoch);
    }

    private static final class GitHubPageRateAccumulator {
        private Long remaining;
        private Instant resetAt;

        private void capture(GitHubHttpResponse response) {
            GitHubRateLimitInfo info = rateLimitInfo(response);
            if (info.remaining() != null) {
                remaining = info.remaining();
            }
            if (info.resetAt() != null) {
                resetAt = info.resetAt();
            }
        }
    }
}
