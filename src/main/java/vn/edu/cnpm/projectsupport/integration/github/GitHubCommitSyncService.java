package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;

/**
 * Imports the commit snapshot for a configured GitHub repository.
 * Each commit is persisted independently so one bad record does not discard
 * commits that were already saved in the same synchronization run.
 */
@Service
public class GitHubCommitSyncService {

    private static final String ENTITY_TYPE = "GITHUB_COMMIT_SYNC";
    private static final int MAX_PAGES = 10_000;

    private final GitHubRestClient gitHubRestClient;
    private final GitHubCommitRepository commitRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final UserExternalAccountRepository externalAccountRepository;
    private final SyncLogRepository syncLogRepository;
    private final GitHubIntegrationConfigRepository integrationConfigRepository;
    private final IntegrationSecretService secretService;

    public GitHubCommitSyncService(
            GitHubRestClient gitHubRestClient,
            GitHubCommitRepository commitRepository,
            GitHubRepositoryRepository repositoryRepository,
            UserExternalAccountRepository externalAccountRepository,
            SyncLogRepository syncLogRepository,
            GitHubIntegrationConfigRepository integrationConfigRepository,
            IntegrationSecretService secretService) {
        this.gitHubRestClient = gitHubRestClient;
        this.commitRepository = commitRepository;
        this.repositoryRepository = repositoryRepository;
        this.externalAccountRepository = externalAccountRepository;
        this.syncLogRepository = syncLogRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.secretService = secretService;
    }

    /** Loads the project GitHub secret and repository snapshot from the existing integration configuration. */
    public GitHubCommitSyncResult syncCommits(Long projectId) {
        IntegrationConfig integrationConfig = integrationConfigRepository
                .findGitHubConfigByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("GitHub integration is not configured"));
        if (integrationConfig.getEncryptedSecret() == null || integrationConfig.getEncryptedSecret().isBlank()) {
            throw new IllegalArgumentException("GitHub access token is not configured");
        }

        GitHubRepository repository = repositoryRepository
                .findByProjectIdOrderByFullNameAsc(projectId, PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("GitHub repository is not configured"));

        String fullName = repository.getFullName();
        int separator = fullName == null ? -1 : fullName.indexOf('/');
        if (separator <= 0 || separator == fullName.length() - 1) {
            throw new IllegalArgumentException("GitHub repository full name is invalid");
        }
        String token = secretService.decrypt(integrationConfig.getEncryptedSecret());
        GitHubClientConfig config = new GitHubClientConfig(
                fullName.substring(0, separator),
                fullName.substring(separator + 1),
                token,
                GitHubClientConfig.DEFAULT_API_VERSION,
                GitHubClientConfig.DEFAULT_TIMEOUT);
        return syncCommits(projectId, config);
    }

    public GitHubCommitSyncResult syncCommits(Long projectId, GitHubClientConfig config) {
        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException("projectId must be positive");
        }
        if (config == null) {
            throw new IllegalArgumentException("GitHub client config must not be null");
        }

        String correlationId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        SyncLog log = new SyncLog(
                projectId,
                IntegrationProvider.GITHUB,
                ENTITY_TYPE,
                config.owner() + "/" + config.repository(),
                SyncDirection.IMPORT,
                correlationId,
                startedAt);
        syncLogRepository.save(log);

        int synced = 0;
        int errors = 0;
        Instant syncedAt = Instant.now();

        try {
            vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepository =
                    gitHubRestClient.getRepository(config);
            GitHubRepository localRepository = upsertRepository(projectId, remoteRepository, syncedAt);

            int page = 1;
            String nextUrl;
            do {
                if (page > MAX_PAGES) {
                    throw new GitHubApiException(
                            org.springframework.http.HttpStatus.BAD_GATEWAY,
                            "GITHUB_PROVIDER_UNAVAILABLE",
                            false, null,
                            "GitHub pagination exceeded the safety limit", null);
                }
                GitHubPage<vn.edu.cnpm.projectsupport.integration.github.GitHubCommit> pageResult =
                        gitHubRestClient.getCommitsPage(config, page);
                for (vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remoteCommit : pageResult.items()) {
                    try {
                        upsertCommit(localRepository.getId(), remoteCommit);
                        synced++;
                    } catch (RuntimeException commitException) {
                        errors++;
                        // Keep successfully persisted commits and continue with the next item.
                        log.setErrorCode("PARTIAL_SYNC");
                        log.setErrorMessage("Một hoặc nhiều commit không thể đồng bộ");
                    }
                }
                nextUrl = pageResult.nextUrl();
                page++;
            } while (nextUrl != null);

            localRepository.setLastSyncedAt(syncedAt);
            repositoryRepository.saveAndFlush(localRepository);

            log.setStatus(errors == 0 ? SyncLogStatus.SUCCESS : SyncLogStatus.FAILED);
            if (errors > 0) {
                log.setErrorCode("PARTIAL_SYNC");
                log.setErrorMessage("Một hoặc nhiều commit không thể đồng bộ");
            }
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);

            return new GitHubCommitSyncResult(
                    projectId,
                    localRepository.getId(),
                    synced,
                    errors,
                    syncedAt,
                    correlationId);
        } catch (RuntimeException exception) {
            log.setStatus(SyncLogStatus.FAILED);
            log.setErrorCode(errorCode(exception));
            log.setErrorMessage(safeMessage(exception));
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);
            throw exception;
        }
    }

    private GitHubRepository upsertRepository(Long projectId, vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote, Instant syncedAt) {
        if (remote.id() == null || remote.fullName() == null || remote.name() == null
                || remote.owner() == null || remote.owner().login() == null
                || remote.defaultBranch() == null || remote.htmlUrl() == null) {
            throw new IllegalArgumentException("GitHub repository response is incomplete");
        }
        GitHubRepository local = repositoryRepository
                .findByProjectIdAndGithubRepositoryId(projectId, remote.id())
                .orElseGet(() -> new GitHubRepository(
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
        local.setNodeId(remote.nodeId());
        local.setName(remote.name());
        local.setOwnerGithubUserId(remote.owner().id());
        local.setOwnerLogin(remote.owner().login());
        local.setPrivateRepository(remote.privateRepository());
        local.setDefaultBranch(remote.defaultBranch());
        local.setHtmlUrl(remote.htmlUrl());
        local.setArchived(remote.archived());
        local.setRemoteUpdatedAt(remote.updatedAt());
        local.setLastSyncedAt(syncedAt);
        return repositoryRepository.saveAndFlush(local);
    }

    private void upsertCommit(Long repositoryId, vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remote) {
        if (remote.sha() == null || remote.sha().isBlank()
                || remote.commit() == null || remote.commit().message() == null
                || remote.commit().author() == null || remote.commit().author().date() == null
                || remote.htmlUrl() == null) {
            throw new IllegalArgumentException("GitHub commit response is incomplete");
        }
        GitHubCommit local = commitRepository.findByRepositoryIdAndSha(repositoryId, remote.sha())
                .orElseGet(() -> new GitHubCommit(
                        repositoryId,
                        remote.sha(),
                        remote.commit().message(),
                        remote.commit().author().date(),
                        remote.htmlUrl()));
        local.setMessage(remote.commit().message());
        local.setCommittedAt(remote.commit().author().date());
        local.setHtmlUrl(remote.htmlUrl());
        local.setAuthorGithubUserId(remote.author() == null ? null : remote.author().id());
        local.setAuthorLogin(remote.author() == null ? null : remote.author().login());
        local.setGitAuthorName(remote.commit().author().name());
        local.setGitAuthorEmail(remote.commit().author().email());
        if (remote.commit().committer() != null) {
            local.setGitCommitterName(remote.commit().committer().name());
            local.setGitCommitterEmail(remote.commit().committer().email());
            local.setCommitterAt(remote.commit().committer().date());
        } else {
            local.setGitCommitterName(null);
            local.setGitCommitterEmail(null);
            local.setCommitterAt(null);
        }
        local.setAdditions(remote.additions());
        local.setDeletions(remote.deletions());
        local.setFilesChanged(remote.filesChanged());
        local.setParentShas(String.join(",", remote.parentShas()));
        mapExternalAuthor(local, remote.author());
        commitRepository.saveAndFlush(local);
    }

    private void mapExternalAuthor(GitHubCommit local, GitHubUser author) {
        if (author == null || author.id() == null) {
            local.setAuthorExternalAccountId(null);
            return;
        }
        externalAccountRepository
                .findByProviderAndExternalUserId(IntegrationProvider.GITHUB, String.valueOf(author.id()))
                .map(UserExternalAccount::getId)
                .ifPresentOrElse(local::setAuthorExternalAccountId,
                        () -> local.setAuthorExternalAccountId(null));
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof GitHubApiException github) {
            return github.getErrorCode();
        }
        return "GITHUB_COMMIT_SYNC_FAILED";
    }

    private String safeMessage(RuntimeException exception) {
        if (exception instanceof GitHubApiException) {
            return exception.getMessage();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "GitHub commit sync failed" : message;
    }
}
