package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class GitHubPullRequestSyncService {

    private static final String ENTITY_TYPE = "GITHUB_PULL_REQUEST_SYNC";
    private static final int MAX_PAGES = 10_000;

    private final GitHubRestClient gitHubRestClient;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final UserExternalAccountRepository externalAccountRepository;
    private final SyncLogRepository syncLogRepository;
    private final GitHubIntegrationConfigRepository configRepository;
    private final IntegrationSecretService secretService;
    private final GitHubTaskLinkService taskLinkService;

    public GitHubPullRequestSyncService(
            GitHubRestClient gitHubRestClient,
            GitHubPullRequestRepository pullRequestRepository,
            GitHubRepositoryRepository repositoryRepository,
            UserExternalAccountRepository externalAccountRepository,
            SyncLogRepository syncLogRepository,
            GitHubIntegrationConfigRepository configRepository,
            IntegrationSecretService secretService,
            GitHubTaskLinkService taskLinkService) {
        this.gitHubRestClient = gitHubRestClient;
        this.pullRequestRepository = pullRequestRepository;
        this.repositoryRepository = repositoryRepository;
        this.externalAccountRepository = externalAccountRepository;
        this.syncLogRepository = syncLogRepository;
        this.configRepository = configRepository;
        this.secretService = secretService;
        this.taskLinkService = taskLinkService;
    }

    public GitHubPullRequestSyncResult syncPullRequests(Long projectId) {
        IntegrationConfig integrationConfig = configRepository.findGitHubConfigByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("GitHub integration is not configured"));
        if (integrationConfig.getEncryptedSecret() == null || integrationConfig.getEncryptedSecret().isBlank()) {
            throw new IllegalArgumentException("GitHub access token is not configured");
        }

        String fullName = integrationConfig.getAccountIdentifier();
        int separator = fullName == null ? -1 : fullName.indexOf('/');
        if (separator <= 0 || separator == fullName.length() - 1 || fullName.indexOf('/', separator + 1) >= 0) {
            throw new IllegalArgumentException("GitHub repository full name is invalid");
        }

        GitHubClientConfig config = new GitHubClientConfig(
                fullName.substring(0, separator),
                fullName.substring(separator + 1),
                secretService.decrypt(integrationConfig.getEncryptedSecret()),
                GitHubClientConfig.DEFAULT_API_VERSION,
                GitHubClientConfig.DEFAULT_TIMEOUT);
        return syncPullRequests(projectId, config);
    }

    GitHubPullRequestSyncResult syncPullRequests(Long projectId, GitHubClientConfig config) {
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
        int linksCreated = 0;
        int unlinkedActivities = 0;
        int errors = 0;
        Instant syncedAt = Instant.now();

        try {
            GitHubRepository remoteRepository = gitHubRestClient.getRepository(config);
            vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository localRepository =
                    upsertRepository(projectId, remoteRepository, syncedAt);

            int page = 1;
            String nextUrl;
            do {
                if (page > MAX_PAGES) {
                    throw new GitHubApiException(
                            org.springframework.http.HttpStatus.BAD_GATEWAY,
                            "GITHUB_PROVIDER_UNAVAILABLE",
                            false,
                            null,
                            "GitHub pagination exceeded the safety limit",
                            null);
                }
                GitHubPage<GitHubPullRequest> pageResult = gitHubRestClient.getPullRequestsPage(config, "all", page);
                for (GitHubPullRequest listedPullRequest : pageResult.items()) {
                    try {
                        if (listedPullRequest == null || listedPullRequest.number() == null) {
                            throw new IllegalArgumentException("GitHub pull request list item has no number");
                        }
                        GitHubPullRequest remote = gitHubRestClient.getPullRequest(config, listedPullRequest.number());
                        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest local =
                                upsertPullRequest(localRepository.getId(), remote);
                        GitHubTaskLinkResult linkResult = taskLinkService.linkPullRequest(projectId, local);
                        linksCreated += linkResult.linksCreated();
                        if (linkResult.linksCreated() == 0 && linkResult.duplicateLinks() == 0) {
                            unlinkedActivities++;
                        }
                        synced++;
                    } catch (RuntimeException pullRequestException) {
                        errors++;
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
                log.setErrorMessage("Một hoặc nhiều Pull Request không thể đồng bộ");
            }
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);

            return new GitHubPullRequestSyncResult(
                    projectId, localRepository.getId(), synced, linksCreated, unlinkedActivities,
                    errors, syncedAt, correlationId);
        } catch (RuntimeException exception) {
            log.setStatus(SyncLogStatus.FAILED);
            log.setErrorCode(errorCode(exception));
            log.setErrorMessage(safeMessage(exception));
            log.setCompletedAt(Instant.now());
            syncLogRepository.save(log);
            throw exception;
        }
    }

    private vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository upsertRepository(
            Long projectId, GitHubRepository remote, Instant syncedAt) {
        if (remote == null || remote.id() == null || remote.fullName() == null || remote.name() == null
                || remote.owner() == null || remote.owner().login() == null
                || remote.defaultBranch() == null || remote.htmlUrl() == null) {
            throw new IllegalArgumentException("GitHub repository response is incomplete");
        }
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository local = repositoryRepository
                .findByProjectIdAndGithubRepositoryId(projectId, remote.id())
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
        return repositoryRepository.saveAndFlush(local);
    }

    private vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest upsertPullRequest(
            Long repositoryId, GitHubPullRequest remote) {
        if (remote == null || remote.id() == null || remote.number() == null
                || remote.title() == null || remote.head() == null || remote.head().ref() == null
                || remote.base() == null || remote.base().ref() == null || remote.htmlUrl() == null) {
            throw new IllegalArgumentException("GitHub pull request response is incomplete");
        }

        GitHubPullRequestState state = GitHubPullRequestState.valueOf(remote.localState());
        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest local = pullRequestRepository
                .findByRepositoryIdAndNumber(repositoryId, remote.number())
                .orElseGet(() -> new vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest(
                        repositoryId,
                        remote.id(),
                        remote.number(),
                        remote.title(),
                        remote.body(),
                        remote.head().ref(),
                        remote.head().sha(),
                        remote.base().ref(),
                        state,
                        Boolean.TRUE.equals(remote.draft()),
                        remote.mergedAt(),
                        remote.mergeCommitSha(),
                        remote.commits(),
                        remote.additions(),
                        remote.deletions(),
                        remote.changedFiles(),
                        remote.closedAt(),
                        remote.htmlUrl()));

        local.setGithubPullRequestId(remote.id());
        local.setRemoteCreatedAt(remote.createdAt());
        local.setTitle(remote.title());
        local.setBody(remote.body());
        local.setHeadRef(remote.head().ref());
        local.setHeadSha(remote.head().sha());
        local.setBaseRef(remote.base().ref());
        local.setState(state);
        local.setDraft(Boolean.TRUE.equals(remote.draft()));
        local.setMergedAt(remote.mergedAt());
        local.setMergeCommitSha(remote.mergeCommitSha());
        local.setCommitCount(remote.commits());
        local.setAdditions(remote.additions());
        local.setDeletions(remote.deletions());
        local.setChangedFiles(remote.changedFiles());
        local.setClosedAt(remote.closedAt());
        local.setHtmlUrl(remote.htmlUrl());
        local.setAuthorGithubUserId(remote.user() == null ? null : remote.user().id());
        local.setAuthorLogin(remote.user() == null ? null : remote.user().login());
        mapExternalAuthor(local, remote.user());
        return pullRequestRepository.saveAndFlush(local);
    }

    private void mapExternalAuthor(
            vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest local,
            GitHubUser author) {
        if (author == null || author.id() == null) {
            local.setAuthorExternalAccountId(null);
            return;
        }
        externalAccountRepository
                .findByProviderAndExternalUserId(IntegrationProvider.GITHUB, String.valueOf(author.id()))
                .map(UserExternalAccount::getId)
                .ifPresentOrElse(local::setAuthorExternalAccountId, () -> local.setAuthorExternalAccountId(null));
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof GitHubApiException github) {
            return github.getErrorCode();
        }
        return "GITHUB_PULL_REQUEST_SYNC_FAILED";
    }

    private String safeMessage(RuntimeException exception) {
        if (exception instanceof GitHubApiException) {
            return exception.getMessage();
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "GitHub pull request sync failed" : message;
    }
}
