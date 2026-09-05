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
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskCommitLinkRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.TaskPullRequestLinkRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GitHubActivityIntegrationTests {

    private static final long GROUP_ID = 9890L;
    private static final long PROJECT_ID = 9891L;
    private static final long TASK_ID = 9893L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/github";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private GitHubRepositoryRepository repositoryRepository;
    @Autowired private GitHubCommitRepository commitRepository;
    @Autowired private GitHubPullRequestRepository pullRequestRepository;
    @Autowired private TaskCommitLinkRepository commitLinkRepository;
    @Autowired private TaskPullRequestLinkRepository pullRequestLinkRepository;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    private GitHubRepository savedRepo;
    private GitHubCommit savedCommit;
    private GitHubPullRequest savedPr;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM task_pr_links");
        jdbcTemplate.update("DELETE FROM task_commit_links");
        jdbcTemplate.update("DELETE FROM github_pull_requests");
        jdbcTemplate.update("DELETE FROM github_commits");
        jdbcTemplate.update("DELETE FROM github_repositories");
        jdbcTemplate.update("DELETE FROM integration_configs WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM tasks WHERE id = ?", TASK_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);

        jdbcTemplate.update("MERGE INTO student_groups (id, code, name) KEY(id) VALUES (?, ?, ?)",
                GROUP_ID, "CNPM-98-TEST", "CNPM 98 Test Group");
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (?, ?, ?)",
                PROJECT_ID, GROUP_ID, "CNPM 98 Project");
        jdbcTemplate.update("""
                INSERT INTO tasks (id, project_id, title, acceptance_criteria, issue_type, priority, status, sync_status)
                VALUES (?, ?, 'Task 98 Activity', 'Test criteria', 'TASK', 'MEDIUM', 'TO_DO', 'NOT_SYNCED')
                """, TASK_ID, PROJECT_ID);

        savedRepo = repositoryRepository.saveAndFlush(new GitHubRepository(
                PROJECT_ID, 12345L, "minhthu31/java-backend", "main", "https://github.com/minhthu31/java-backend"));

        savedCommit = new GitHubCommit(savedRepo.getId(), "sha123456", "feat(CNPM-98): test commit activity",
                Instant.parse("2026-09-02T10:00:00Z"), "https://github.com/minhthu31/java-backend/commit/sha123456");
        savedCommit = commitRepository.saveAndFlush(savedCommit);

        savedPr = new GitHubPullRequest(
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
        savedPr = pullRequestRepository.saveAndFlush(savedPr);

        commitLinkRepository.saveAndFlush(new TaskCommitLink(new TaskCommitLinkId(TASK_ID, savedCommit.getId()), "AUTO"));
        pullRequestLinkRepository.saveAndFlush(new TaskPullRequestLink(new TaskPullRequestLinkId(TASK_ID, savedPr.getId()), "AUTO"));
    }

    @Test
    @DisplayName("1. Lấy danh sách commit theo repository có pagination và filter issueKey thành công")
    void listCommits_shouldReturnPagedData() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/repositories/{repositoryId}/commits", PROJECT_ID, savedRepo.getId())
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("issueKey", "CNPM-98")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].sha").value("sha123456"))
                .andExpect(jsonPath("$.data.content[0].issueKeys[0]").value("CNPM-98"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("2. Lấy danh sách Pull Request có filter state và pagination thành công")
    void listPullRequests_shouldReturnPagedData() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/repositories/{repositoryId}/pull-requests", PROJECT_ID, savedRepo.getId())
                        .with(user("member").roles("TEAM_MEMBER"))
                        .param("state", "OPEN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].number").value(10))
                .andExpect(jsonPath("$.data.content[0].state").value("OPEN"));
    }

    @Test
    @DisplayName("3. Lấy Unified Activities và lọc theo Task hoạt động chính xác")
    void listTaskActivities_shouldReturnCommitAndPr() throws Exception {
        when(projectAuthorization.canViewTask(PROJECT_ID, TASK_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/tasks/{taskId}/activities", PROJECT_ID, TASK_ID)
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("4. Quyền RBAC: Người dùng không thuộc Project bị chặn 403 Forbidden")
    void nonProjectMember_shouldBeForbidden() throws Exception {
        when(projectAuthorization.canViewTasks(PROJECT_ID)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/activities", PROJECT_ID)
                        .with(user("outsider").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }
}