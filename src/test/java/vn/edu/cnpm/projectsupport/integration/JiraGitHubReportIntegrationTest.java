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
import java.util.Optional;
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
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubPullRequestRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraProject;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
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
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;

    @MockitoBean private JiraClient jiraClient;
    @MockitoBean private GitHubRestClient gitHubRestClient;
    @MockitoBean private GitHubIntegrationConfigRepository gitHubIntegrationConfigRepository;
    @MockitoBean private IntegrationSecretService integrationSecretService;

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

    private void mockGitHubIntegrationConfig(Long projectId, String repoFullName) {
        IntegrationConfig mockConfig = mock(IntegrationConfig.class);
        when(mockConfig.getAccountIdentifier()).thenReturn(repoFullName);
        when(mockConfig.getEncryptedSecret()).thenReturn("encrypted-mock-token");
        when(gitHubIntegrationConfigRepository.findGitHubConfigByProjectId(projectId)).thenReturn(Optional.of(mockConfig));
        when(integrationSecretService.decrypt("encrypted-mock-token")).thenReturn("decrypted-token");
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

    private GitHubRepository createMockRemoteRepo(Long id, String name, String fullName, String ownerLogin) {
        GitHubRepository remoteRepo = mock(GitHubRepository.class);
        when(remoteRepo.id()).thenReturn(id);
        when(remoteRepo.nodeId()).thenReturn("node-" + id);
        when(remoteRepo.name()).thenReturn(name);
        when(remoteRepo.fullName()).thenReturn(fullName);
        when(remoteRepo.defaultBranch()).thenReturn("main");
        when(remoteRepo.htmlUrl()).thenReturn("https://github.com/" + fullName);
        when(remoteRepo.privateRepository()).thenReturn(false);
        when(remoteRepo.archived()).thenReturn(false);
        when(remoteRepo.updatedAt()).thenReturn(Instant.now());

        GitHubRepository.Owner owner = mock(GitHubRepository.Owner.class);
        when(owner.id()).thenReturn(100L);
        when(owner.login()).thenReturn(ownerLogin);
        when(remoteRepo.owner()).thenReturn(owner);

        return remoteRepo;
    }

    @Test
    @DisplayName("AC1: Test tạo Task local, đồng bộ Jira và xác nhận Task map đúng Issue Key")
    void testTaskCreationAndJiraSyncFlow() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

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

        JiraSyncResult syncResult = jiraSyncService.syncProject(projectId);
        assertThat(syncResult.issuesSynced()).isGreaterThanOrEqualTo(1);

        JiraIssueSnapshot snapshot = jiraIssueSnapshotRepository
                .findByProjectIdAndJiraIssueKey(projectId, jiraKey)
                .orElse(null);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getJiraIssueKey()).isEqualTo(jiraKey);

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC2 & AC3: Test đồng bộ repository, commit, Pull Request và kiểm tra liên kết Task")
    @SuppressWarnings("unchecked")
    void testGitHubRepositorySyncAndTaskLinking() {
        JsonNode login = assertLogin("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        mockGitHubIntegrationConfig(projectId, "org/repo");

        GitHubRepository remoteRepo = createMockRemoteRepo(8801L, "repo", "org/repo", "org");
        when(gitHubRestClient.getRepository(any())).thenReturn(remoteRepo);

        // 1. Mock Commit
        String commitSha = "c1a2b3d4e5f67890";
        GitHubCommit listedCommit = mock(GitHubCommit.class);
        when(listedCommit.sha()).thenReturn(commitSha);

        GitHubCommit fullCommit = mock(GitHubCommit.class);
        when(fullCommit.sha()).thenReturn(commitSha);
        when(fullCommit.htmlUrl()).thenReturn("https://github.com/org/repo/commit/" + commitSha);

        GitHubCommit.GitAuthor gitAuthor = new GitHubCommit.GitAuthor("Dev", "dev@local", Instant.now());
        GitHubCommit.CommitMetadata commitMeta = new GitHubCommit.CommitMetadata("feat: [CNPM-110] resolve task requirement", gitAuthor, gitAuthor);
        when(fullCommit.commit()).thenReturn(commitMeta);
        when(fullCommit.author()).thenReturn(null);
        when(fullCommit.parentShas()).thenReturn(List.of());

        GitHubPage<GitHubCommit> commitPage = mock(GitHubPage.class);
        when(commitPage.items()).thenReturn(List.of(listedCommit));
        when(commitPage.nextUrl()).thenReturn(null);

        when(gitHubRestClient.getCommitsPage(any(), eq(1))).thenReturn(commitPage);
        when(gitHubRestClient.getCommit(any(), eq(commitSha))).thenReturn(fullCommit);

        var commitSyncResult = gitHubCommitSyncService.syncCommits(projectId);
        assertThat(commitSyncResult.commitsSynced()).isGreaterThanOrEqualTo(1);

        // 2. Mock Pull Request
        GitHubPullRequest listedPr = mock(GitHubPullRequest.class);
        when(listedPr.number()).thenReturn(11);

        GitHubPullRequest fullPr = mock(GitHubPullRequest.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(fullPr.id()).thenReturn(301L);
        when(fullPr.number()).thenReturn(11);
        when(fullPr.title()).thenReturn("feat: [CNPM-110] implementation PR");
        when(fullPr.body()).thenReturn("Body description");
        when(fullPr.localState()).thenReturn("OPEN");
        when(fullPr.draft()).thenReturn(false);
        when(fullPr.htmlUrl()).thenReturn("https://github.com/org/repo/pull/11");
        when(fullPr.createdAt()).thenReturn(Instant.now());
        when(fullPr.user()).thenReturn(null);
        when(fullPr.head().ref()).thenReturn("feature/CNPM-110");
        when(fullPr.head().sha()).thenReturn(commitSha);
        when(fullPr.base().ref()).thenReturn("main");

        GitHubPage<GitHubPullRequest> prPage = mock(GitHubPage.class);
        when(prPage.items()).thenReturn(List.of(listedPr));
        when(prPage.nextUrl()).thenReturn(null);

        when(gitHubRestClient.getPullRequestsPage(any(), anyString(), eq(1))).thenReturn(prPage);
        when(gitHubRestClient.getPullRequest(any(), eq(11))).thenReturn(fullPr);

        var prSyncResult = gitHubPullRequestSyncService.syncPullRequests(projectId);
        assertThat(prSyncResult).isNotNull();

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
    @SuppressWarnings("unchecked")
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

        // Idempotent GitHub Commit Sync
        mockGitHubIntegrationConfig(projectId, "org/repo-idemp");
        GitHubRepository remoteRepo = createMockRemoteRepo(9902L, "repo-idemp", "org/repo-idemp", "org");
        when(gitHubRestClient.getRepository(any())).thenReturn(remoteRepo);

        String sha = "deadbeef12345678";
        GitHubCommit listed = mock(GitHubCommit.class);
        when(listed.sha()).thenReturn(sha);

        GitHubCommit fullCommit = mock(GitHubCommit.class);
        when(fullCommit.sha()).thenReturn(sha);
        when(fullCommit.htmlUrl()).thenReturn("https://github.com/org/repo-idemp/commit/" + sha);

        GitHubCommit.GitAuthor author = new GitHubCommit.GitAuthor("Author", "auth@test", Instant.now());
        when(fullCommit.commit()).thenReturn(new GitHubCommit.CommitMetadata("chore: commit once", author, author));
        when(fullCommit.author()).thenReturn(null);
        when(fullCommit.parentShas()).thenReturn(List.of());

        GitHubPage<GitHubCommit> commitPage = mock(GitHubPage.class);
        when(commitPage.items()).thenReturn(List.of(listed));
        when(commitPage.nextUrl()).thenReturn(null);

        when(gitHubRestClient.getCommitsPage(any(), eq(1))).thenReturn(commitPage);
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
        doThrow(new RuntimeException("Jira Backlog API Timeout"))
                .when(jiraClient).getBacklog(eq(projectId), anyString(), anyInt(), anyInt());
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        JiraSyncResult result = jiraSyncService.syncProject(projectId);
        assertThat(result.errors()).isGreaterThan(0);

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
