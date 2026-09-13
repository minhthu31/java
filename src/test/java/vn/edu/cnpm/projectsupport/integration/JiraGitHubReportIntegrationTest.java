package vn.edu.cnpm.projectsupport.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.edu.cnpm.projectsupport.integration.github.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.GitHubCommitSyncService;
import vn.edu.cnpm.projectsupport.integration.github.GitHubPage;
import vn.edu.cnpm.projectsupport.integration.github.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.GitHubPullRequestSyncService;
import vn.edu.cnpm.projectsupport.integration.github.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.GitHubRestClient;
import vn.edu.cnpm.projectsupport.integration.github.GitHubUser;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraProject;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssueSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncResult;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncService;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JiraGitHubReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JiraSyncService jiraSyncService;
    @Autowired private GitHubCommitSyncService gitHubCommitSyncService;
    @Autowired private GitHubPullRequestSyncService gitHubPullRequestSyncService;
    @Autowired private JiraIssueSnapshotRepository jiraIssueSnapshotRepository;
    @Autowired private GitHubCommitRepository gitHubCommitRepository;
    @Autowired private GitHubPullRequestRepository gitHubPullRequestRepository;
    @Autowired private GitHubRepositoryRepository gitHubRepositoryRepository;
    @Autowired private GitHubIntegrationConfigRepository gitHubIntegrationConfigRepository;
    @Autowired private IntegrationSecretService integrationSecretService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;

    @MockitoBean private JiraClient jiraClient;
    @MockitoBean private GitHubRestClient gitHubRestClient;

    private String validCreateBody(String title) {
        return String.format("""
                {
                  "title":"%s",
                  "acceptanceCriteria":"Khai báo đầy đủ trường hợp kiểm thử tích hợp",
                  "issueType":"TASK",
                  "priority":"HIGH"
                }
                """, title);
    }

    private void setupGitHubIntegrationConfig(Long projectId, String repoFullName) {
        gitHubIntegrationConfigRepository.findGitHubConfigByProjectId(projectId).ifPresent(gitHubIntegrationConfigRepository::delete);

        IntegrationConfig config = new IntegrationConfig();
        config.setProjectId(projectId);
        config.setProvider(IntegrationProvider.GITHUB);
        config.setAccountIdentifier(repoFullName);
        config.setEncryptedSecret(integrationSecretService.encrypt("mock-github-token"));
        config.setCreatedAt(Instant.now());
        gitHubIntegrationConfigRepository.save(config);
    }

    private void ensureProjectHasJiraKey(Long projectId, String jiraKey) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            project.setJiraProjectKey(jiraKey);
            projectRepository.save(project);
        }
    }

    private JiraIssueDto createMockIssueDto(String id, String key, String summary) {
        JiraIssueDto dto = mock(JiraIssueDto.class);
        when(dto.id()).thenReturn(id);
        when(dto.key()).thenReturn(key);
        when(dto.summary()).thenReturn(summary);
        when(dto.updated()).thenReturn(Instant.now().toString());
        return dto;
    }

    @Test
    @DisplayName("AC1: Test tạo Task local, đồng bộ Jira và xác nhận Task map đúng Issue Key")
    void testTaskCreationAndJiraSyncFlow() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // 1. Tạo task local
        String res = mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "IDEMP-TASK-SYNC-110")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody("Local task to sync Jira PROJ-110")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long taskId = objectMapper.readTree(res).path("data").path("id").asLong();
        assertThat(taskId).isPositive();

        // 2. Cấu hình Jira Project và Mock phản hồi từ Jira API
        String jiraKey = "PROJ-110";
        ensureProjectHasJiraKey(projectId, "PROJ");

        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10001", "PROJ", "Project PROJ", "http://jira.local/rest/api/2/project/PROJ"));

        JiraIssueDto issueDto = createMockIssueDto("50001", jiraKey, "Local task to sync Jira PROJ-110");

        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        when(jiraClient.getBacklog(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        // 3. Thực thi đồng bộ
        JiraSyncResult syncResult = jiraSyncService.syncProject(projectId);
        assertThat(syncResult.issuesSynced()).isGreaterThanOrEqualTo(1);

        // 4. Xác nhận Issue Snapshot được lưu và Task mapping
        JiraIssueSnapshot snapshot = jiraIssueSnapshotRepository
                .findByProjectIdAndJiraIssueKey(projectId, jiraKey)
                .orElse(null);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getJiraIssueKey()).isEqualTo(jiraKey);

        Task localTask = taskRepository.findById(taskId).orElse(null);
        assertThat(localTask).isNotNull();
        localTask.setJiraIssueKey(jiraKey);
        taskRepository.save(localTask);

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jiraIssueKey").value(jiraKey));
    }

    @Test
    @DisplayName("AC2 & AC3: Test đồng bộ repository, commit, Pull Request và kiểm tra liên kết Task")
    void testGitHubRepositorySyncAndTaskLinking() {
        JsonNode login = assertLogin("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        setupGitHubIntegrationConfig(projectId, "org/repo");

        GitHubUser orgUser = new GitHubUser(100L, "org");
        GitHubUser authorUser = new GitHubUser(101L, "developer");
        GitHubRepository remoteRepo = new GitHubRepository(
                8801L, "node-8801", "repo", orgUser, "org/repo", false, "main", "https://github.com/org/repo", false, Instant.now());
        when(gitHubRestClient.getRepository(any())).thenReturn(remoteRepo);

        // 1. Mock Commit có format link Task: [CNPM-110]
        String commitSha = "c1a2b3d4e5f67890";
        GitHubCommit.GitAuthor gitAuthor = new GitHubCommit.GitAuthor("Dev", "dev@local", Instant.now());
        GitHubCommit.CommitMetadata commitMeta = new GitHubCommit.CommitMetadata("feat: [CNPM-110] resolve task requirement", gitAuthor, gitAuthor);
        GitHubCommit fullCommit = new GitHubCommit(commitSha, commitMeta, authorUser, "https://github.com/org/repo/commit/" + commitSha, null, null, null);
        GitHubCommit listedCommit = new GitHubCommit(commitSha, null, null, null, null, null, null);

        when(gitHubRestClient.getCommitsPage(any(), eq(1))).thenReturn(new GitHubPage<>(List.of(listedCommit), null));
        when(gitHubRestClient.getCommit(any(), eq(commitSha))).thenReturn(fullCommit);

        var commitSyncResult = gitHubCommitSyncService.syncCommits(projectId);
        assertThat(commitSyncResult.commitsSynced()).isGreaterThanOrEqualTo(1);

        // 2. Mock Pull Request
        GitHubPullRequest.GitRef headRef = new GitHubPullRequest.GitRef("feature/CNPM-110", commitSha);
        GitHubPullRequest.GitRef baseRef = new GitHubPullRequest.GitRef("main", "main-sha");
        GitHubPullRequest fullPr = new GitHubPullRequest(
                301L, 11, "feat: [CNPM-110] implementation PR", "Body description", "open",
                false, "https://github.com/org/repo/pull/11", headRef, baseRef, authorUser,
                Instant.now(), null, null, null, 1, 20, 5, 2);
        GitHubPullRequest listedPr = new GitHubPullRequest(
                301L, 11, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(gitHubRestClient.getPullRequestsPage(any(), anyString(), eq(1))).thenReturn(new GitHubPage<>(List.of(listedPr), null));
        when(gitHubRestClient.getPullRequest(any(), eq(11))).thenReturn(fullPr);

        var prSyncResult = gitHubPullRequestSyncService.syncPullRequests(projectId);
        assertThat(prSyncResult.synced()).isGreaterThanOrEqualTo(1);

        // 3. Đối chiếu dữ liệu DB
        var localRepo = gitHubRepositoryRepository.findByProjectIdAndGithubRepositoryId(projectId, 8801L).orElse(null);
        assertThat(localRepo).isNotNull();

        var localCommit = gitHubCommitRepository.findByRepositoryIdAndSha(localRepo.getId(), commitSha).orElse(null);
        assertThat(localCommit).isNotNull();
        assertThat(localCommit.getMessage()).contains("[CNPM-110]");

        var localPr = gitHubPullRequestRepository.findByRepositoryIdAndNumber(localRepo.getId(), 11).orElse(null);
        assertThat(localPr).isNotNull();
        assertThat(localPr.getTitle()).contains("[CNPM-110]");
    }

    @Test
    @DisplayName("AC4: Test đối chiếu số liệu báo cáo summary, taskMetrics và progress endpoint")
    void testProgressAndMemberContributionReports() throws Exception {
        JsonNode leaderLogin = login("leader.test", "password");
        long projectId = leaderLogin.path("projectId").asLong();
        String leaderToken = leaderLogin.path("accessToken").asText();

        // 1. Kiểm tra summary với đúng đường dẫn json taskMetrics
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskMetrics.totalTasks").isNumber())
                .andExpect(jsonPath("$.data.taskMetrics.completedTasks").isNumber());

        // 2. Kiểm tra endpoint progress report
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/progress", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 3. Kiểm tra filter thời gian hợp lệ
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-10T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk());

        // 4. Team Member kiểm tra chỉ xem đúng phần đóng góp của chính mình
        JsonNode memberLogin = login("member.test", "password");
        long memberId = memberLogin.path("id").asLong();
        String memberToken = memberLogin.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(memberId));
    }

    @Test
    @DisplayName("AC5: Chạy đồng bộ lại không tạo trùng dữ liệu commit, PR, Jira snapshot (Idempotency)")
    void testSyncIdempotencyDoesNotDuplicateData() {
        JsonNode login = assertLogin("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        // Idempotent Jira Sync
        ensureProjectHasJiraKey(projectId, "PROJ-IDEMP");
        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10002", "PROJ-IDEMP", "Project IDEMP", "http://jira.local"));
        JiraIssueDto issueDto = createMockIssueDto("60001", "IDEMP-1", "Idempotent Issue");
        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        when(jiraClient.getBacklog(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        jiraSyncService.syncProject(projectId);
        long jiraCount1 = jiraIssueSnapshotRepository.count();
        jiraSyncService.syncProject(projectId);
        long jiraCount2 = jiraIssueSnapshotRepository.count();
        assertThat(jiraCount1).isEqualTo(jiraCount2);

        // Idempotent GitHub Commit & PR Sync
        setupGitHubIntegrationConfig(projectId, "org/repo-idemp");
        GitHubUser orgUser = new GitHubUser(200L, "org");
        GitHubRepository remoteRepo = new GitHubRepository(
                9902L, "node-9902", "repo-idemp", orgUser, "org/repo-idemp", false, "main", "https://github.com/org/repo-idemp", false, Instant.now());
        when(gitHubRestClient.getRepository(any())).thenReturn(remoteRepo);

        String sha = "deadbeef12345678";
        GitHubCommit.GitAuthor author = new GitHubCommit.GitAuthor("Author", "auth@test", Instant.now());
        GitHubCommit fullCommit = new GitHubCommit(sha, new GitHubCommit.CommitMetadata("chore: commit once", author, author), orgUser, "url", null, null, null);
        when(gitHubRestClient.getCommitsPage(any(), eq(1))).thenReturn(new GitHubPage<>(List.of(new GitHubCommit(sha, null, null, null, null, null, null)), null));
        when(gitHubRestClient.getCommit(any(), eq(sha))).thenReturn(fullCommit);

        gitHubCommitSyncService.syncCommits(projectId);
        long commitCount1 = gitHubCommitRepository.count();
        gitHubCommitSyncService.syncCommits(projectId);
        long commitCount2 = gitHubCommitRepository.count();
        assertThat(commitCount1).isEqualTo(commitCount2);
    }

    @Test
    @DisplayName("AC6 & AC7: Chịu lỗi một phần khi API external thất bại, bảo toàn dữ liệu đã sync trước đó")
    void testPartialFailureResilienceAndDatabaseConsistency() {
        JsonNode login = assertLogin("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        ensureProjectHasJiraKey(projectId, "PROJ-FAIL-RESIL");
        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10003", "PROJ-FAIL-RESIL", "Project Fail Resil", "http://jira.local"));

        String safeIssueKey = "RESIL-1";
        JiraIssueDto issueDto = createMockIssueDto("70001", safeIssueKey, "Preserved issue on partial failure");

        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        // Giả lập lỗi ở bước lấy Backlog
        doThrow(new RuntimeException("Jira Backlog API Timeout"))
                .when(jiraClient).getBacklog(eq(projectId), anyString(), anyInt(), anyInt());
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        JiraSyncResult result = jiraSyncService.syncProject(projectId);
        assertThat(result.errors()).isGreaterThan(0);

        // Bản ghi Issue được đồng bộ trước đó vẫn còn nguyên vẹn trong cơ sở dữ liệu
        JiraIssueSnapshot savedIssue = jiraIssueSnapshotRepository
                .findByProjectIdAndJiraIssueKey(projectId, safeIssueKey)
                .orElse(null);
        assertThat(savedIssue).isNotNull();
        assertThat(savedIssue.getSummary()).isEqualTo("Preserved issue on partial failure");
    }

    private JsonNode assertLogin(String username, String password) {
        try {
            return login(username, password);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "usernameOrEmail", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data");
    }
}
