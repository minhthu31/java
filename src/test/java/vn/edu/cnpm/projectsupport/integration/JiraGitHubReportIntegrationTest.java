package vn.edu.cnpm.projectsupport.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import vn.edu.cnpm.projectsupport.integration.github.GitHubClientConfig;
import vn.edu.cnpm.projectsupport.integration.github.GitHubCommitSyncService;
import vn.edu.cnpm.projectsupport.integration.github.GitHubPage;
import vn.edu.cnpm.projectsupport.integration.github.GitHubRestClient;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubCommitRepository;
import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubRepositoryRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraProject;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssueSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueFieldsDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncResult;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncService;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JiraGitHubReportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JiraSyncService jiraSyncService;
    @Autowired private GitHubCommitSyncService gitHubCommitSyncService;
    @Autowired private JiraIssueSnapshotRepository jiraIssueSnapshotRepository;
    @Autowired private GitHubCommitRepository gitHubCommitRepository;
    @Autowired private GitHubRepositoryRepository gitHubRepositoryRepository;
    @Autowired private ProjectRepository projectRepository;

    @MockitoBean private JiraClient jiraClient;
    @MockitoBean private GitHubRestClient gitHubRestClient;

    private String validCreateBody() {
        return """
                {
                  "title":"Xây dựng Task API contract và Controller",
                  "acceptanceCriteria":"Đủ CRUD, status và assignee",
                  "issueType":"TASK",
                  "priority":"HIGH"
                }
                """;
    }

    private void ensureProjectHasJiraKey(Long projectId, String jiraKey) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            project.setJiraProjectKey(jiraKey);
            projectRepository.save(project);
        }
    }

    @Test
    @DisplayName("AC1: Test luồng tạo Task local và đồng bộ lên Jira, kiểm tra Issue Key")
    void testTaskCreationAndJiraSyncFlow() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();
        String token = login.path("accessToken").asText();

        // 1. Tạo task local qua API
        String res = mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "IDEMP-KEY-AC1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdTask = objectMapper.readTree(res).path("data");
        long taskId = createdTask.path("id").asLong();
        assertThat(taskId).isPositive();

        // 2. Cấu hình Jira Project Key và giả lập response
        String jiraKey = "PROJ-101";
        ensureProjectHasJiraKey(projectId, "PROJ");

        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10001", "PROJ", "Project PROJ", "http://jira.local/rest/api/2/project/PROJ"));

        JiraIssueFieldsDto fields = new JiraIssueFieldsDto("Task for Jira Sync Test", null, null, null, Instant.now().toString());
        JiraIssueDto issueDto = new JiraIssueDto("20001", jiraKey, fields);

        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        when(jiraClient.getBacklog(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        // 3. Thực hiện đồng bộ Jira
        JiraSyncResult syncResult = jiraSyncService.syncProject(projectId);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.issuesSynced()).isGreaterThanOrEqualTo(1);

        // 4. Kiểm tra snapshot được lưu với đúng Issue Key
        JiraIssueSnapshot snapshot = jiraIssueSnapshotRepository
                .findByProjectIdAndJiraIssueKey(projectId, jiraKey)
                .orElse(null);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getJiraIssueKey()).isEqualTo(jiraKey);
        assertThat(snapshot.getSummary()).isEqualTo("Task for Jira Sync Test");
    }

    @Test
    @DisplayName("AC2 & AC3: Test đồng bộ repository, commit và liên kết Task")
    void testGitHubRepositorySyncAndTaskLinking() {
        JsonNode login = assertLogin("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        GitHubClientConfig config = new GitHubClientConfig("org", "repo", "mock-token", "2022-11-28", 5000);

        vn.edu.cnpm.projectsupport.integration.github.GitHubRepository remoteRepo =
                new vn.edu.cnpm.projectsupport.integration.github.GitHubRepository(
                        9901L, "node-9901", "repo",
                        new vn.edu.cnpm.projectsupport.integration.github.GitHubUser(501L, "org"),
                        "org/repo", false, "main", "https://github.com/org/repo", false, Instant.now());

        when(gitHubRestClient.getRepository(any(GitHubClientConfig.class))).thenReturn(remoteRepo);

        String commitSha = "abc1234567890def";
        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit listed =
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit(commitSha, null, null, null, null, 0, 0, 0, List.of());

        vn.edu.cnpm.projectsupport.integration.github.GitHubCommitDetail detail =
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommitDetail(
                        "feat: resolve [CNPM-61] link task to commit",
                        new vn.edu.cnpm.projectsupport.integration.github.GitHubCommitAuthor("Dev", "dev@local", Instant.now()),
                        null);

        vn.edu.cnpm.projectsupport.integration.github.GitHubCommit remoteCommit =
                new vn.edu.cnpm.projectsupport.integration.github.GitHubCommit(
                        commitSha, detail, "https://github.com/org/repo/commit/" + commitSha,
                        new vn.edu.cnpm.projectsupport.integration.github.GitHubUser(502L, "dev-user"),
                        null, 10, 2, 1, List.of());

        when(gitHubRestClient.getCommitsPage(any(GitHubClientConfig.class), eq(1)))
                .thenReturn(new GitHubPage<>(List.of(listed), null));
        when(gitHubRestClient.getCommit(any(GitHubClientConfig.class), eq(commitSha)))
                .thenReturn(remoteCommit);

        var syncResult = gitHubCommitSyncService.syncCommits(projectId, config);
        assertThat(syncResult).isNotNull();
        assertThat(syncResult.synced()).isGreaterThanOrEqualTo(1);

        GitHubRepository localRepo = gitHubRepositoryRepository.findByProjectIdAndGithubRepositoryId(projectId, 9901L).orElse(null);
        assertThat(localRepo).isNotNull();

        GitHubCommit localCommit = gitHubCommitRepository.findByRepositoryIdAndSha(localRepo.getId(), commitSha).orElse(null);
        assertThat(localCommit).isNotNull();
        assertThat(localCommit.getMessage()).contains("[CNPM-61]");
    }

    @Test
    @DisplayName("AC4: Test đối chiếu số liệu báo cáo tiến độ và đóng góp thành viên kèm bộ lọc")
    void testProgressAndMemberContributionReports() throws Exception {
        JsonNode leaderLogin = login("leader.test", "password");
        long projectId = leaderLogin.path("projectId").asLong();
        String leaderToken = leaderLogin.path("accessToken").asText();

        // 1. Đối chiếu số liệu báo cáo summary
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.totalTasks").isNumber())
                .andExpect(jsonPath("$.data.completedTasks").isNumber());

        // 2. Đối chiếu báo cáo với bộ lọc ngày hợp lệ
        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-10T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());

        // 3. Team Member xem đóng góp cá nhân
        JsonNode memberLogin = login("member.test", "password");
        long memberId = memberLogin.path("id").asLong();
        String memberToken = memberLogin.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/projects/{projectId}/reports/summary", projectId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(memberId));
    }

    @Test
    @DisplayName("AC5: Chạy đồng bộ lại không tạo trùng dữ liệu (Idempotency)")
    void testSyncIdempotencyDoesNotDuplicateData() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        ensureProjectHasJiraKey(projectId, "PROJ-IDEMP");
        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10002", "PROJ-IDEMP", "Project PROJ-IDEMP", "http://jira.local"));

        JiraIssueFieldsDto fields = new JiraIssueFieldsDto("Idempotent Issue Sync", null, null, null, Instant.now().toString());
        JiraIssueDto issueDto = new JiraIssueDto("30001", "PROJ-IDEMP-1", fields);

        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        when(jiraClient.getBacklog(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        jiraSyncService.syncProject(projectId);
        long countAfterFirstSync = jiraIssueSnapshotRepository.count();

        jiraSyncService.syncProject(projectId);
        long countAfterSecondSync = jiraIssueSnapshotRepository.count();

        assertThat(countAfterSecondSync).isEqualTo(countAfterFirstSync);
    }

    @Test
    @DisplayName("AC6 & AC7: Lỗi một phần giữ nguyên dữ liệu đã lưu và chạy ổn định trên DB test")
    void testPartialFailureResilienceAndDatabaseConsistency() throws Exception {
        JsonNode login = login("leader.test", "password");
        long projectId = login.path("projectId").asLong();

        ensureProjectHasJiraKey(projectId, "PROJ-PARTIAL");
        when(jiraClient.getProject(eq(projectId), anyString()))
                .thenReturn(new JiraProject("10003", "PROJ-PARTIAL", "Project PARTIAL", "http://jira.local"));

        String savedKey = "PARTIAL-1";
        JiraIssueFieldsDto fields = new JiraIssueFieldsDto("Issue synced before backlog failure", null, null, null, Instant.now().toString());
        JiraIssueDto issueDto = new JiraIssueDto("40001", savedKey, fields);

        when(jiraClient.getIssues(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issueDto)));
        doThrow(new RuntimeException("Jira Backlog API Timeout"))
                .when(jiraClient).getBacklog(eq(projectId), anyString(), anyInt(), anyInt());
        when(jiraClient.getSprints(eq(projectId), anyString(), anyInt(), anyInt()))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        JiraSyncResult result = jiraSyncService.syncProject(projectId);
        assertThat(result.errors()).isGreaterThan(0);

        JiraIssueSnapshot savedIssue = jiraIssueSnapshotRepository
                .findByProjectIdAndJiraIssueKey(projectId, savedKey)
                .orElse(null);
        assertThat(savedIssue).isNotNull();
        assertThat(savedIssue.getSummary()).isEqualTo("Issue synced before backlog failure");
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
