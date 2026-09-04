package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GitHubRestClientTest {

    @Mock
    private GitHubHttpTransport transport;

    private GitHubRestClient client;
    private GitHubClientConfig config;

    @BeforeEach
    void setUp() {
        client = new GitHubRestClient(transport, new ObjectMapper());
        config = new GitHubClientConfig(
                "octocat", "Hello-World", "VERY_SECRET_TOKEN",
                "2026-03-10", Duration.ofSeconds(7));
    }

    @Test
    void requestContainsRequiredHeadersAndConfigDoesNotExposeToken() throws Exception {
        when(transport.get(eq("https://api.github.com/user"), any(), eq(Duration.ofSeconds(7))))
                .thenReturn(new GitHubHttpResponse(200,
                        "{\"id\":1,\"login\":\"octocat\"}", Map.of()));

        GitHubUser user = client.getAuthenticatedUser(config);

        assertThat(user.login()).isEqualTo("octocat");
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(transport).get(eq("https://api.github.com/user"), headers.capture(), eq(Duration.ofSeconds(7)));
        assertThat(headers.getValue())
                .containsEntry("Authorization", "Bearer VERY_SECRET_TOKEN")
                .containsEntry("Accept", "application/vnd.github+json")
                .containsEntry("X-GitHub-Api-Version", "2026-03-10")
                .containsEntry("User-Agent", "ProjectSupport-Backend");
        assertThat(headers.getValue().get("Authorization")).isEqualTo("Bearer VERY_SECRET_TOKEN");
        assertThat(config.toString()).doesNotContain("VERY_SECRET_TOKEN");
    }

    @Test
    void readsAuthenticatedUserAndRepository() throws Exception {
        when(transport.get(eq("https://api.github.com/user"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, "{\"id\":42,\"login\":\"dev01\"}", Map.of()));
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        {"id":123,"name":"Hello-World","full_name":"octocat/Hello-World",
                         "private":false,"default_branch":"main","html_url":"https://github.com/octocat/Hello-World","archived":false,
                         "permissions":{"admin":false,"maintain":false,"push":true,"triage":true,"pull":true}}
                        """, Map.of()));

        GitHubConnectionResult result = client.testConnection(config);

        assertThat(result.connected()).isTrue();
        assertThat(result.githubUserId()).isEqualTo(42L);
        assertThat(result.login()).isEqualTo("dev01");
        assertThat(result.githubRepositoryId()).isEqualTo(123L);
        assertThat(result.repositoryFullName()).isEqualTo("octocat/Hello-World");
        assertThat(result.permission()).isEqualTo("push");
    }


    @Test
    void mapsGitHubUserAvatarAndHtmlUrl() throws Exception {
        when(transport.get(eq("https://api.github.com/user"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        {"id":42,"login":"dev01","name":"Developer",
                         "avatar_url":"https://avatars.githubusercontent.com/u/42",
                         "html_url":"https://github.com/dev01"}
                        """, Map.of()));

        GitHubUser user = client.getAuthenticatedUser(config);

        assertThat(user.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/42");
        assertThat(user.htmlUrl()).isEqualTo("https://github.com/dev01");
    }

    @Test
    void mapsCommitStatsFilesParentsAndHtmlUrl() throws Exception {
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/commits?per_page=100&page=1"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        [{"sha":"abc123","html_url":"https://github.com/octocat/Hello-World/commit/abc123",
                          "commit":{"message":"CNPM-90 stats"},
                          "stats":{"additions":7,"deletions":3,"total":10},
                          "files":[{"filename":"A.java"},{"filename":"B.java"}],
                          "parents":[{"sha":"parent1"},{"sha":"parent2"}]}]
                        """, Map.of()));

        GitHubPage<GitHubCommit> page = client.getCommitsPage(config, 1);
        GitHubCommit commit = page.items().getFirst();

        assertThat(commit.additions()).isEqualTo(7);
        assertThat(commit.deletions()).isEqualTo(3);
        assertThat(commit.filesChanged()).isEqualTo(2);
        assertThat(commit.parentShas()).containsExactly("parent1", "parent2");
        assertThat(commit.htmlUrl()).isEqualTo("https://github.com/octocat/Hello-World/commit/abc123");
    }

    @Test
    void getsCompleteCommitDetailsBySha() throws Exception {
        String sha = "0123456789abcdef0123456789abcdef01234567";

        when(transport.get(
                eq("https://api.github.com/repos/octocat/Hello-World/commits/" + sha),
                any(),
                any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        {"sha":"0123456789abcdef0123456789abcdef01234567",
                        "html_url":"https://github.com/octocat/Hello-World/commit/0123456789abcdef0123456789abcdef01234567",
                        "commit":{"message":"complete",
                                  "author":{"name":"Alice","email":"alice@example.com","date":"2026-09-02T10:00:00Z"},
                                  "committer":{"name":"CI","email":"ci@example.com","date":"2026-09-02T10:05:00Z"}},
                        "stats":{"additions":10,"deletions":4,"total":14},
                        "files":[{"filename":"A.java"},{"filename":"B.java"}],
                        "parents":[{"sha":"parent1"}]}
                       """, Map.of()));

    GitHubCommit commit = client.getCommit(config, sha);

    assertThat(commit.sha()).isEqualTo(sha);
    assertThat(commit.additions()).isEqualTo(10);
    assertThat(commit.deletions()).isEqualTo(4);
    assertThat(commit.filesChanged()).isEqualTo(2);
    assertThat(commit.commit().committer().name()).isEqualTo("CI");
    assertThat(commit.commit().committer().date())
            .isEqualTo(Instant.parse("2026-09-02T10:05:00Z"));
}
    @Test
    void followsLinkHeaderPaginationForCommits() throws Exception {
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/commits?per_page=100&page=1"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        [{"sha":"aaa","commit":{"message":"CNPM-90 first"}}]
                        """, Map.of("link",
                        "<https://api.github.com/repos/octocat/Hello-World/commits?per_page=100&page=2>; rel=\"next\"")));
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/commits?per_page=100&page=2"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, "[{\"sha\":\"bbb\",\"commit\":{\"message\":\"CNPM-90 second\"}}]", Map.of()));

        List<GitHubCommit> commits = client.getAllCommits(config);

        assertThat(commits).extracting(GitHubCommit::sha).containsExactly("aaa", "bbb");
    }

    @Test
    void followsPaginationForPullRequestsAndMapsMergedState() throws Exception {
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/pulls?state=all&per_page=100&page=1"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, """
                        [{"id":9,"number":12,"title":"CNPM-90","state":"closed","merged_at":"2026-09-02T00:00:00Z",
                          "head":{"ref":"feature/CNPM-90","sha":"aaa"},"base":{"ref":"main","sha":"bbb"}}]
                        """, Map.of("link",
                        "<https://api.github.com/repos/octocat/Hello-World/pulls?state=all&per_page=100&page=2>; rel=\"next\"")));
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/pulls?state=all&per_page=100&page=2"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, "[]", Map.of()));

        List<GitHubPullRequest> pullRequests = client.getAllPullRequests(config, "all");

        assertThat(pullRequests).hasSize(1);
        assertThat(pullRequests.getFirst().localState()).isEqualTo("MERGED");
    }

    @Test
    void maps401ToAuthenticationErrorWithoutProviderBody() throws Exception {
        when(transport.get(any(), any(), any()))
                .thenReturn(new GitHubHttpResponse(401, "{\"message\":\"secret token leaked\"}", Map.of()));

        assertThatThrownBy(() -> client.getAuthenticatedUser(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> {
                    GitHubApiException exception = (GitHubApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getErrorCode()).isEqualTo("GITHUB_AUTHENTICATION_FAILED");
                    assertThat(exception.getMessage()).doesNotContain("secret token leaked");
                    assertThat(exception.getMessage()).doesNotContain("VERY_SECRET_TOKEN");
                });
    }

    @Test
    void maps403WithoutRateLimitToAuthorizationError() throws Exception {
        when(transport.get(any(), any(), any()))
                .thenReturn(new GitHubHttpResponse(403, "{\"message\":\"forbidden\"}", Map.of("x-ratelimit-remaining", "99")));

        assertThatThrownBy(() -> client.getAuthenticatedUser(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> {
                    GitHubApiException exception = (GitHubApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getErrorCode()).isEqualTo("GITHUB_AUTHORIZATION_FAILED");
                    assertThat(exception.isRetryable()).isFalse();
                });
    }

    @Test
    void maps403WithZeroRemainingToRateLimited() throws Exception {
        when(transport.get(any(), any(), any()))
                .thenReturn(new GitHubHttpResponse(403, "{\"message\":\"rate limit\"}",
                        Map.of("x-ratelimit-remaining", "0", "x-ratelimit-reset", String.valueOf(Instant.now().plusSeconds(60).getEpochSecond()))));

        assertThatThrownBy(() -> client.getAuthenticatedUser(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> {
                    GitHubApiException exception = (GitHubApiException) error;
                    assertThat(exception.getErrorCode()).isEqualTo("GITHUB_RATE_LIMITED");
                    assertThat(exception.isRetryable()).isTrue();
                    assertThat(exception.getRetryAfterSeconds()).isNotNull();
                });
    }

    @Test
    void maps429WithRetryAfter() throws Exception {
        when(transport.get(any(), any(), any()))
                .thenReturn(new GitHubHttpResponse(429, "{\"message\":\"too many\"}",
                        Map.of("retry-after", "17")));

        assertThatThrownBy(() -> client.getAuthenticatedUser(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> {
                    GitHubApiException exception = (GitHubApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getErrorCode()).isEqualTo("GITHUB_RATE_LIMITED");
                    assertThat(exception.getRetryAfterSeconds()).isEqualTo(17L);
                });
    }

    @Test
    void maps404And5xxToProviderSpecificSafeErrors() throws Exception {
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World"), any(), any()))
                .thenReturn(new GitHubHttpResponse(404, "{\"message\":\"not found\"}", Map.of()));
        assertThatThrownBy(() -> client.getRepository(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> assertThat(((GitHubApiException) error).getErrorCode())
                        .isEqualTo("GITHUB_REPOSITORY_NOT_FOUND"));

        when(transport.get(eq("https://api.github.com/user"), any(), any()))
                .thenReturn(new GitHubHttpResponse(503, "{\"message\":\"server error\"}", Map.of()));
        assertThatThrownBy(() -> client.getAuthenticatedUser(config))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(error -> {
                    GitHubApiException exception = (GitHubApiException) error;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(exception.getErrorCode()).isEqualTo("GITHUB_PROVIDER_UNAVAILABLE");
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    @Test
    void rejectsUntrustedPaginationUrl() throws Exception {
        when(transport.get(eq("https://api.github.com/repos/octocat/Hello-World/commits?per_page=100&page=1"), any(), any()))
                .thenReturn(new GitHubHttpResponse(200, "[]", Map.of(
                        "link", "<https://evil.example/steal?token=none>; rel=\"next\"")));

        assertThatThrownBy(() -> client.getAllCommits(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted");
    }
}
