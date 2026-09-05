package vn.edu.cnpm.projectsupport.integration.github;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLinkId;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.UserExternalAccountRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GitHubActivityIntegrationTests {

    private static final long GROUP_ID = 9890L;
    private static final long PROJECT_ID = 9891L;
    private static final long TASK_98_ID = 9893L;
    private static final long TASK_9_ID = 9894L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/github";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private GitHubRepositoryRepository repositoryRepository;
    @Autowired private GitHubCommitRepository commitRepository;
    @Autowired private GitHubPullRequestRepository pullRequestRepository;
    @Autowired private TaskCommitLinkRepository commitLinkRepository;
    @Autowired private TaskPullRequestLinkRepository pullRequestLinkRepository;
    @Autowired private UserExternalAccountRepository externalAccountRepository;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    private Long memberUserId;
    private GitHubRepository savedRepo;
    private GitHubCommit savedCommit98;
    private GitHubCommit savedCommit9;
    private GitHubPullRequest savedPr98;

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
        jdbcTemplate.update("DELETE FROM jira_issues");
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM tasks WHERE id IN (?, ?)", TASK_98_ID, TASK_9_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("MERGE INTO student_groups (id, code, name) KEY(id) VALUES (?, ?, ?)",
                GROUP_ID, "CNPM-98-TEST", "CNPM 98 Test Group");
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 98 Project");

        jdbcTemplate.update("""
                INSERT INTO tasks (id, project_id, title, acceptance_criteria, issue_type, priority, status, sync_status)
                VALUES (?, ?, 'Task 98 Activity', 'Test criteria', 'TASK', 'MEDIUM', 'TO_DO', 'NOT_SYNCED')
                """, TASK_98_ID, PROJECT_ID);
        jdbcTemplate.update("""
                INSERT INTO tasks (id, project_id, title, acceptance_criteria, issue_type, priority, status, sync_status)
                VALUES (?, ?, 'Task 9 Activity', 'Test criteria', 'TASK', 'MEDIUM', 'TO_DO', 'NOT_SYNCED')
                """, TASK_9_ID, PROJECT_ID);

        jdbcTemplate.update("INSERT INTO jira_issues (task_id, jira_issue_key, sync_status) VALUES (?, 'CNPM-98', 'SYNCED')", TASK_98_ID);
        jdbcTemplate.update("INSERT INTO jira_issues (task_id, jira_issue_key, sync_status) VALUES (?, 'CNPM-9', 'SYNCED')", TASK_9_ID);

        UserExternalAccount account = externalAccountRepository.saveAndFlush(
                new UserExternalAccount(memberUserId, IntegrationProvider.GITHUB, "gh-actor-98", "member98"));

        savedRepo = repositoryRepository.saveAndFlush(new GitHubRepository(
                PROJECT_ID, 12345L, "minhthu31/java-backend", "main", "https://github.com/minhthu31/java-backend"));

        savedCommit98 = new GitHubCommit(savedRepo.getId(), "sha98", "feat(CNPM-98): test commit activity",
                Instant.parse("2026-09-02T10:00:00Z"), "https://github.com/minhthu31/java-backend/commit/sha98");
        savedCommit98.setAuthorExternalAccountId(account.getId());
        savedCommit98 = commitRepository.saveAndFlush(savedCommit98);

        savedCommit9 = new GitHubCommit(savedRepo.getId(), "sha9", "feat(CNPM-9): test commit nine",
                Instant.parse("2026-09-02T11:00:00Z"), "https://github.com/minhthu31/java-backend/commit/sha9");
        savedCommit9.setAuthorExternalAccountId(account.getId());
        savedCommit9 = commitRepository.saveAndFlush(savedCommit9);

        savedPr98 = new GitHubPullRequest(
                savedRepo.getId(),
                5001L,
                10,
                "feat(CNPM-98): test PR activity",
                "Body referencing CNPM-98",
                "feature/CNPM-98-test",
                "headsha",
                "main",
                GitHubPullRequestState.OPEN,
                false,
                null,
                null,
                1,
                5,
                2,
                1,
                null,
                "https://github.com/minhthu31/java-backend/pull/10");
        savedPr98.setAuthorExternalAccountId(account.getId());
        savedPr98 = pullRequestRepository.saveAndFlush(savedPr98);

        commitLinkRepository.saveAndFlush(new TaskCommitLink(new TaskCommitLinkId(TASK_98_ID, savedCommit98.getId()), "AUTO"));
        commitLinkRepository.saveAndFlush(new TaskCommitLink(new TaskCommitLinkId(TASK_9_ID, savedCommit9.getId()), "AUTO"));
        pullRequestLinkRepository.saveAndFlush(new TaskPullRequestLink(new TaskPullRequestLinkId(TASK_98_ID, savedPr98.getId()), "AUTO"));
    }

    @Test
    @DisplayName("1. Lọc exact issueKey CNPM-9 không bị ăn sang CNPM-98")
    void listCommits_exactIssueKey_shouldNotIncludeOthers() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/repositories/{repositoryId}/commits", PROJECT_ID, savedRepo.getId())
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("issueKey", "CNPM-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].sha").value("sha9"));
    }

    @Test
    @DisplayName("2. actorUserId lọc và trả về ID user local, không phải authorExternalAccountId")
    void listActivities_actorUserId_shouldMapToLocalUserId() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/activities", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("actorUserId", String.valueOf(memberUserId))
                        .param("type", "COMMIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].actorUserId").value(memberUserId));
    }

    @Test
    @DisplayName("3. Validate from > to và type sai phải trả 400 Bad Request")
    void listActivities_invalidParams_shouldReturnBadRequest() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        // from > to
        mockMvc.perform(get(BASE_URL + "/activities", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("from", "2026-09-05T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // type sai
        mockMvc.perform(get(BASE_URL + "/activities", PROJECT_ID)
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. Giới hạn page size tối đa 100")
    void listCommits_sizeExceeding100_shouldReturnBadRequest() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/repositories/{repositoryId}/commits", PROJECT_ID, savedRepo.getId())
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}