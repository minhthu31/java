package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class GitHubCommitSyncServiceTest {

    @Mock GitHubRestClient client;
    @Mock GitHubCommitRepository commitRepository;
    @Mock GitHubRepositoryRepository repositoryRepository;
    @Mock UserExternalAccountRepository externalAccountRepository;
    @Mock SyncLogRepository syncLogRepository;
    @Mock GitHubIntegrationConfigRepository integrationConfigRepository;
    @Mock IntegrationSecretService secretService;
    @Mock GitHubTaskLinkService taskLinkService;

    private GitHubCommitSyncService service;
    private GitHubClientConfig config;
    private GitHubRepository localRepository;

    @BeforeEach
    void setUp() {
        service = new GitHubCommitSyncService(
                client, commitRepository, repositoryRepository, externalAccountRepository,
                syncLogRepository, null, null, taskLinkService);
        config = new GitHubClientConfig(
                "octocat", "Hello-World", "token", "2026-03-10", Duration.ofSeconds(5));
        localRepository = mock(GitHubRepository.class);
        when(localRepository.getId()).thenReturn(20L);
        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(1L, 123L))
                .thenReturn(Optional.of(localRepository));
        when(syncLogRepository.save(any(SyncLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void syncsAllPagesAndMapsCommitFields() {
        Instant authorDate = Instant.parse("2026-09-02T10:00:00Z");
        Instant committerDate = Instant.parse("2026-09-02T10:05:00Z");
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor author = new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor("Alice", "alice@example.com", authorDate);
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor committer = new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor("CI", "ci@example.com", committerDate);
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remote = new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit(
                "sha-1",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.CommitMetadata("CNPM-94 import", author, committer),
                new GitHubUser(77L, "alice", "Alice", null, "https://github.com/alice", null),
                "https://github.com/octocat/Hello-World/commit/sha-1",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.Stats(5, 2, 7),
                List.of(new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.CommitFile("f", "A.java", "modified", 5, 2, 7)),
                List.of(new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.Parent("parent")));

        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getCommitsPage(config, 1)).thenReturn(new GitHubPage<>(List.of(remote),
                "https://api.github.com/repos/octocat/Hello-World/commits?page=2", null));
        when(client.getCommitsPage(config, 2)).thenReturn(new GitHubPage<>(List.of(), null, null));
        when(client.getCommit(config, "sha-1")).thenReturn(remote);
        when(commitRepository.findByRepositoryIdAndSha(20L, "sha-1")).thenReturn(Optional.empty());
        when(commitRepository.saveAndFlush(any(GitHubCommit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repositoryRepository.saveAndFlush(any(GitHubRepository.class)))
                .thenReturn(localRepository);

        GitHubCommitSyncResult result = service.syncCommits(1L, config);

        assertThat(result.commitsSynced()).isEqualTo(1);
        assertThat(result.errors()).isZero();
        ArgumentCaptor<GitHubCommit> captor = ArgumentCaptor.forClass(GitHubCommit.class);
        verify(commitRepository).saveAndFlush(captor.capture());
        GitHubCommit saved = captor.getValue();
        assertThat(saved.getSha()).isEqualTo("sha-1");
        assertThat(saved.getMessage()).isEqualTo("CNPM-94 import");
        assertThat(saved.getGitAuthorName()).isEqualTo("Alice");
        assertThat(saved.getGitAuthorEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getGitCommitterName()).isEqualTo("CI");
        assertThat(saved.getGitCommitterEmail()).isEqualTo("ci@example.com");
        assertThat(saved.getCommitterAt()).isEqualTo(committerDate);
        verify(taskLinkService).linkCommit(1L, saved);
        verify(client).getCommitsPage(config, 1);
        verify(client).getCommitsPage(config, 2);
    }

    @Test
    void existingShaIsUpdatedInsteadOfInserted() {
        GitHubCommit existing = new GitHubCommit(20L, "sha-1", "old", Instant.parse("2026-09-01T00:00:00Z"),
                "https://github.com/old");
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remote = new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit(
                "sha-1",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.CommitMetadata("new message", new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor("A", "a@e", Instant.parse("2026-09-02T00:00:00Z")), null),
                new GitHubUser(77L, "alice", null, null, null, null),
                "https://github.com/new", null, null, List.of());
        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getCommitsPage(config, 1)).thenReturn(new GitHubPage<>(List.of(remote), null, null));
        when(client.getCommit(config, "sha-1")).thenReturn(remote);
        when(commitRepository.findByRepositoryIdAndSha(20L, "sha-1")).thenReturn(Optional.of(existing));
        when(commitRepository.saveAndFlush(existing)).thenReturn(existing);
        when(repositoryRepository.saveAndFlush(any(GitHubRepository.class)))
                .thenReturn(localRepository);

        GitHubCommitSyncResult result = service.syncCommits(1L, config);

        assertThat(result.commitsSynced()).isEqualTo(1);
        assertThat(existing.getMessage()).isEqualTo("new message");
        assertThat(existing.getHtmlUrl()).isEqualTo("https://github.com/new");
        verify(commitRepository).saveAndFlush(existing);
    }

    @Test
    void oneBadCommitDoesNotDiscardOtherCommits() {
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit first = remoteCommit("first");
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit second = remoteCommit("second");
        when(client.getRepository(config)).thenReturn(remoteRepository());
        when(client.getCommitsPage(config, 1)).thenReturn(new GitHubPage<>(List.of(first, second), null, null));
        when(client.getCommit(config, "first")).thenReturn(first);
        when(client.getCommit(config, "second")).thenReturn(second);
        when(commitRepository.findByRepositoryIdAndSha(20L, "first")).thenReturn(Optional.empty());
        when(commitRepository.findByRepositoryIdAndSha(20L, "second")).thenReturn(Optional.empty());
        when(commitRepository.saveAndFlush(any(GitHubCommit.class)))
                .thenAnswer(invocation -> {
                    GitHubCommit value = invocation.getArgument(0);
                    if ("second".equals(value.getSha())) {
                        throw new RuntimeException("bad commit");
                    }
                    return value;
                });
        when(repositoryRepository.saveAndFlush(any(GitHubRepository.class)))
                .thenReturn(localRepository);

        GitHubCommitSyncResult result = service.syncCommits(1L, config);

        assertThat(result.commitsSynced()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
        verify(commitRepository, times(2)).saveAndFlush(any(GitHubCommit.class));
        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues().getLast().getStatus().name()).isEqualTo("FAILED");
    }

    @Test
    void usesProjectIntegrationConfigRepositoryInsteadOfFirstLocalRepository() {
        IntegrationConfig integrationConfig =
                new IntegrationConfig(1L, IntegrationProvider.GITHUB, "encrypted-token");
        integrationConfig.setAccountIdentifier("configured-owner/configured-repository");
        when(integrationConfigRepository.findGitHubConfigByProjectId(1L))
                .thenReturn(Optional.of(integrationConfig));
        when(secretService.decrypt("encrypted-token")).thenReturn("token");

        when(client.getRepository(any(GitHubClientConfig.class))).thenReturn(remoteRepository());
        when(client.getCommitsPage(any(GitHubClientConfig.class), eq(1)))
                .thenReturn(new GitHubPage<>(List.of(), null, null));
        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(1L, 123L))
                .thenReturn(Optional.of(localRepository));
        when(repositoryRepository.saveAndFlush(any(GitHubRepository.class)))
                .thenReturn(localRepository);

        serviceWithConfigRepositories().syncCommits(1L);

        ArgumentCaptor<GitHubClientConfig> configCaptor = ArgumentCaptor.forClass(GitHubClientConfig.class);
        verify(client).getRepository(configCaptor.capture());
        assertThat(configCaptor.getValue().owner()).isEqualTo("configured-owner");
        assertThat(configCaptor.getValue().repository()).isEqualTo("configured-repository");
        verify(repositoryRepository, never()).findByProjectIdOrderByFullNameAsc(any(), any());
    }

    private GitHubCommitSyncService serviceWithConfigRepositories() {
        return new GitHubCommitSyncService(
                client, commitRepository, repositoryRepository, externalAccountRepository,
                syncLogRepository, integrationConfigRepository, secretService, taskLinkService);
    }

    private vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepository() {
        return new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository(
                123L, "node", "Hello-World", "octocat/Hello-World",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository.Owner(999L, "octocat"), false, "main",
                "https://github.com/octocat/Hello-World", false, Instant.parse("2026-09-02T00:00:00Z"),
                null);
    }

    private vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remoteCommit(String sha) {
        return new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit(
                sha,
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.CommitMetadata("message", new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit.GitAuthor("A", "a@e", Instant.now()), null),
                null,
                "https://github.com/octocat/Hello-World/commit/" + sha,
                null, null, List.of());
    }
}
