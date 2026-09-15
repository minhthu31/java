package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class GitHubPullRequestSyncServiceTest {

    @Mock GitHubRestClient client;
    @Mock GitHubPullRequestRepository pullRequestRepository;
    @Mock GitHubRepositoryRepository repositoryRepository;
    @Mock UserExternalAccountRepository externalAccountRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock GitHubIntegrationConfigRepository configRepository;
    @Mock IntegrationSecretService secretService;
    @Mock GitHubTaskLinkService taskLinkService;
    @Mock GitHubCommitSyncService commitSyncService;
    @Mock GitHubPullRequestCommitRepository pullRequestCommitRepository;

    private GitHubPullRequestSyncService service;
    private GitHubClientConfig config;
    private GitHubRepository localRepository;

    @BeforeEach
    void setUp() {
        service = new GitHubPullRequestSyncService(
                client,
                pullRequestRepository,
                repositoryRepository,
                externalAccountRepository,
                syncLogRepository,
                configRepository,
                secretService,
                taskLinkService,
                commitSyncService,
                pullRequestCommitRepository);
        config = new GitHubClientConfig(
                "octocat", "Hello-World", "token", "2026-03-10", Duration.ofSeconds(5));
        localRepository = mock(GitHubRepository.class);
        lenient().when(localRepository.getId()).thenReturn(20L);
        lenient().when(repositoryRepository.findByProjectIdAndGithubRepositoryId(1L, 123L))
                .thenReturn(Optional.of(localRepository));
        lenient().when(repositoryRepository.saveAndFlush(any(GitHubRepository.class))).thenReturn(localRepository);
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskLinkService.linkPullRequest(eq(1L), any()))
                .thenReturn(new GitHubTaskLinkResult(0, 0, 0, List.of()));
        lenient().when(client.getPullRequestCommitsPage(eq(config), anyInt(), eq(1)))
                .thenReturn(new GitHubPage<>(List.of(), null, null));
        lenient().when(pullRequestCommitRepository.findByPullRequestIdOrderByCommitOrderAsc(any()))
                .thenReturn(List.of());
    }

    @Test
    void syncsAllPagesUpsertsAndLinksPullRequests() {
        GitHubPullRequest first = pullRequest(10, "open", null);
        GitHubPullRequest second = pullRequest(11, "closed", Instant.parse("2026-09-08T12:00:00Z"));
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest persisted =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        when(persisted.getId()).thenReturn(30L);

        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getPullRequestsPage(config, "all", 1))
                .thenReturn(new GitHubPage<>(List.of(first), "https://api.github.com/next", null));
        when(client.getPullRequestsPage(config, "all", 2))
                .thenReturn(new GitHubPage<>(List.of(second), null, null));
        when(client.getPullRequest(config, 10)).thenReturn(first);
        when(client.getPullRequest(config, 11)).thenReturn(second);
        when(pullRequestRepository.findByRepositoryIdAndNumber(20L, 10)).thenReturn(Optional.empty());
        when(pullRequestRepository.findByRepositoryIdAndNumber(20L, 11)).thenReturn(Optional.empty());
        when(pullRequestRepository.saveAndFlush(any())).thenReturn(persisted);

        GitHubPullRequestSyncResult result = service.syncPullRequests(1L, config);

        assertThat(result.pullRequestsSynced()).isEqualTo(2);
        assertThat(result.errors()).isZero();
        verify(client).getPullRequestsPage(config, "all", 1);
        verify(client).getPullRequestsPage(config, "all", 2);
        verify(taskLinkService, times(2)).linkPullRequest(1L, persisted);

        ArgumentCaptor<vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest> captor =
                ArgumentCaptor.forClass(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        verify(pullRequestRepository, times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues().get(0).getState()).isEqualTo(GitHubPullRequestState.OPEN);
        assertThat(captor.getAllValues().get(1).getState()).isEqualTo(GitHubPullRequestState.MERGED);
        assertThat(captor.getAllValues().get(0).getHeadRef()).isEqualTo("feature/CNPM-101");
    }

    @Test
    void oneBadPullRequestIsRecordedAsPartialWithoutDiscardingOthers() {
        GitHubPullRequest valid = pullRequest(10, "open", null);
        GitHubPullRequest invalid = new GitHubPullRequest(
                11L, 11, null, null, null, null, null, "open", false,
                null, null, null, null, null, null, null, null, null, null);
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest persisted =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        when(persisted.getId()).thenReturn(30L);

        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getPullRequestsPage(config, "all", 1))
                .thenReturn(new GitHubPage<>(List.of(valid, invalid), null, null));
        when(client.getPullRequest(config, 10)).thenReturn(valid);
        when(client.getPullRequest(config, 11)).thenReturn(invalid);
        when(pullRequestRepository.findByRepositoryIdAndNumber(20L, 10)).thenReturn(Optional.empty());
        when(pullRequestRepository.saveAndFlush(any())).thenReturn(persisted);

        GitHubPullRequestSyncResult result = service.syncPullRequests(1L, config);

        assertThat(result.pullRequestsSynced()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues().getLast().getErrorCode()).isEqualTo("PARTIAL_SYNC");
    }

    @Test
    void failedSyncMasksCredentialsBeforeWritingSyncLog() {
        String token = "github_pat_" + "b".repeat(50);
        when(client.getRepository(config)).thenThrow(
                new RuntimeException("token=" + token + "; Authorization: Basic unsafe-basic-value"));

        assertThatThrownBy(() -> service.syncPullRequests(1L, config))
                .isInstanceOf(RuntimeException.class);

        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        String errorMessage = logCaptor.getAllValues().getLast().getErrorMessage();
        assertThat(errorMessage)
                .contains("[REDACTED]")
                .doesNotContain(token, "unsafe-basic-value");
    }

    @Test
    void mapsClosedPullRequestWithoutMergedTimestamp() {
        GitHubPullRequest closed = pullRequest(12, "closed", null);
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest persisted =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        when(persisted.getId()).thenReturn(31L);
        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getPullRequestsPage(config, "all", 1))
                .thenReturn(new GitHubPage<>(List.of(closed), null, null));
        when(client.getPullRequest(config, 12)).thenReturn(closed);
        when(pullRequestRepository.findByRepositoryIdAndNumber(20L, 12)).thenReturn(Optional.empty());
        when(pullRequestRepository.saveAndFlush(any())).thenReturn(persisted);

        GitHubPullRequestSyncResult result = service.syncPullRequests(1L, config);

        assertThat(result.errors()).isZero();
        ArgumentCaptor<vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest> captor =
                ArgumentCaptor.forClass(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        verify(pullRequestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(GitHubPullRequestState.CLOSED);
    }

    @Test
    void syncsPullRequestCommitPagesAndReusesExistingRelationOnRetry() {
        GitHubPullRequest pullRequest = pullRequest(15, "open", null);
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest persistedPullRequest =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit persistedCommit =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit.class);
        GitHubCommit listedCommit = commit("0123456789abcdef0123456789abcdef01234567");
        when(persistedPullRequest.getId()).thenReturn(35L);
        when(persistedCommit.getId()).thenReturn(45L);
        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getPullRequestsPage(config, "all", 1))
                .thenReturn(new GitHubPage<>(List.of(pullRequest), null, null));
        when(client.getPullRequest(config, 15)).thenReturn(pullRequest);
        when(pullRequestRepository.findByRepositoryIdAndNumber(20L, 15)).thenReturn(Optional.empty());
        when(pullRequestRepository.saveAndFlush(any())).thenReturn(persistedPullRequest);
        when(client.getPullRequestCommitsPage(config, 15, 1))
                .thenReturn(new GitHubPage<>(List.of(listedCommit), "https://api.github.com/next", null));
        when(client.getPullRequestCommitsPage(config, 15, 2))
                .thenReturn(new GitHubPage<>(List.of(), null, null));
        when(client.getCommit(config, listedCommit.sha())).thenReturn(listedCommit);
        when(commitSyncService.upsertCommit(20L, listedCommit)).thenReturn(persistedCommit);
        when(taskLinkService.linkCommit(1L, persistedCommit))
                .thenReturn(new GitHubTaskLinkResult(0, 0, 0, List.of()));

        service.syncPullRequests(1L, config);

        ArgumentCaptor<vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestCommit> relationCaptor =
                ArgumentCaptor.forClass(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestCommit.class);
        verify(pullRequestCommitRepository).saveAndFlush(relationCaptor.capture());
        var persistedRelation = relationCaptor.getValue();
        assertThat(persistedRelation.getPullRequestId()).isEqualTo(35L);
        assertThat(persistedRelation.getCommitId()).isEqualTo(45L);
        assertThat(persistedRelation.getCommitOrder()).isEqualTo(1);
        verify(client).getPullRequestCommitsPage(config, 15, 2);

        when(pullRequestCommitRepository.findByPullRequestIdOrderByCommitOrderAsc(35L))
                .thenReturn(List.of(persistedRelation));
        service.syncPullRequests(1L, config);

        verify(pullRequestCommitRepository, times(2)).saveAndFlush(persistedRelation);
    }

    private GitHubPullRequest pullRequest(int number, String state, Instant mergedAt) {
        return new GitHubPullRequest(
                1_000L + number,
                number,
                "CNPM-101 Pull Request " + number,
                "Liên kết CNPM-101",
                new GitHubUser(77L, "member", null, null, null, null),
                new GitHubPullRequest.Ref("feature/CNPM-101", "abc1234"),
                new GitHubPullRequest.Ref("main", "def5678"),
                state,
                false,
                mergedAt,
                mergedAt == null ? null : "merge123",
                2,
                10,
                2,
                3,
                "https://github.com/octocat/Hello-World/pull/" + number,
                Instant.parse("2026-09-08T10:00:00Z"),
                Instant.parse("2026-09-08T11:00:00Z"),
                mergedAt);
    }

    private GitHubCommit commit(String sha) {
        Instant committedAt = Instant.parse("2026-09-08T09:00:00Z");
        return new GitHubCommit(
                sha,
                new GitHubCommit.CommitMetadata(
                        "CNPM-95 sync commit",
                        new GitHubCommit.GitAuthor("Member", "member@example.com", committedAt),
                        new GitHubCommit.GitAuthor("Member", "member@example.com", committedAt)),
                new GitHubUser(77L, "member", null, null, null, null),
                "https://github.com/octocat/Hello-World/commit/" + sha,
                null,
                List.of(),
                List.of());
    }

    private vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepository() {
        return new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository(
                123L,
                "node",
                "Hello-World",
                "octocat/Hello-World",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository.Owner(999L, "octocat"),
                false,
                "main",
                "https://github.com/octocat/Hello-World",
                false,
                Instant.parse("2026-09-08T00:00:00Z"),
                null);
    }
}
