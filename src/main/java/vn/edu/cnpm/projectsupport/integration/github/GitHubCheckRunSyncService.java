package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRun;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRunStatus;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCheckRunRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.security.SensitiveDataSanitizer;

@Service
public class GitHubCheckRunSyncService {
    @Autowired(required = false)
    private vn.edu.cnpm.projectsupport.autotest.AutomationIncidentService automationIncidentService;

    private static final String ENTITY_TYPE = "GITHUB_CHECK_RUN_SYNC";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 10_000;

    private final GitHubRestClient gitHubRestClient;
    private final GitHubCheckRunRepository checkRunRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final GitHubCommitRepository commitRepository;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final GitHubIntegrationConfigRepository integrationConfigRepository;
    private final IntegrationSecretService secretService;
    private final SyncLogRepository syncLogRepository;

    public GitHubCheckRunSyncService(
            GitHubRestClient gitHubRestClient,
            GitHubCheckRunRepository checkRunRepository,
            GitHubRepositoryRepository repositoryRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            GitHubIntegrationConfigRepository integrationConfigRepository,
            IntegrationSecretService secretService,
            SyncLogRepository syncLogRepository) {
        this.gitHubRestClient = gitHubRestClient;
        this.checkRunRepository = checkRunRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.secretService = secretService;
        this.syncLogRepository = syncLogRepository;
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public GitHubCheckRunSyncResult syncCheckRuns(Long projectId) {
        IntegrationConfig integrationConfig = integrationConfigRepository
                .findGitHubConfigByProjectId(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "GitHub integration is not configured"));

        if (integrationConfig.getEncryptedSecret() == null
                || integrationConfig.getEncryptedSecret().isBlank()) {
            throw new IllegalArgumentException(
                    "GitHub access token is not configured");
        }

        String fullName = integrationConfig.getAccountIdentifier();

        int separator = fullName == null
                ? -1
                : fullName.indexOf('/');

        if (separator <= 0
                || separator == fullName.length() - 1
                || fullName.indexOf('/', separator + 1) >= 0) {
            throw new IllegalArgumentException(
                    "GitHub repository full name is invalid");
        }

        String token = secretService.decrypt(
                integrationConfig.getEncryptedSecret());

        GitHubClientConfig config = new GitHubClientConfig(
                fullName.substring(0, separator),
                fullName.substring(separator + 1),
                token,
                GitHubClientConfig.DEFAULT_API_VERSION,
                GitHubClientConfig.DEFAULT_TIMEOUT);

        return syncCheckRuns(projectId, config);
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    GitHubCheckRunSyncResult syncCheckRuns(
            Long projectId,
            GitHubClientConfig config) {

        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException(
                    "projectId must be positive");
        }

        if (config == null) {
            throw new IllegalArgumentException(
                    "GitHub client config must not be null");
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
        int created = 0;
        int updated = 0;
        int errors = 0;

        Instant syncedAt = Instant.now();

        try {
            vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote =
                    gitHubRestClient.getRepository(config);

            GitHubRepository localRepository =
                    upsertRepository(projectId, remote, syncedAt);

            Set<String> refs = new LinkedHashSet<>();

            if (remote.defaultBranch() != null
                    && !remote.defaultBranch().isBlank()) {
                refs.add(remote.defaultBranch());
            }

            int commitPage = 0;

            org.springframework.data.domain.Page<
                    vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit> commits;

            do {
                commits = commitRepository
                        .findByRepositoryIdOrderByCommittedAtDesc(
                                localRepository.getId(),
                                PageRequest.of(
                                        commitPage++,
                                        PAGE_SIZE));

                commits.getContent()
                        .stream()
                        .map(vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit::getSha)
                        .filter(sha -> sha != null && !sha.isBlank())
                        .forEach(refs::add);

            } while (commits.hasNext());

            Map<String, Long> pullRequestIdsByHeadSha =
                    new HashMap<>();

            int prPage = 0;

            org.springframework.data.domain.Page<GitHubPullRequest> pullRequests;

            do {
                pullRequests = pullRequestRepository
                        .findByRepositoryIdOrderByRemoteCreatedAtDesc(
                                localRepository.getId(),
                                PageRequest.of(
                                        prPage++,
                                        PAGE_SIZE));

                pullRequests.getContent().forEach(pr -> {
                    if (pr.getHeadSha() != null
                            && !pr.getHeadSha().isBlank()
                            && pr.getId() != null) {

                        refs.add(pr.getHeadSha());

                        pullRequestIdsByHeadSha.putIfAbsent(
                                pr.getHeadSha(),
                                pr.getId());
                    }
                });

            } while (pullRequests.hasNext());

            for (String ref : refs) {

                int page = 1;

                GitHubPage<GitHubCheckRunResponse> pageResult;

                do {
                    if (page > MAX_PAGES) {
                        throw new GitHubApiException(
                                org.springframework.http.HttpStatus.BAD_GATEWAY,
                                "GITHUB_PROVIDER_UNAVAILABLE",
                                false,
                                null,
                                "GitHub check-runs pagination exceeded the safety limit",
                                null);
                    }

                    try {
                        pageResult =
                                gitHubRestClient.getCheckRunsPage(
                                        config,
                                        ref,
                                        page);

                        for (GitHubCheckRunResponse remoteCheckRun
                                : pageResult.items()) {

                            try {
                                if (remoteCheckRun == null
                                        || remoteCheckRun.id() == null
                                        || remoteCheckRun.headSha() == null
                                        || remoteCheckRun.headSha().isBlank()
                                        || remoteCheckRun.name() == null
                                        || remoteCheckRun.name().isBlank()) {

                                    throw new IllegalArgumentException(
                                            "GitHub check-run response is incomplete");
                                }

                                Long pullRequestId =
                                        pullRequestIdsByHeadSha.get(
                                                remoteCheckRun.headSha());

                                GitHubCheckRunStatus status =
                                        mapStatus(
                                                remoteCheckRun.status(),
                                                remoteCheckRun.conclusion());

                                GitHubCheckRun local =
                                        checkRunRepository
                                                .findByRepositoryIdAndExternalId(
                                                        localRepository.getId(),
                                                        remoteCheckRun.id())
                                                .orElse(null);

                                if (local == null) {

                                    local = new GitHubCheckRun(
                                            localRepository.getId(),
                                            remoteCheckRun.id(),
                                            remoteCheckRun.headSha(),
                                            remoteCheckRun.name(),
                                            status,
                                            remoteCheckRun.htmlUrl(),
                                            remoteCheckRun.startedAt(),
                                            remoteCheckRun.completedAt());

                                    local.setPullRequestId(
                                            pullRequestId);

                                    checkRunRepository.save(local);
                                    created++;

                                } else {

                                    local.update(
                                            remoteCheckRun.headSha(),
                                            pullRequestId,
                                            remoteCheckRun.name(),
                                            status,
                                            remoteCheckRun.htmlUrl(),
                                            remoteCheckRun.startedAt(),
                                            remoteCheckRun.completedAt());

                                    updated++;
                                }

                                if (automationIncidentService != null) {
                                    automationIncidentService.recordCheckResult(
                                            projectId, remoteCheckRun.id(), remoteCheckRun.name(),
                                            remoteCheckRun.headSha(), remoteCheckRun.htmlUrl(), status);
                                }

                                synced++;

                            } catch (RuntimeException itemException) {
                                errors++;
                            }
                        }

                        page++;

                    } catch (GitHubApiException exception) {

                        if ("GITHUB_ACTIONS_DISABLED"
                                .equals(exception.getErrorCode())) {

                            log.setStatus(SyncLogStatus.SUCCESS);
                            log.setErrorCode(
                                    "GITHUB_ACTIONS_DISABLED");
                            log.setErrorMessage(
                                    "GitHub Actions/check runs are disabled for this repository");
                            log.setCompletedAt(Instant.now());

                            syncLogRepository.save(log);

                            return new GitHubCheckRunSyncResult(
                                    projectId,
                                    localRepository.getId(),
                                    synced,
                                    created,
                                    updated,
                                    errors,
                                    false,
                                    syncedAt,
                                    correlationId);
                        }

                        throw exception;
                    }

                } while (pageResult.nextUrl() != null);
            }

            localRepository.setLastSyncedAt(syncedAt);

            repositoryRepository.saveAndFlush(localRepository);

            log.setStatus(
                    errors == 0
                            ? SyncLogStatus.SUCCESS
                            : SyncLogStatus.FAILED);

            if (errors > 0) {
                log.setErrorCode("PARTIAL_SYNC");
                log.setErrorMessage(
                        "Một hoặc nhiều check run không thể đồng bộ");
            }

            log.setCompletedAt(Instant.now());

            syncLogRepository.save(log);

            return new GitHubCheckRunSyncResult(
                    projectId,
                    localRepository.getId(),
                    synced,
                    created,
                    updated,
                    errors,
                    true,
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

    /**
     * repositoryId ở API là ID nội bộ của GitHubRepository,
     * không phải githubRepositoryId từ GitHub.
     */
    public List<GitHubCheckRun> listCheckRuns(
            Long projectId,
            Long repositoryId) {

        if (projectId == null || projectId < 1) {
            throw new IllegalArgumentException(
                    "projectId must be positive");
        }

        if (repositoryId == null || repositoryId < 1) {
            throw new IllegalArgumentException(
                    "repositoryId must be positive");
        }

        GitHubRepository repository =
                repositoryRepository.findById(repositoryId)
                        .filter(value ->
                                projectId.equals(value.getProjectId()))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "GitHub repository does not belong to project"));

        return checkRunRepository
                .findByRepositoryIdOrderByCompletedAtDesc(
                        repository.getId());
    }

    private GitHubRepository upsertRepository(
            Long projectId,
            vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remote,
            Instant syncedAt) {

        if (remote.id() == null
                || remote.fullName() == null
                || remote.name() == null
                || remote.owner() == null
                || remote.owner().login() == null
                || remote.defaultBranch() == null
                || remote.htmlUrl() == null) {

            throw new IllegalArgumentException(
                    "GitHub repository response is incomplete");
        }

        GitHubRepository local =
                repositoryRepository
                        .findByProjectIdAndGithubRepositoryId(
                                projectId,
                                remote.id())
                        .orElseGet(() ->
                                new GitHubRepository(
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
        local.setPrivateRepository(
                remote.privateRepository());
        local.setDefaultBranch(
                remote.defaultBranch());
        local.setHtmlUrl(remote.htmlUrl());
        local.setArchived(remote.archived());
        local.setRemoteUpdatedAt(remote.updatedAt());
        local.setLastSyncedAt(syncedAt);

        return repositoryRepository.saveAndFlush(local);
    }

    static GitHubCheckRunStatus mapStatus(
            String status,
            String conclusion) {

        if (conclusion != null) {

            switch (conclusion.trim().toLowerCase()) {

                case "success" -> {
                    return GitHubCheckRunStatus.SUCCESS;
                }

                case "failure",
                        "timed_out",
                        "startup_failure",
                        "action_required" -> {
                    return GitHubCheckRunStatus.FAILURE;
                }

                case "cancelled" -> {
                    return GitHubCheckRunStatus.CANCELLED;
                }

                case "skipped" -> {
                    return GitHubCheckRunStatus.SKIPPED;
                }

                case "neutral" -> {
                    return GitHubCheckRunStatus.NEUTRAL;
                }

                default -> {
                }
            }
        }

        if ("completed".equalsIgnoreCase(status)) {
            return GitHubCheckRunStatus.FAILURE;
        }

        return GitHubCheckRunStatus.PENDING;
    }

    private String errorCode(RuntimeException exception) {

        if (exception instanceof GitHubApiException github) {
            return github.getErrorCode();
        }

        return "GITHUB_CHECK_RUN_SYNC_FAILED";
    }

    private String safeMessage(RuntimeException exception) {
        return SensitiveDataSanitizer.sanitize(
                exception,
                "GitHub check-run sync failed");
    }
}
