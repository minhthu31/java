package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRun;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRunStatus;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCheckRunRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class GitHubCheckRunSyncServiceTest {

    @Mock
    private GitHubRestClient gitHubRestClient;

    @Mock
    private GitHubCheckRunRepository checkRunRepository;

    @Mock
    private GitHubRepositoryRepository repositoryRepository;

    @Mock
    private GitHubCommitRepository commitRepository;

    @Mock
    private GitHubPullRequestRepository pullRequestRepository;

    @Mock
    private GitHubIntegrationConfigRepository integrationConfigRepository;

    @Mock
    private IntegrationSecretService secretService;

    @Mock
    private SyncLogRepository syncLogRepository;

    private GitHubCheckRunSyncService service;
    private GitHubClientConfig config;
    private GitHubRepository localRepository;
    private vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepository;

    @BeforeEach
    void setUp() {
        service = new GitHubCheckRunSyncService(
                gitHubRestClient,
                checkRunRepository,
                repositoryRepository,
                commitRepository,
                pullRequestRepository,
                integrationConfigRepository,
                secretService,
                syncLogRepository);

        config = new GitHubClientConfig(
                "octocat",
                "Hello-World",
                "token",
                "2026-03-10",
                Duration.ofSeconds(5));

        localRepository = Mockito.mock(GitHubRepository.class);

        remoteRepository =
                new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository(
                        123L,
                        "node",
                        "Hello-World",
                        "octocat/Hello-World",
                        new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository.Owner(
                                42L,
                                "octocat"),
                        false,
                        "main",
                        "https://github.com/octocat/Hello-World",
                        false,
                        Instant.parse("2026-09-10T00:00:00Z"),
                        null);
    }

    private void stubSyncContext() {
        when(localRepository.getId())
                .thenReturn(11L);

        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(
                1L,
                123L))
                .thenReturn(Optional.of(localRepository));

        when(repositoryRepository.saveAndFlush(localRepository))
                .thenReturn(localRepository);

        when(commitRepository.findByRepositoryIdOrderByCommittedAtDesc(
                eq(11L),
                any()))
                .thenReturn(new PageImpl<>(List.of()));

        when(pullRequestRepository.findByRepositoryIdOrderByRemoteCreatedAtDesc(
                eq(11L),
                any()))
                .thenReturn(new PageImpl<>(List.of()));

        when(syncLogRepository.save(any(SyncLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(gitHubRestClient.getRepository(config))
                .thenReturn(remoteRepository);
    }

    @Test
    void syncsCheckRunsAndLinksCommitHeadToPullRequest() {
        stubSyncContext();

        GitHubCheckRunResponse response =
                new GitHubCheckRunResponse(
                        9001L,
                        "build",
                        "completed",
                        "success",
                        "abc1234",
                        "https://github.com/octocat/Hello-World/runs/9001",
                        Instant.parse("2026-09-10T10:00:00Z"),
                        Instant.parse("2026-09-10T10:02:00Z"));

        when(gitHubRestClient.getCheckRunsPage(
                config,
                "main",
                1))
                .thenReturn(new GitHubPage<>(
                        List.of(response),
                        null,
                        null));

        when(checkRunRepository.findByRepositoryIdAndExternalId(
                11L,
                9001L))
                .thenReturn(Optional.empty());

        when(checkRunRepository.save(any(GitHubCheckRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GitHubCheckRunSyncResult result =
                service.syncCheckRuns(1L, config);

        assertThat(result.synced())
                .isEqualTo(1);

        assertThat(result.created())
                .isEqualTo(1);

        assertThat(result.updated())
                .isZero();

        assertThat(result.errors())
                .isZero();

        assertThat(result.actionsEnabled())
                .isTrue();

        var captor =
                org.mockito.ArgumentCaptor.forClass(
                        GitHubCheckRun.class);

        verify(checkRunRepository)
                .save(captor.capture());

        assertThat(captor.getValue().getExternalId())
                .isEqualTo(9001L);

        assertThat(captor.getValue().getCommitSha())
                .isEqualTo("abc1234");

        assertThat(captor.getValue().getStatus())
                .isEqualTo(GitHubCheckRunStatus.SUCCESS);

        assertThat(captor.getValue().getPullRequestId())
                .isNull();
    }

    @Test
    void rerunningSameCheckRunUpdatesInsteadOfCreatingDuplicate() {
        stubSyncContext();

        GitHubCheckRun existing =
                new GitHubCheckRun(
                        11L,
                        9001L,
                        "abc1234",
                        "build",
                        GitHubCheckRunStatus.PENDING,
                        "old-url",
                        null,
                        null);

        GitHubCheckRunResponse response =
                new GitHubCheckRunResponse(
                        9001L,
                        "build",
                        "completed",
                        "failure",
                        "abc1234",
                        "new-url",
                        null,
                        Instant.parse("2026-09-10T10:02:00Z"));

        when(gitHubRestClient.getCheckRunsPage(
                config,
                "main",
                1))
                .thenReturn(new GitHubPage<>(
                        List.of(response),
                        null,
                        null));

        when(checkRunRepository.findByRepositoryIdAndExternalId(
                11L,
                9001L))
                .thenReturn(Optional.of(existing));

        GitHubCheckRunSyncResult result =
                service.syncCheckRuns(1L, config);

        assertThat(result.created())
                .isZero();

        assertThat(result.updated())
                .isEqualTo(1);

        assertThat(existing.getStatus())
                .isEqualTo(GitHubCheckRunStatus.FAILURE);

        assertThat(existing.getHtmlUrl())
                .isEqualTo("new-url");

        verify(checkRunRepository, never())
                .save(any(GitHubCheckRun.class));
    }

    @Test
    void actionsDisabledIsReportedWithoutFailingSync() {
        stubSyncContext();

        when(gitHubRestClient.getCheckRunsPage(
                config,
                "main",
                1))
                .thenThrow(new GitHubApiException(
                        HttpStatus.CONFLICT,
                        "GITHUB_ACTIONS_DISABLED",
                        false,
                        null,
                        "GitHub Actions/check runs are disabled for this repository",
                        null));

        GitHubCheckRunSyncResult result =
                service.syncCheckRuns(1L, config);

        assertThat(result.actionsEnabled())
                .isFalse();

        assertThat(result.synced())
                .isZero();

        assertThat(result.errors())
                .isZero();

        verify(syncLogRepository, Mockito.atLeastOnce())
                .save(any(SyncLog.class));
    }

    @Test
    void statusMappingCoversPendingSuccessFailureAndCancelled() {
        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "queued",
                        null))
                .isEqualTo(GitHubCheckRunStatus.PENDING);

        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "completed",
                        "success"))
                .isEqualTo(GitHubCheckRunStatus.SUCCESS);

        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "completed",
                        "failure"))
                .isEqualTo(GitHubCheckRunStatus.FAILURE);

        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "completed",
                        "cancelled"))
                .isEqualTo(GitHubCheckRunStatus.CANCELLED);

        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "completed",
                        "skipped"))
                .isEqualTo(GitHubCheckRunStatus.SKIPPED);

        assertThat(
                GitHubCheckRunSyncService.mapStatus(
                        "completed",
                        "neutral"))
                .isEqualTo(GitHubCheckRunStatus.NEUTRAL);
    }

    @Test
    void listCheckRunsUsesInternalRepositoryId() {
        GitHubRepository repository =
                Mockito.mock(GitHubRepository.class);

        when(repository.getId())
                .thenReturn(11L);

        when(repository.getProjectId())
                .thenReturn(1L);

        GitHubCheckRun checkRun =
                Mockito.mock(GitHubCheckRun.class);

        when(repositoryRepository.findById(11L))
                .thenReturn(Optional.of(repository));

        when(checkRunRepository.findByRepositoryIdOrderByCompletedAtDesc(11L))
                .thenReturn(List.of(checkRun));

        List<GitHubCheckRun> result =
                service.listCheckRuns(1L, 11L);

        assertThat(result)
                .containsExactly(checkRun);

        verify(repositoryRepository)
                .findById(11L);

        verify(checkRunRepository)
                .findByRepositoryIdOrderByCompletedAtDesc(11L);

        verify(repositoryRepository, never())
                .findByProjectIdAndGithubRepositoryId(1L, 11L);
    }

    @Test
    void listCheckRunsRejectsRepositoryFromAnotherProject() {
        GitHubRepository repository =
            Mockito.mock(GitHubRepository.class);

        when(repository.getProjectId())
              .thenReturn(2L);

        when(repositoryRepository.findById(11L))
              .thenReturn(Optional.of(repository));

        org.assertj.core.api.Assertions.assertThatThrownBy(
               () -> service.listCheckRuns(1L, 11L))
               .isInstanceOf(IllegalArgumentException.class)
               .hasMessage(
                      "GitHub repository does not belong to project");

        verify(repositoryRepository)
                .findById(11L);

        verify(checkRunRepository, never())
                .findByRepositoryIdOrderByCompletedAtDesc(any());
  }
}