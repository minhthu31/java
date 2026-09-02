package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GitHubPersistenceRepositoryTests {

    private static final long GROUP_ID = 9890L;
    private static final long PROJECT_ID = 9891L;
    private static final long OTHER_PROJECT_ID = 9892L;
    private static final long TASK_ID = 9893L;

    @Autowired private GitHubIntegrationConfigRepository configRepository;
    @Autowired private GitHubRepositoryRepository repositoryRepository;
    @Autowired private GitHubCommitRepository commitRepository;
    @Autowired private GitHubPullRequestRepository pullRequestRepository;
    @Autowired private UserExternalAccountRepository externalAccountRepository;
    @Autowired private TaskCommitLinkRepository taskCommitLinkRepository;
    @Autowired private TaskPullRequestLinkRepository taskPullRequestLinkRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long memberUserId;

    @BeforeEach
    void setUp() {
        memberUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'member.test'", Long.class);

        jdbcTemplate.update("DELETE FROM task_pr_links");
        jdbcTemplate.update("DELETE FROM task_commit_links");
        jdbcTemplate.update("DELETE FROM github_pull_requests");
        jdbcTemplate.update("DELETE FROM github_commits");
        jdbcTemplate.update("DELETE FROM github_repositories");
        jdbcTemplate.update("DELETE FROM user_external_accounts");
        jdbcTemplate.update("DELETE FROM integration_configs");
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", TASK_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id IN (?, ?)", PROJECT_ID, OTHER_PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("""
                MERGE INTO student_groups (id, code, name)
                KEY(id)
                VALUES (?, ?, ?)
                """, GROUP_ID, "CNPM-89-TEST", "CNPM 89 Test Group");
        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 89 Project");
        jdbcTemplate.update(
                "INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                OTHER_PROJECT_ID, GROUP_ID, "Other CNPM 89 Project");
        jdbcTemplate.update("""
                INSERT INTO tasks (
                    id, project_id, title, acceptance_criteria,
                    issue_type, priority, status, sync_status)
                VALUES (?, ?, 'GitHub persistence task', 'Repository test',
                    'TASK', 'MEDIUM', 'TO_DO', 'NOT_SYNCED')
                """, TASK_ID, PROJECT_ID);
    }

    @Test
    void flywayCreatedGitHubTables() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                WHERE LOWER(TABLE_NAME) IN (
                    'github_repositories', 'github_commits', 'github_pull_requests',
                    'user_external_accounts', 'task_commit_links', 'task_pr_links')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(6);
    }

    @Test
    void savesAndFindsGitHubConfigByProject() {
        IntegrationConfig config = new IntegrationConfig(
                PROJECT_ID, IntegrationProvider.GITHUB, "enc:test-token");
        config.setBaseUrl("https://api.github.com");
        config.setAccountIdentifier("student-dev");

        configRepository.saveAndFlush(config);

        assertThat(configRepository.findGitHubConfigByProjectId(PROJECT_ID))
                .isPresent()
                .get()
                .extracting(IntegrationConfig::getProvider)
                .isEqualTo(IntegrationProvider.GITHUB);
    }

    @Test
    void findsRepositoryByProjectAndGitHubRepositoryId() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));

        assertThat(repositoryRepository.findByProjectIdOrderByFullNameAsc(PROJECT_ID))
                .containsExactly(repository);
        assertThat(repositoryRepository.findByGithubRepositoryId(12345L)).contains(repository);
        assertThat(repositoryRepository.findByProjectIdAndGithubRepositoryId(PROJECT_ID, 12345L))
                .contains(repository);
    }

    @Test
    void commitShaIsUniqueInsideRepository() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        commitRepository.saveAndFlush(commit(repository.getId(), "abc123", null));

        assertThatThrownBy(() -> commitRepository.saveAndFlush(commit(repository.getId(), "abc123", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pullRequestNumberIsUniqueInsideRepository() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        pullRequestRepository.saveAndFlush(pullRequest(repository.getId(), 15, null));

        assertThatThrownBy(() -> pullRequestRepository.saveAndFlush(pullRequest(repository.getId(), 15, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsCommitAndPullRequestByRepositoryKeys() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        GitHubCommit commit = commitRepository.saveAndFlush(commit(repository.getId(), "def456", null));
        GitHubPullRequest pr = pullRequestRepository.saveAndFlush(pullRequest(repository.getId(), 20, null));

        assertThat(commitRepository.findByRepositoryIdAndSha(repository.getId(), "def456")).contains(commit);
        assertThat(pullRequestRepository.findByRepositoryIdAndNumber(repository.getId(), 20)).contains(pr);
    }

    @Test
    void getsActivityByProjectAndMember() {
        UserExternalAccount account = externalAccountRepository.saveAndFlush(
                new UserExternalAccount(memberUserId, IntegrationProvider.GITHUB, "gh-user-89", "member89"));

        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        GitHubRepository otherRepository = repositoryRepository.saveAndFlush(
                repository(OTHER_PROJECT_ID, 67890L, "team/other"));

        GitHubCommit memberCommit = commitRepository.saveAndFlush(commit(repository.getId(), "member-sha", account.getId()));
        GitHubCommit otherProjectCommit = commitRepository.saveAndFlush(
                commit(otherRepository.getId(), "outside-sha", account.getId()));
        GitHubPullRequest memberPr = pullRequestRepository.saveAndFlush(
                pullRequest(repository.getId(), 21, account.getId()));

        assertThat(commitRepository.findActivityByProjectId(PROJECT_ID)).containsExactly(memberCommit);
        assertThat(commitRepository.findActivityByProjectIdAndUserId(PROJECT_ID, memberUserId))
                .containsExactly(memberCommit)
                .doesNotContain(otherProjectCommit);
        assertThat(pullRequestRepository.findActivityByProjectIdAndUserId(PROJECT_ID, memberUserId))
                .containsExactly(memberPr);
    }

    @Test
    void taskCommitLinkCannotBeDuplicated() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        GitHubCommit commit = commitRepository.saveAndFlush(commit(repository.getId(), "link-sha", null));
        TaskCommitLinkId id = new TaskCommitLinkId(TASK_ID, commit.getId());

        taskCommitLinkRepository.saveAndFlush(new TaskCommitLink(id, "AUTO"));
        assertThat(taskCommitLinkRepository.existsById(id)).isTrue();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO task_commit_links (task_id, commit_id, link_source) VALUES (?, ?, 'AUTO')",
                TASK_ID, commit.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void taskPullRequestLinkCannotBeDuplicated() {
        GitHubRepository repository = repositoryRepository.saveAndFlush(repository(PROJECT_ID, 12345L, "team/demo"));
        GitHubPullRequest pr = pullRequestRepository.saveAndFlush(pullRequest(repository.getId(), 22, null));
        TaskPullRequestLinkId id = new TaskPullRequestLinkId(TASK_ID, pr.getId());

        taskPullRequestLinkRepository.saveAndFlush(new TaskPullRequestLink(id, "AUTO"));
        assertThat(taskPullRequestLinkRepository.existsById(id)).isTrue();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO task_pr_links (task_id, pull_request_id, link_source) VALUES (?, ?, 'AUTO')",
                TASK_ID, pr.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private GitHubRepository repository(long projectId, long githubId, String fullName) {
        GitHubRepository repository = new GitHubRepository(
                projectId, githubId, fullName, "main", "https://github.com/" + fullName);
        repository.setLastSyncedAt(Instant.parse("2026-09-01T12:00:00Z"));
        return repository;
    }

    private GitHubCommit commit(long repositoryId, String sha, Long accountId) {
        GitHubCommit commit = new GitHubCommit(
                repositoryId,
                sha,
                "CNPM-89 persistence",
                Instant.parse("2026-09-01T12:30:00Z"),
                "https://github.com/team/demo/commit/" + sha);
        commit.setAuthorExternalAccountId(accountId);
        return commit;
    }

    private GitHubPullRequest pullRequest(long repositoryId, int number, Long accountId) {
        GitHubPullRequest pr = new GitHubPullRequest(
                repositoryId,
                number,
                "CNPM-89 persistence",
                "feature/CNPM-89-github-persistence",
                "main",
                "OPEN",
                "https://github.com/team/demo/pull/" + number);
        pr.setAuthorExternalAccountId(accountId);
        return pr;
    }
}
