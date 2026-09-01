package vn.edu.cnpm.projectsupport.integration.jira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraProject;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraBacklogSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssueSnapshot;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueFieldsDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraBacklogSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueSnapshotRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;

@ExtendWith(MockitoExtension.class)
class JiraSyncServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final String PROJECT_KEY = "CNPM";

    @Mock private ProjectRepository projectRepository;
    @Mock private JiraClient jiraClient;
    @Mock private JiraIssueSnapshotRepository issueSnapshotRepository;
    @Mock private JiraBacklogSnapshotRepository backlogSnapshotRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private SyncLogRepository syncLogRepository;

    private JiraSyncService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new JiraSyncService(
                projectRepository,
                jiraClient,
                issueSnapshotRepository,
                backlogSnapshotRepository,
                sprintRepository,
                syncLogRepository);

        project = org.mockito.Mockito.mock(Project.class);
        lenient().when(project.getJiraProjectKey()).thenReturn(PROJECT_KEY);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    @Test
    void shouldReadMultipleIssuePages() {
        JiraIssueDto first = issue("100", "CNPM-1", "One", "2026-08-29T01:00:00.000+0000");
        JiraIssueDto second = issue("101", "CNPM-2", "Two", "2026-08-29T02:00:00.000+0000");

        when(jiraClient.getProject(PROJECT_ID, PROJECT_KEY))
                .thenReturn(new JiraProject("1", PROJECT_KEY, "CNPM", "https://example.atlassian.net/rest/api/3/project/1"));
        when(jiraClient.getIssues(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 2, false, List.of(first)));
        when(jiraClient.getIssues(PROJECT_ID, PROJECT_KEY, 1, 50))
                .thenReturn(new JiraPageDto<>(1, 50, 2, true, List.of(second)));
        when(jiraClient.getBacklog(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        JiraSyncResult result = service.syncProject(PROJECT_ID);

        assertThat(result.issuesSynced()).isEqualTo(2);
        verify(jiraClient).getIssues(PROJECT_ID, PROJECT_KEY, 0, 50);
        verify(jiraClient).getIssues(PROJECT_ID, PROJECT_KEY, 1, 50);
        verify(issueSnapshotRepository, times(2)).save(any(JiraIssueSnapshot.class));
    }

    @Test
    void shouldUpsertExistingIssueAndNotCreateDuplicate() {
        JiraIssueSnapshot existing = new JiraIssueSnapshot(PROJECT_ID, "100", "CNPM-1");
        when(issueSnapshotRepository.findByProjectIdAndJiraIssueId(PROJECT_ID, "100"))
                .thenReturn(Optional.of(existing));
        when(jiraClient.getProject(PROJECT_ID, PROJECT_KEY))
                .thenReturn(new JiraProject("1", PROJECT_KEY, "CNPM", "https://example.atlassian.net/p/1"));
        when(jiraClient.getIssues(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true,
                        List.of(issue("100", "CNPM-1", "Updated", "2026-08-29T03:00:00.000+0000"))));
        when(jiraClient.getBacklog(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        service.syncProject(PROJECT_ID);

        assertThat(existing.getSummary()).isEqualTo("Updated");
        assertThat(existing.getRemoteUpdatedAt()).isEqualTo(Instant.parse("2026-08-29T03:00:00Z"));
        assertThat(existing.getLastSyncedAt()).isNotNull();
        verify(issueSnapshotRepository).save(existing);
    }

    @Test
    void shouldRunAgainWithoutCreatingAnotherIssueSnapshot() {
        JiraIssueSnapshot existing = new JiraIssueSnapshot(PROJECT_ID, "100", "CNPM-1");
        when(issueSnapshotRepository.findByProjectIdAndJiraIssueId(PROJECT_ID, "100"))
                .thenReturn(Optional.of(existing));
        stubSuccessfulSync(issue("100", "CNPM-1", "Same", "2026-08-29T03:00:00.000+0000"));

        service.syncProject(PROJECT_ID);
        service.syncProject(PROJECT_ID);

        verify(issueSnapshotRepository, times(2)).save(existing);
    }

    @Test
    void shouldKeepSuccessfulIssueWhenBacklogFails() {
        when(jiraClient.getProject(PROJECT_ID, PROJECT_KEY))
                .thenReturn(new JiraProject("1", PROJECT_KEY, "CNPM", "https://example.atlassian.net/p/1"));
        when(jiraClient.getIssues(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true,
                        List.of(issue("100", "CNPM-1", "Issue", "2026-08-29T03:00:00.000+0000"))));
        when(jiraClient.getBacklog(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenThrow(new RuntimeException("Backlog unavailable"));
        when(jiraClient.getSprints(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));

        JiraSyncResult result = service.syncProject(PROJECT_ID);

        assertThat(result.issuesSynced()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
        verify(issueSnapshotRepository).save(any(JiraIssueSnapshot.class));

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(log -> log.getStatus() == SyncLogStatus.FAILED
                        && "PARTIAL_SYNC".equals(log.getErrorCode()));
    }

    @Test
    void shouldSetLastSyncedAtAndCreateSuccessSyncLog() {
        stubSuccessfulSync(issue("100", "CNPM-1", "Issue", "2026-08-29T03:00:00.000+0000"));

        JiraSyncResult result = service.syncProject(PROJECT_ID);

        assertThat(result.lastSyncedAt()).isNotNull();
        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(log -> log.getStatus() == SyncLogStatus.SUCCESS
                        && log.getCompletedAt() != null
                        && log.getCorrelationId() != null);
    }

    @Test
    void shouldCreateFailedSyncLogWhenProjectHasNoJiraKey() {
        when(project.getJiraProjectKey()).thenReturn(" ");

        assertThatThrownBy(() -> service.syncProject(PROJECT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project chưa được cấu hình Jira Project Key");

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(log -> log.getStatus() == SyncLogStatus.FAILED
                        && "JIRA_PROJECT_KEY_MISSING".equals(log.getErrorCode()));
        verify(jiraClient, never()).getProject(anyLong(), any());
    }

    @Test
    void shouldCreateFailedSyncLogWhenProjectDoesNotExist() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.syncProject(PROJECT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project không tồn tại");

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(log ->
                log.getStatus() == SyncLogStatus.FAILED
                        && "PROJECT_NOT_FOUND".equals(log.getErrorCode())
                        && log.getProjectId() == null);
    }

    private void stubSuccessfulSync(JiraIssueDto issue) {
        when(jiraClient.getProject(PROJECT_ID, PROJECT_KEY))
                .thenReturn(new JiraProject("1", PROJECT_KEY, "CNPM", "https://example.atlassian.net/p/1"));
        when(jiraClient.getIssues(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 1, true, List.of(issue)));
        when(jiraClient.getBacklog(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraPageDto<>(0, 50, 0, true, List.of()));
        when(jiraClient.getSprints(PROJECT_ID, PROJECT_KEY, 0, 50))
                .thenReturn(new JiraSprintPageDto(0, 50, 0, true, List.of()));
    }

    private JiraIssueDto issue(String id, String key, String summary, String updated) {
        return new JiraIssueDto(
                id,
                key,
                new JiraIssueFieldsDto(
                        summary, null, null, null, null, null, null, null, null, updated));
    }
}
