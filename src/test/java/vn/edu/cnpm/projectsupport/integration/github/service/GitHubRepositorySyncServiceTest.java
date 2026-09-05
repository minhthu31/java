package vn.edu.cnpm.projectsupport.integration.github.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import vn.edu.cnpm.projectsupport.integration.github.GitHubApiException;
import vn.edu.cnpm.projectsupport.integration.github.GitHubRestClient;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class GitHubRepositorySyncServiceTest {

    private static final Long PROJECT_ID = 93L;
    private static final Long GITHUB_REPOSITORY_ID = 123456L;

    @Mock private ProjectRepository projectRepository;
    @Mock private GitHubIntegrationConfigRepository configRepository;
    @Mock private GitHubRepositoryRepository repositoryRepository;
    @Mock private SyncLogRepository syncLogRepository;
    @Mock private GitHubRestClient gitHubRestClient;
    @Mock private IntegrationSecretService secretService;

    private GitHubRepositorySyncService service;
    private IntegrationConfig integrationConfig;

    @BeforeEach
    void setUp() {
        service = new GitHubRepositorySyncService(projectRepository, configRepository, repositoryRepository, syncLogRepository, gitHubRestClient, secretService);
        integrationConfig = new IntegrationConfig(PROJECT_ID, IntegrationProvider.GITHUB, "encrypted-token");
        integrationConfig.setAccountIdentifier("octocat/Hello-World");
    }

    @Test
    void firstSyncCreatesRepositorySnapshotAndSuccessLog() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(configRepository.findGitHubConfigByProjectId(PROJECT_ID)).thenReturn(Optional.of(integrationConfig));
        when(secretService.decrypt("encrypted-token")).thenReturn("github-token");
        when(gitHubRestClient.getRepository(any())).thenReturn(remoteRepository("main"));
        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(PROJECT_ID, GITHUB_REPOSITORY_ID)).thenReturn(Optional.empty());
        when(repositoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GitHubRepositorySyncResult result = service.syncRepository(PROJECT_ID);

        ArgumentCaptor<vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository> repositoryCaptor =
                ArgumentCaptor.forClass(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository.class);
        verify(repositoryRepository).save(repositoryCaptor.capture());
        var saved = repositoryCaptor.getValue();

        assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(saved.getGithubRepositoryId()).isEqualTo(GITHUB_REPOSITORY_ID);
        assertThat(saved.getOwnerGithubUserId()).isEqualTo(1L);
        assertThat(saved.getOwnerLogin()).isEqualTo("octocat");
        assertThat(saved.getName()).isEqualTo("Hello-World");
        assertThat(saved.getFullName()).isEqualTo("octocat/Hello-World");
        assertThat(saved.getHtmlUrl()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(saved.getDefaultBranch()).isEqualTo("main");
        assertThat(saved.getLastSyncedAt()).isNotNull();

        assertThat(result.githubRepositoryId()).isEqualTo(GITHUB_REPOSITORY_ID);
        assertThat(result.owner()).isEqualTo("octocat");
        assertThat(result.name()).isEqualTo("Hello-World");
        assertThat(result.defaultBranch()).isEqualTo("main");
        assertThat(result.lastSyncedAt()).isNotNull();
        assertThat(result.correlationId()).isNotBlank();

        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        SyncLog completedLog = logCaptor.getAllValues().get(1);
        assertThat(completedLog.getProvider()).isEqualTo(IntegrationProvider.GITHUB);
        assertThat(completedLog.getEntityType()).isEqualTo("GITHUB_REPOSITORY");
        assertThat(completedLog.getStatus()).isEqualTo(SyncLogStatus.SUCCESS);
        assertThat(completedLog.getCompletedAt()).isNotNull();
    }

    @Test
    void runningSyncAgainUpsertsSameRepositoryInsteadOfCreatingDuplicate() {
        java.util.concurrent.atomic.AtomicReference<vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository> stored = new java.util.concurrent.atomic.AtomicReference<>();

        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(configRepository.findGitHubConfigByProjectId(PROJECT_ID)).thenReturn(Optional.of(integrationConfig));
        when(secretService.decrypt("encrypted-token")).thenReturn("github-token");
        when(gitHubRestClient.getRepository(any()))
                .thenReturn(remoteRepository("master"), remoteRepository("main"));
        when(repositoryRepository.findByProjectIdAndGithubRepositoryId(PROJECT_ID, GITHUB_REPOSITORY_ID))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repositoryRepository.save(any())).thenAnswer(invocation -> {
            var repository = (vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository) invocation.getArgument(0);
            stored.set(repository);
            return repository;
        });
        when(syncLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GitHubRepositorySyncResult first = service.syncRepository(PROJECT_ID);
        var firstSavedInstance = stored.get();
        GitHubRepositorySyncResult second = service.syncRepository(PROJECT_ID);

        assertThat(first.defaultBranch()).isEqualTo("master");
        assertThat(second.defaultBranch()).isEqualTo("main");
        assertThat(stored.get()).isSameAs(firstSavedInstance);
        assertThat(stored.get().getDefaultBranch()).isEqualTo("main");
        assertThat(stored.get().getLastSyncedAt()).isNotNull();
        verify(repositoryRepository, times(2)).save(any());
        verify(syncLogRepository, times(4)).save(any());
    }

    @Test
    void repositoryNotFoundKeepsSafeGithubErrorAndWritesFailedLog() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(configRepository.findGitHubConfigByProjectId(PROJECT_ID)).thenReturn(Optional.of(integrationConfig));
        when(secretService.decrypt("encrypted-token")).thenReturn("github-token");
        when(syncLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GitHubApiException notFound = new GitHubApiException(
                HttpStatus.NOT_FOUND,
                "GITHUB_REPOSITORY_NOT_FOUND",
                false,
                null,
                "GitHub repository was not found or is not visible",
                null);
        when(gitHubRestClient.getRepository(any())).thenThrow(notFound);

        assertThatThrownBy(() -> service.syncRepository(PROJECT_ID)).isSameAs(notFound).hasMessage("GitHub repository was not found or is not visible");

        verify(repositoryRepository, never()).save(any());
        ArgumentCaptor<SyncLog> logCaptor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(logCaptor.capture());
        SyncLog failedLog = logCaptor.getAllValues().get(1);
        assertThat(failedLog.getStatus()).isEqualTo(SyncLogStatus.FAILED);
        assertThat(failedLog.getErrorCode()).isEqualTo("GITHUB_REPOSITORY_NOT_FOUND");
        assertThat(failedLog.getErrorMessage()).doesNotContain("github-token");
    }

    private vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepository(String defaultBranch) {
        return new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository(
                GITHUB_REPOSITORY_ID,
                "NODE_123",
                "Hello-World",
                "octocat/Hello-World",
                new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository.Owner(1L, "octocat"),
                false,
                defaultBranch,
                "https://github.com/octocat/Hello-World",
                false,
                Instant.parse("2026-09-04T10:00:00Z"),
                null);
    }
}
