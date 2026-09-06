package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubUnlinkedAuthorProjection;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class GitHubAccountLinkService {

    private final UserExternalAccountRepository externalAccountRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final GitHubIntegrationConfigRepository integrationConfigRepository;
    private final GitHubCommitRepository commitRepository;
    private final GitHubPullRequestRepository pullRequestRepository;
    private final IntegrationSecretService secretService;
    private final GitHubRestClient gitHubRestClient;
    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserService currentUserService;

    public GitHubAccountLinkService(
            UserExternalAccountRepository externalAccountRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            GitHubIntegrationConfigRepository integrationConfigRepository,
            GitHubCommitRepository commitRepository,
            GitHubPullRequestRepository pullRequestRepository,
            IntegrationSecretService secretService,
            GitHubRestClient gitHubRestClient,
            ActivityLogRepository activityLogRepository,
            CurrentUserService currentUserService) {
        this.externalAccountRepository = externalAccountRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.integrationConfigRepository = integrationConfigRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.secretService = secretService;
        this.gitHubRestClient = gitHubRestClient;
        this.activityLogRepository = activityLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GitHubAccountLinkResponse linkAccount(Long projectId, Long userId, GitHubAccountLinkRequest request) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        if (!isProjectParticipant(projectId, userId)) {
            throw new ResourceNotFoundException("Người dùng " + user.getUsername() + " không thuộc Project với ID: " + projectId);
        }

        GitHubUser remote = fetchRemoteUser(projectId, request.getUsername());
        String verifiedExternalId = verifyIdentity(request, remote);

        return externalAccountRepository
                .findByProviderAndExternalUserId(IntegrationProvider.GITHUB, verifiedExternalId)
                .map(existing -> reuseOrReject(existing, projectId, userId, remote))
                .orElseGet(() -> createOrRelink(projectId, project, userId, verifiedExternalId, remote));
    }

    @Transactional(readOnly = true)
    public Page<GitHubUnlinkedAccountResponse> listUnlinkedAccounts(Long projectId, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId);
        }

        Map<Long, String> byGithubUserId = new LinkedHashMap<>();
        for (GitHubUnlinkedAuthorProjection projection : commitRepository.findUnlinkedAuthors(projectId)) {
            byGithubUserId.putIfAbsent(projection.getGithubUserId(), projection.getLogin());
        }
        for (GitHubUnlinkedAuthorProjection projection : pullRequestRepository.findUnlinkedAuthors(projectId)) {
            byGithubUserId.putIfAbsent(projection.getGithubUserId(), projection.getLogin());
        }

        List<GitHubUnlinkedAccountResponse> all = new ArrayList<>(byGithubUserId.size());
        byGithubUserId.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> all.add(new GitHubUnlinkedAccountResponse(String.valueOf(entry.getKey()), entry.getValue())));

        int start = Math.min((int) pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    private boolean isProjectParticipant(Long projectId, Long userId) {
        return projectRepository.countActiveLeader(projectId, userId) > 0
                || projectRepository.countActiveMember(projectId, userId) > 0
                || projectRepository.countAssignedLecturer(projectId, userId) > 0;
    }

    private GitHubUser fetchRemoteUser(Long projectId, String username) {
        IntegrationConfig integrationConfig = integrationConfigRepository
                .findGitHubConfigByProjectId(projectId)
                .orElseThrow(() -> new IllegalArgumentException("GitHub integration is not configured"));
        if (integrationConfig.getEncryptedSecret() == null || integrationConfig.getEncryptedSecret().isBlank()) {
            throw new IllegalArgumentException("GitHub access token is not configured");
        }

        String fullName = integrationConfig.getAccountIdentifier();
        int separator = fullName == null ? -1 : fullName.indexOf('/');
        if (separator <= 0 || separator == fullName.length() - 1 || fullName.indexOf('/', separator + 1) >= 0) {
            throw new IllegalArgumentException("GitHub repository full name is invalid");
        }
        String token = secretService.decrypt(integrationConfig.getEncryptedSecret());
        GitHubClientConfig clientConfig = new GitHubClientConfig(
                fullName.substring(0, separator),
                fullName.substring(separator + 1),
                token,
                GitHubClientConfig.DEFAULT_API_VERSION,
                Duration.ofSeconds(10));
        return gitHubRestClient.getUser(clientConfig, username);
    }

    private String verifyIdentity(GitHubAccountLinkRequest request, GitHubUser remote) {
        if (remote == null || remote.id() == null || remote.login() == null) {
            throw new IllegalArgumentException("GitHub trả về hồ sơ người dùng không đầy đủ");
        }
        String remoteId = String.valueOf(remote.id());
        if (!remoteId.equals(request.getExternalAccountId())) {
            throw new IllegalArgumentException("externalAccountId không khớp với GitHub user ID thực tế của username đã cung cấp");
        }
        if (!remote.login().equalsIgnoreCase(request.getUsername().trim())) {
            throw new IllegalArgumentException("username không khớp với GitHub login thực tế");
        }
        return remoteId;
    }

    private GitHubAccountLinkResponse reuseOrReject(UserExternalAccount existing, Long projectId, Long userId, GitHubUser remote) {
        if (!existing.getUserId().equals(userId)) {
            throw new ResourceInUseException("GitHub account " + existing.getExternalUserId() + " đã liên kết với người dùng khác");
        }
        existing.setExternalLogin(remote.login());
        existing.setAvatarUrl(remote.avatarUrl());
        existing.setProfileUrl(remote.htmlUrl());
        UserExternalAccount saved = externalAccountRepository.save(existing);
        backfill(projectId, existing.getExternalUserId(), saved.getId());
        return toResponse(saved);
    }

    private GitHubAccountLinkResponse createOrRelink(Long projectId, vn.edu.cnpm.projectsupport.project.domain.Project project,
        Long userId, String externalUserId, GitHubUser remote) {
        var existingForUser = externalAccountRepository.findByUserIdAndProvider(userId, IntegrationProvider.GITHUB);
        UserExternalAccount account;
        boolean relink = existingForUser.isPresent();
        String oldExternalUserId = null;
        String oldLogin = null;

        if (relink) {
            account = existingForUser.get();
            oldExternalUserId = account.getExternalUserId();
            oldLogin = account.getExternalLogin();
            account.relink(externalUserId, remote.login(), remote.avatarUrl(), remote.htmlUrl());
        } else {
            account = new UserExternalAccount(userId, IntegrationProvider.GITHUB, externalUserId, remote.login(), remote.avatarUrl(), remote.htmlUrl());
        }

        UserExternalAccount saved = externalAccountRepository.saveAndFlush(account);
        backfill(projectId, externalUserId, saved.getId());

        Long actorUserId = currentUserService.findCurrentUser().map(User::getId).orElse(userId);
        if (relink && !externalUserId.equals(oldExternalUserId)) {
            activityLogRepository.save(ActivityLog.githubAccountRelinked(
                    project.getGroupId(), saved.getId(), actorUserId, userId,
                    oldExternalUserId, oldLogin, externalUserId, remote.login()));
        } else if (!relink) {
            activityLogRepository.save(ActivityLog.githubAccountLinked(
                    project.getGroupId(), saved.getId(), actorUserId, userId,
                    externalUserId, remote.login()));
        }
        return toResponse(saved);
    }

    private void backfill(Long projectId, String externalUserId, Long accountId) {
        Long githubUserId = Long.valueOf(externalUserId);
        commitRepository.backfillAuthorExternalAccountId(projectId, githubUserId, accountId);
        pullRequestRepository.backfillAuthorExternalAccountId(projectId, githubUserId, accountId);
    }

    private GitHubAccountLinkResponse toResponse(UserExternalAccount saved) {
        Instant linkedAt = saved.getUpdatedAt() != null ? saved.getUpdatedAt() : Instant.now();
        return new GitHubAccountLinkResponse(
                saved.getUserId(),
                saved.getExternalUserId(),
                saved.getExternalLogin(),
                saved.getAvatarUrl(),
                saved.getProfileUrl(),
                linkedAt);
    }
}
