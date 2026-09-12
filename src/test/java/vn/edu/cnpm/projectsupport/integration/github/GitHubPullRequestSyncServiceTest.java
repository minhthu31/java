package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
                taskLinkService);
        config = new GitHubClientConfig(
                "octocat", "Hello-World", "token", "2026-03-10", Duration.ofSeconds(5));
        localRepository = mock(GitHubRepository.class);
        when(localRepository.getId()).thenReturn(20L);
        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(1L, 123L))
                .thenReturn(Optional.of(localRepository));
        when(repositoryRepository.saveAndFlush(any(GitHubRepository.class))).thenReturn(localRepository);
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskLinkService.linkPullRequest(eq(1L), any()))
                .thenReturn(new GitHubTaskLinkResult(0, 0, 0, List.of()));
    }

    @Test
    void syncsAllPagesUpsertsAndLinksPullRequests() {
        GitHubPullRequest first = pullRequest(10, "open", null);
        GitHubPullRequest second = pullRequest(11, "closed", Instant.parse("2026-09-08T12:00:00Z"));
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest persisted =
                mock(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest.class);

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
