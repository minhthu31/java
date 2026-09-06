package vn.edu.cnpm.projectsupport.integration.github.service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.integration.github.GitHubApiException;
import vn.edu.cnpm.projectsupport.integration.github.GitHubClientConfig;
import vn.edu.cnpm.projectsupport.integration.github.GitHubRestClient;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class GitHubRepositorySyncService {

    private static final String ENTITY_TYPE = "GITHUB_REPOSITORY";

    private final ProjectRepository projectRepository;
    private final GitHubIntegrationConfigRepository configRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final SyncLogRepository syncLogRepository;
    private final GitHubRestClient gitHubRestClient;
    private final IntegrationSecretService secretService;

    public GitHubRepositorySyncService(
            ProjectRepository projectRepository,
            GitHubIntegrationConfigRepository configRepository,
            GitHubRepositoryRepository repositoryRepository,
            SyncLogRepository syncLogRepository,
            GitHubRestClient gitHubRestClient,
            IntegrationSecretService secretService) {
        this.projectRepository = projectRepository;
        this.configRepository = configRepository;
        this.repositoryRepository = repositoryRepository;
        this.syncLogRepository = syncLogRepository;
        this.gitHubRestClient = gitHubRestClient;
        this.secretService = secretService;
    }

    public GitHubRepositorySyncResult syncRepository(Long projectId) {
        validateProject(projectId);

        IntegrationConfig integrationConfig = configRepository.findGitHubConfigByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project chưa được cấu hình GitHub: " + projectId));

        RepositoryCoordinates coordinates = parseRepository(integrationConfig.getAccountIdentifier());
        String correlationId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        SyncLog syncLog = new SyncLog(
                projectId,
                IntegrationProvider.GITHUB,
                ENTITY_TYPE,
                integrationConfig.getAccountIdentifier(),
                SyncDirection.IMPORT,
                correlationId,
                startedAt);
        syncLogRepository.save(syncLog);

        try {
            String accessToken = secretService.decrypt(integrationConfig.getEncryptedSecret());
            GitHubClientConfig clientConfig = new GitHubClientConfig(
                    coordinates.owner(),
                    coordinates.repository(),
                    accessToken,
                    GitHubClientConfig.DEFAULT_API_VERSION,
                    GitHubClientConfig.DEFAULT_TIMEOUT);

            vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote = gitHubRestClient.getRepository(clientConfig);
            validateRemoteRepository(remote);

            Instant syncedAt = Instant.now();
            vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository local = repositoryRepository.findByProjectIdAndGithubRepositoryId(projectId, remote.id())
                            .orElseGet(() -> new vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository(
                                    projectId,
                                    remote.id(),
                                    remote.nodeId(),
                                    remote.name(),
                                    remote.owner().login(),
                                    remote.fullName(),
                                    remote.privateRepository(),
                                    remote.defaultBranch(),
                                    remote.htmlUrl(),
                                    remote.archived(),
                                    remote.updatedAt()));

            applySnapshot(local, remote, syncedAt);
            local = repositoryRepository.save(local);

            syncLog.setStatus(SyncLogStatus.SUCCESS);
            syncLog.setCompletedAt(Instant.now());
            syncLogRepository.save(syncLog);

            return new GitHubRepositorySyncResult(
                    projectId,
                    local.getId(),
                    local.getGithubRepositoryId(),
                    local.getOwnerLogin(),
                    local.getName(),
                    local.getFullName(),
                    local.getHtmlUrl(),
                    local.getDefaultBranch(),
                    local.getLastSyncedAt(),
                    correlationId);
        } catch (RuntimeException exception) {
            syncLog.setStatus(SyncLogStatus.FAILED);
            syncLog.setErrorCode(errorCode(exception));
            syncLog.setErrorMessage(safeMessage(exception));
            syncLog.setCompletedAt(Instant.now());
            syncLogRepository.save(syncLog);
            throw exception;
        }
    }

    private void validateProject(Long projectId) {
        if (projectId == null || !projectRepository.existsById(projectId)) {
            throw new NoSuchElementException("Project không tồn tại: " + projectId);
        }
    }

    private RepositoryCoordinates parseRepository(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Cấu hình GitHub repository không hợp lệ");
        }
        String[] parts = fullName.trim().split("/", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Cấu hình GitHub repository phải có dạng owner/name");
        }
        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    private void validateRemoteRepository(vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote) {
        if (remote == null
                || remote.id() == null
                || remote.name() == null || remote.name().isBlank()
                || remote.fullName() == null || remote.fullName().isBlank()
                || remote.owner() == null
                || remote.owner().login() == null || remote.owner().login().isBlank()
                || remote.htmlUrl() == null || remote.htmlUrl().isBlank()
                || remote.defaultBranch() == null || remote.defaultBranch().isBlank()) {
            throw new GitHubApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "GITHUB_PROVIDER_UNAVAILABLE",false,null, "GitHub returned incomplete repository information",null);
        }
    }

    private void applySnapshot(
            vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository local,
            vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote,
            Instant syncedAt) {
        local.setNodeId(remote.nodeId());
        local.setName(remote.name());
        local.setFullName(remote.fullName());
        local.setOwnerGithubUserId(remote.owner().id());
        local.setOwnerLogin(remote.owner().login());
        local.setPrivateRepository(remote.privateRepository());
        local.setDefaultBranch(remote.defaultBranch());
        local.setHtmlUrl(remote.htmlUrl());
        local.setArchived(remote.archived());
        local.setRemoteUpdatedAt(remote.updatedAt());
        local.setLastSyncedAt(syncedAt);
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof GitHubApiException githubApiException) {
            return githubApiException.getErrorCode();
        }
        if (exception instanceof IllegalArgumentException) {
            return "GITHUB_CONFIG_INVALID";
        }
        return "GITHUB_REPOSITORY_SYNC_FAILED";
    }

    private String safeMessage(RuntimeException exception) {
        if (exception instanceof GitHubApiException) {
            return exception.getMessage();
        }
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return "Không thể đồng bộ GitHub repository";
    }

    private record RepositoryCoordinates(String owner, String repository) {
    }
}
