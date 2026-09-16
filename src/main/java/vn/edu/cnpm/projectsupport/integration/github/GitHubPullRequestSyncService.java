package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestCommitRepository;
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
import vn.edu.cnpm.projectsupport.security.SensitiveDataSanitizer;

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
    private final GitHubCommitSyncService commitSyncService;
    private final GitHubPullRequestCommitRepository pullRequestCommitRepository;

    public GitHubPullRequestSyncService(
            GitHubRestClient gitHubRestClient,
            GitHubPullRequestRepository pullRequestRepository,
            GitHubRepositoryRepository repositoryRepository,
            UserExternalAccountRepository externalAccountRepository,
            SyncLogRepository syncLogRepository,
            GitHubIntegrationConfigRepository configRepository,
            IntegrationSecretService secretService,
            GitHubTaskLinkService taskLinkService,
            GitHubCommitSyncService commitSyncService,
            GitHubPullRequestCommitRepository pullRequestCommitRepository) {
        this.gitHubRestClient = gitHubRestClient;
        this.pullRequestRepository = pullRequestRepository;
        this.repositoryRepository = repositoryRepository;
        this.externalAccountRepository = externalAccountRepository;
        this.syncLogRepository = syncLogRepository;
        this.configRepository = configRepository;
        this.secretService = secretService;
        this.taskLinkService = taskLinkService;
        this.commitSyncService = commitSyncService;
        this.pullRequestCommitRepository = pullRequestCommitRepository;
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
            vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository localRepository = upsertRepository(projectId, remoteRepository, syncedAt);

            int page = 1;
            String nextUrl;
            do {
                if (page > MAX_PAGES) {
                    throw new GitHubApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,"GITHUB_PROVIDER_UNAVAILABLE",
                    false,null,"GitHub pagination exceeded the safety limit",null);
                }
                GitHubPage<GitHubPullRequest> pageResult = gitHubRestClient.getPullRequestsPage(config, "all", page);
                for (GitHubPullRequest listedPullRequest : pageResult.items()) {
                    try {
                        if (listedPullRequest == null || listedPullRequest.number() == null) {
                            throw new IllegalArgumentException("GitHub pull request list item has no number");
                        }
                        GitHubPullRequest remote = gitHubRestClient.getPullRequest(config, listedPullRequest.number());
                        vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest local = upsertPullRequest(localRepository.getId(), remote);
                        GitHubTaskLinkResult linkResult = taskLinkService.linkPullRequest(projectId, local);
                        linksCreated += linkResult.linksCreated();
                        if (linkResult.linksCreated() == 0 && linkResult.duplicateLinks() == 0) {
                            unlinkedActivities++;
                        }
                        CommitLinkSyncResult commitResult = syncPullRequestCommits(
                                projectId,
                                localRepository.getId(),
                                local.getId(),
                                remote.number(),
                                config);
                        linksCreated += commitResult.linksCreated();
                        unlinkedActivities += commitResult.unlinkedActivities();
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

            return new GitHubPullRequestSyncResult(projectId, localRepository.getId(), synced, linksCreated, unlinkedActivities, errors, syncedAt, correlationId);
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
                        remote.htmlUrl(),
                        remote.createdAt()));

        local.setGithubPullRequestId(remote.id());
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
        local.setRemoteCreatedAt(remote.createdAt());
        local.setHtmlUrl(remote.htmlUrl());
        local.setAuthorGithubUserId(remote.user() == null ? null : remote.user().id());
        local.setAuthorLogin(remote.user() == null ? null : remote.user().login());
        mapExternalAuthor(local, remote.user());
        return pullRequestRepository.saveAndFlush(local);
    }

    private CommitLinkSyncResult syncPullRequestCommits(
            Long projectId,
            Long repositoryId,
            Long pullRequestId,
            Integer pullRequestNumber,
            GitHubClientConfig config) {
        if (pullRequestId == null) {
            throw new IllegalStateException("Pull Request must be persisted before its commits are synchronized");
        }

        List<GitHubPullRequestCommit> existingLinks =
                pullRequestCommitRepository.findByPullRequestIdOrderByCommitOrderAsc(pullRequestId);
        Map<Long, GitHubPullRequestCommit> existingByCommitId = new HashMap<>();
        for (GitHubPullRequestCommit link : existingLinks) {
            existingByCommitId.put(link.getCommitId(), link);
        }

        Set<Long> retainedCommitIds = new HashSet<>();
        int linksCreated = 0;
        int unlinkedActivities = 0;
        int commitOrder = 0;
        int page = 1;
        String nextUrl;
        do {
            if (page > MAX_PAGES) {
                throw new GitHubApiException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "GITHUB_PROVIDER_UNAVAILABLE",
                        false,
                        null,
                        "GitHub Pull Request commit pagination exceeded the safety limit",
                        null);
            }

            GitHubPage<vn.edu.cnpm.projectsupport.integration.github.GitHubCommit> pageResult =
                    gitHubRestClient.getPullRequestCommitsPage(config, pullRequestNumber, page);
            for (vn.edu.cnpm.projectsupport.integration.github.GitHubCommit listedCommit : pageResult.items()) {
                if (listedCommit == null || listedCommit.sha() == null || listedCommit.sha().isBlank()) {
                    throw new IllegalArgumentException("GitHub Pull Request commit list item has no SHA");
                }

                vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remoteCommit =
                        gitHubRestClient.getCommit(config, listedCommit.sha());
                vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit localCommit =
                        commitSyncService.upsertCommit(repositoryId, remoteCommit);
                if (localCommit.getId() == null) {
                    throw new IllegalStateException("Commit must be persisted before it is linked to a Pull Request");
                }

                retainedCommitIds.add(localCommit.getId());
                GitHubPullRequestCommit relation = existingByCommitId.get(localCommit.getId());
                if (relation == null) {
                    relation = new GitHubPullRequestCommit(pullRequestId, localCommit.getId(), ++commitOrder);
                } else {
                    relation.setCommitOrder(++commitOrder);
                }
                pullRequestCommitRepository.saveAndFlush(relation);

                GitHubTaskLinkResult taskLinkResult = taskLinkService.linkCommit(projectId, localCommit);
                linksCreated += taskLinkResult.linksCreated();
                if (taskLinkResult.linksCreated() == 0 && taskLinkResult.duplicateLinks() == 0) {
                    unlinkedActivities++;
                }
            }
            nextUrl = pageResult.nextUrl();
            page++;
        } while (nextUrl != null);

        List<GitHubPullRequestCommit> staleLinks = existingLinks.stream()
                .filter(link -> !retainedCommitIds.contains(link.getCommitId()))
                .toList();
        if (!staleLinks.isEmpty()) {
            pullRequestCommitRepository.deleteAll(staleLinks);
        }
        return new CommitLinkSyncResult(linksCreated, unlinkedActivities);
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
        return SensitiveDataSanitizer.sanitize(
                exception,
                "GitHub pull request sync failed");
    }

    private record CommitLinkSyncResult(int linksCreated, int unlinkedActivities) {
    }
}
