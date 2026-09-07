package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.CurrentUserService;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class GitHubAccountLinkServiceTest {

    private static final long PROJECT_ID = 101L;
    private static final long USER_ID = 202L;
    private static final long OTHER_USER_ID = 303L;
    private static final long GITHUB_ID = 9001L;
    private static final long NEW_GITHUB_ID = 9002L;

    @Mock private UserExternalAccountRepository externalAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private GitHubIntegrationConfigRepository integrationConfigRepository;
    @Mock private GitHubCommitRepository commitRepository;
    @Mock private GitHubPullRequestRepository pullRequestRepository;
    @Mock private IntegrationSecretService secretService;
    @Mock private GitHubRestClient gitHubRestClient;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private User user;
    @Mock private Project project;

    private GitHubAccountLinkService service;

    @BeforeEach
    void setUp() {
        service = new GitHubAccountLinkService(
                externalAccountRepository, userRepository, projectRepository,
                integrationConfigRepository, commitRepository, pullRequestRepository,
                secretService, gitHubRestClient, activityLogRepository, currentUserService);
        lenient().when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        lenient().when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        lenient().when(project.getGroupId()).thenReturn(77L);
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(user.getUsername()).thenReturn("member");
        lenient().when(projectRepository.countActiveMember(PROJECT_ID, USER_ID)).thenReturn(1L);
        lenient().when(integrationConfigRepository.findGitHubConfigByProjectId(PROJECT_ID))
                .thenReturn(Optional.of(config()));
        lenient().when(secretService.decrypt("encrypted")).thenReturn("token");
        lenient().when(gitHubRestClient.getUser(any(), eq("octocat")))
                .thenReturn(new GitHubUser(GITHUB_ID, "octocat", "Octo Cat", "avatar", "profile", null));
        lenient().when(currentUserService.findCurrentUser()).thenReturn(Optional.of(user));
    }

    @Test
    void success_createsLink_backfillsOnlyTheRequestedProject_andAudits() {
        GitHubAccountLinkRequest request = new GitHubAccountLinkRequest(String.valueOf(GITHUB_ID), "octocat");
        UserExternalAccount saved = account(USER_ID, GITHUB_ID, "octocat");
        when(externalAccountRepository.findByProviderAndExternalUserId(IntegrationProvider.GITHUB, String.valueOf(GITHUB_ID)))
                .thenReturn(Optional.empty());
        when(externalAccountRepository.findByUserIdAndProvider(USER_ID, IntegrationProvider.GITHUB))
                .thenReturn(Optional.empty());
        when(externalAccountRepository.saveAndFlush(any(UserExternalAccount.class))).thenReturn(saved);

        GitHubAccountLinkResponse response = service.linkAccount(PROJECT_ID, USER_ID, request);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.externalAccountId()).isEqualTo(String.valueOf(GITHUB_ID));
        verify(commitRepository).backfillAuthorExternalAccountId(PROJECT_ID, GITHUB_ID, saved.getId());
        verify(pullRequestRepository).backfillAuthorExternalAccountId(PROJECT_ID, GITHUB_ID, saved.getId());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void wrongProject_rejectsTargetUserBeforeCallingGitHub() {
        lenient().when(projectRepository.countActiveMember(PROJECT_ID, USER_ID)).thenReturn(0L);
        when(projectRepository.countActiveLeader(PROJECT_ID, USER_ID)).thenReturn(0L);
        when(projectRepository.countAssignedLecturer(PROJECT_ID, USER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> service.linkAccount(
                PROJECT_ID, USER_ID, new GitHubAccountLinkRequest(String.valueOf(GITHUB_ID), "octocat")))
                .isInstanceOf(vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException.class);
        verifyNoGitHubSideEffects();
    }

    @Test
    void duplicateGithubId_rejectsWhenLinkedToAnotherUser() {
        UserExternalAccount existing = account(OTHER_USER_ID, GITHUB_ID, "octocat");
        when(externalAccountRepository.findByProviderAndExternalUserId(IntegrationProvider.GITHUB, String.valueOf(GITHUB_ID)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.linkAccount(
                PROJECT_ID, USER_ID, new GitHubAccountLinkRequest(String.valueOf(GITHUB_ID), "octocat")))
                .isInstanceOf(ResourceInUseException.class);
        verify(externalAccountRepository, never()).saveAndFlush(any());
        verify(commitRepository, never()).backfillAuthorExternalAccountId(any(), any(), any());
        verify(pullRequestRepository, never()).backfillAuthorExternalAccountId(any(), any(), any());
        verify(activityLogRepository, never()).save(any());
    }

    @Test
    void relink_updatesExistingUserLink_backfillsNewGithubId_andAuditsChange() {
        UserExternalAccount existing = account(USER_ID, GITHUB_ID, "old-login");
        UserExternalAccount saved = account(USER_ID, NEW_GITHUB_ID, "new-login");
        when(gitHubRestClient.getUser(any(), eq("new-login")))
                .thenReturn(new GitHubUser(NEW_GITHUB_ID, "new-login", "New", "avatar2", "profile2", null));
        when(externalAccountRepository.findByProviderAndExternalUserId(IntegrationProvider.GITHUB, String.valueOf(NEW_GITHUB_ID)))
                .thenReturn(Optional.empty());
        when(externalAccountRepository.findByUserIdAndProvider(USER_ID, IntegrationProvider.GITHUB))
                .thenReturn(Optional.of(existing));
        when(externalAccountRepository.saveAndFlush(existing)).thenReturn(saved);

        GitHubAccountLinkResponse response = service.linkAccount(
                PROJECT_ID, USER_ID, new GitHubAccountLinkRequest(String.valueOf(NEW_GITHUB_ID), "new-login"));

        assertThat(response.externalAccountId()).isEqualTo(String.valueOf(NEW_GITHUB_ID));
        verify(commitRepository).backfillAuthorExternalAccountId(PROJECT_ID, NEW_GITHUB_ID, saved.getId());
        verify(pullRequestRepository).backfillAuthorExternalAccountId(PROJECT_ID, NEW_GITHUB_ID, saved.getId());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    private IntegrationConfig config() {
        IntegrationConfig config = new IntegrationConfig(PROJECT_ID, IntegrationProvider.GITHUB, "encrypted");
        config.setAccountIdentifier("owner/repository");
        return config;
    }

    private UserExternalAccount account(long userId, long githubId, String login) {
        UserExternalAccount account = org.mockito.Mockito.mock(UserExternalAccount.class);
        lenient().when(account.getId()).thenReturn(githubId + 10000L);
        lenient().when(account.getUserId()).thenReturn(userId);
        lenient().when(account.getExternalUserId()).thenReturn(String.valueOf(githubId));
        lenient().when(account.getExternalLogin()).thenReturn(login);
        lenient().when(account.getAvatarUrl()).thenReturn("avatar");
        lenient().when(account.getProfileUrl()).thenReturn("profile");
        return account;
    }

    private void verifyNoGitHubSideEffects() {
        verify(gitHubRestClient, never()).getUser(any(), any());
        verify(activityLogRepository, never()).save(any());
    }
}
