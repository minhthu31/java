package vn.edu.cnpm.projectsupport.integration.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfigStatus;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraBacklogSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraBacklogSnapshotRepository;

@DataJpaTest
@ActiveProfiles("test")
class JiraPersistenceRepositoryTests {

    private static final long GROUP_ID = 9750L;
    private static final long PROJECT_ID = 9751L;
    private static final long OTHER_PROJECT_ID = 9752L;
    private static final long TASK_ID = 9753L;
    private static final long OTHER_TASK_ID = 9754L;

    @Autowired
    private IntegrationConfigRepository integrationConfigRepository;

    @Autowired
    private JiraIssueRepository jiraIssueRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JiraBacklogSnapshotRepository jiraBacklogSnapshotRepository;

    @BeforeEach
    void setUpProjectAndTasks() {
        jdbcTemplate.update("""
                INSERT INTO student_groups (id, code, name)
                VALUES (?, 'CNPM-75-TEST', 'CNPM 75 Test Group')
                """, GROUP_ID);
        jdbcTemplate.update("""
                INSERT INTO projects (id, group_id, name)
                VALUES (?, ?, 'CNPM 75 Project')
                """, PROJECT_ID, GROUP_ID);
        jdbcTemplate.update("""
                INSERT INTO projects (id, group_id, name)
                VALUES (?, ?, 'Other CNPM 75 Project')
                """, OTHER_PROJECT_ID, GROUP_ID);
        insertTask(TASK_ID, PROJECT_ID, "Jira persistence task");
        insertTask(OTHER_TASK_ID, PROJECT_ID, "Other Jira persistence task");
    }

    @Test
    void savesAndFindsConfigurationByProjectAndProvider() {
        IntegrationConfig config = jiraConfig();
        config.setStatus(IntegrationConfigStatus.CONNECTED);
        config.setLastCheckedAt(Instant.parse("2026-08-25T08:00:00Z"));

        integrationConfigRepository.saveAndFlush(config);

        IntegrationConfig found = integrationConfigRepository
                .findByProjectIdAndProvider(PROJECT_ID, IntegrationProvider.JIRA)
                .orElseThrow();
        assertThat(found.getBaseUrl()).isEqualTo("https://example.atlassian.net");
        assertThat(found.getStatus()).isEqualTo(IntegrationConfigStatus.CONNECTED);
        assertThat(integrationConfigRepository.existsByProjectIdAndProvider(
                PROJECT_ID, IntegrationProvider.JIRA)).isTrue();
    }

    @Test
    void configurationIsUniqueByProjectAndProvider() {
        integrationConfigRepository.saveAndFlush(jiraConfig());

        assertThatThrownBy(() -> integrationConfigRepository.saveAndFlush(jiraConfig()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void encryptedSecretIsExcludedFromJsonSerialization() throws Exception {
        assertThat(IntegrationConfig.class
                .getMethod("getEncryptedSecret")
                .isAnnotationPresent(JsonIgnore.class))
                .isTrue();
    }

    @Test
    void savesSnapshotAndFindsJiraIssueByTaskOrIssueKey() {
        JiraIssue issue = jiraIssue(TASK_ID, "10074", "CNPM-74");
        issue.setRemoteUpdatedAt(Instant.parse("2026-08-25T08:30:00Z"));
        issue.setSnapshotHash("sha256:cnpm75-test");
        issue.setRawSnapshot(Map.of("key", "CNPM-74", "status", "To Do"));

        jiraIssueRepository.saveAndFlush(issue);

        assertThat(jiraIssueRepository.findByTaskId(TASK_ID)).contains(issue);
        assertThat(jiraIssueRepository.findByJiraIssueId("10074")).contains(issue);
        assertThat(jiraIssueRepository.findByJiraIssueKey("CNPM-74")).contains(issue);
        assertThat(jiraIssueRepository.findByTaskId(TASK_ID).orElseThrow().getRawSnapshot())
                .containsEntry("status", "To Do");
    }

    @Test
    void taskCannotMapToTwoJiraIssues() {
        jiraIssueRepository.saveAndFlush(jiraIssue(TASK_ID, "10074", "CNPM-74"));

        assertThatThrownBy(() -> jiraIssueRepository.saveAndFlush(
                jiraIssue(TASK_ID, "10075", "CNPM-75")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void jiraIssueIdCannotBeDuplicated() {
        jiraIssueRepository.saveAndFlush(jiraIssue(TASK_ID, "10074", "CNPM-74"));

        assertThatThrownBy(() -> jiraIssueRepository.saveAndFlush(
                jiraIssue(OTHER_TASK_ID, "10074", "CNPM-75")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void jiraIssueKeyCannotBeDuplicated() {
        jiraIssueRepository.saveAndFlush(jiraIssue(TASK_ID, "10074", "CNPM-74"));

        assertThatThrownBy(() -> jiraIssueRepository.saveAndFlush(
                jiraIssue(OTHER_TASK_ID, "10075", "CNPM-74")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsSyncHistoryByProjectStatusAndCorrelationId() {
        SyncLog first = syncLog(
                PROJECT_ID,
                "TASK",
                "74",
                "corr-cnpm-75",
                Instant.parse("2026-08-25T08:00:00Z"),
                SyncLogStatus.SUCCESS);
        SyncLog second = syncLog(
                PROJECT_ID,
                "TASK",
                "75",
                "corr-cnpm-75",
                Instant.parse("2026-08-25T08:05:00Z"),
                SyncLogStatus.FAILED);
        SyncLog outsideProject = syncLog(
                OTHER_PROJECT_ID,
                "TASK",
                "76",
                "corr-cnpm-75",
                Instant.parse("2026-08-25T08:10:00Z"),
                SyncLogStatus.FAILED);
        syncLogRepository.saveAllAndFlush(java.util.List.of(first, second, outsideProject));

        assertThat(syncLogRepository.findByProjectIdOrderByStartedAtDesc(
                PROJECT_ID, PageRequest.of(0, 10)).getContent())
                .containsExactly(second, first);
        assertThat(syncLogRepository.findByProjectIdAndStatusOrderByStartedAtDesc(
                PROJECT_ID, SyncLogStatus.FAILED, PageRequest.of(0, 10)).getContent())
                .containsExactly(second);
        assertThat(syncLogRepository.findByProjectIdAndCorrelationIdOrderByStartedAtDesc(
                PROJECT_ID, "corr-cnpm-75"))
                .containsExactly(second, first);
        assertThat(syncLogRepository
                .findByProjectIdAndStatusAndCorrelationIdOrderByStartedAtDesc(
                        PROJECT_ID, SyncLogStatus.FAILED, "corr-cnpm-75"))
                .containsExactly(second);
    }

    private IntegrationConfig jiraConfig() {
        IntegrationConfig config = new IntegrationConfig(
                PROJECT_ID,
                IntegrationProvider.JIRA,
                "enc:v1:local-test-ciphertext");
        config.setBaseUrl("https://example.atlassian.net");
        config.setAccountIdentifier("integration@example.com");
        return config;
    }

    private JiraIssue jiraIssue(
            Long taskId,
            String jiraIssueId,
            String jiraIssueKey) {
        return new JiraIssue(
                taskId,
                jiraIssueId,
                jiraIssueKey,
                "https://example.atlassian.net/browse/" + jiraIssueKey,
                Instant.parse("2026-08-25T08:00:00Z"));
    }

    private SyncLog syncLog(
            Long projectId,
            String entityType,
            String entityId,
            String correlationId,
            Instant startedAt,
            SyncLogStatus status) {
        SyncLog log = new SyncLog(
                projectId,
                IntegrationProvider.JIRA,
                entityType,
                entityId,
                SyncDirection.EXPORT,
                correlationId,
                startedAt);
        log.setStatus(status);
        log.setCompletedAt(startedAt.plusSeconds(1));
        if (status == SyncLogStatus.FAILED) {
            log.setRetryCount(1);
            log.setErrorCode("JIRA_UNAVAILABLE");
            log.setErrorMessage("Sanitized test failure");
        }
        return log;
    }

    private void insertTask(long taskId, long projectId, String title) {
        jdbcTemplate.update("""
                INSERT INTO tasks (
                    id, project_id, title, acceptance_criteria,
                    issue_type, priority, status, sync_status)
                VALUES (?, ?, ?, 'Repository test', 'TASK', 'MEDIUM', 'TO_DO', 'NOT_SYNCED')
                """, taskId, projectId, title);
    }

    @Test
    void backlogSnapshotCanBeUpdatedWhenJiraProjectKeyChanges() {
        JiraBacklogSnapshot snapshot = new JiraBacklogSnapshot(PROJECT_ID, "OLD");
        snapshot.setLastSyncedAt(Instant.parse("2026-08-29T08:00:00Z"));
        snapshot.setSnapshotHash("hash-old");
        snapshot.setRawSnapshot(Map.of("projectKey", "OLD", "items", java.util.List.of()));

        jiraBacklogSnapshotRepository.saveAndFlush(snapshot);

        JiraBacklogSnapshot found = jiraBacklogSnapshotRepository.findByProjectId(PROJECT_ID).orElseThrow();
        found.setJiraProjectKey("NEW");
        jiraBacklogSnapshotRepository.saveAndFlush(found);

        JiraBacklogSnapshot updated = jiraBacklogSnapshotRepository.findByProjectId(PROJECT_ID).orElseThrow();
        assertThat(updated.getJiraProjectKey())
                .isEqualTo("NEW");
        }
}
