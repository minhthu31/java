package vn.edu.cnpm.projectsupport.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.simple.JdbcClient;

import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClientException;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.TaskAssigneeUpdateRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private FeatureRepository featureRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectAuthorizationService projectAuthorization;
    @Mock private JiraClient jiraClient;
    @Mock private SyncLogRepository syncLogRepository;
    @Mock private JdbcClient jdbcClient;
    @Mock private JdbcClient.StatementSpec statementSpec;
    @Mock private JdbcClient.MappedQuerySpec<String> querySpec;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Mock private Project mockProject;
    private Task mockTask;

    @BeforeEach
    void setUp() {
        lenient().when(mockProject.getId()).thenReturn(10L);
        lenient().when(mockProject.getGroupId()).thenReturn(100L);
        lenient().when(mockProject.getJiraProjectKey()).thenReturn("CNPM");

        mockTask = new Task(10L, "Sample Task", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setStatus(TaskStatus.TO_DO);
        mockTask.setSyncStatus(SyncStatus.NOT_SYNCED);
        mockTask.setAssigneeUserId(20L);
    }

    private void mockJdbcForExternalUser(Optional<String> externalUserIdOpt) {
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(String.class)).thenReturn(querySpec);
        when(querySpec.optional()).thenReturn(externalUserIdOpt);
    }

    @Test
    @DisplayName("1. Status hop le goi transitionIssueStatus dung mot lan")
    void updateTaskStatus_validTransition_callsJiraTransitionOnce() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        taskService.updateTaskStatus(10L, 501L, req);

        verify(jiraClient, times(1)).transitionIssueStatus(10L, "CNPM", "CNPM-501", "IN_PROGRESS");
        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
    }

    @Test
    @DisplayName("2. Assignee dung external Jira account ID, khong dung username")
    void updateTaskAssignee_validMember_usesJiraAccountId() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(projectRepository.countActiveMember(10L, 25L)).thenReturn(1L);
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        mockJdbcForExternalUser(Optional.of("jira-account-uuid-999"));

        TaskAssigneeUpdateRequest req = new TaskAssigneeUpdateRequest();
        req.setAssigneeUserId(25L);

        taskService.updateTaskAssignee(10L, 501L, req);

        verify(jiraClient, times(1)).updateIssueAssignee(10L, "CNPM", "CNPM-501", "jira-account-uuid-999");
        assertThat(mockTask.getAssigneeUserId()).isEqualTo(25L);
        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
    }

    @Test
    @DisplayName("3. Thanh vien chua lien ket Jira tra ASSIGNEE_MAPPING_MISSING")
    void updateTaskAssignee_userNotMapped_throwsAssigneeMappingMissing() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(projectRepository.countActiveMember(10L, 25L)).thenReturn(1L);
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));

        mockJdbcForExternalUser(Optional.empty());

        TaskAssigneeUpdateRequest req = new TaskAssigneeUpdateRequest();
        req.setAssigneeUserId(25L);

        assertThatThrownBy(() -> taskService.updateTaskAssignee(10L, 501L, req))
                .isInstanceOf(JiraClientException.class)
                .hasMessageContaining("ASSIGNEE_MAPPING_MISSING");

        verify(jiraClient, never()).updateIssueAssignee(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("4. Transition khong ton tai tra dung error code")
    void updateTaskStatus_missingTransition_throwsJiraTransitionMappingMissing() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));

        doThrow(new JiraClientException("JIRA_TRANSITION_MAPPING_MISSING"))
                .when(jiraClient).transitionIssueStatus(10L, "CNPM", "CNPM-501", "IN_PROGRESS");

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, 501L, req))
                .isInstanceOf(JiraClientException.class)
                .hasMessageContaining("JIRA_TRANSITION_MAPPING_MISSING");
    }

    @Test
    @DisplayName("5. Jira loi tao Task SYNC_FAILED va SyncLog FAILED thuc su duoc commit")
    void updateTaskStatus_jiraFails_savesSyncFailedAndLogFailed() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));

        doThrow(new JiraClientException("JIRA_UNAVAILABLE"))
                .when(jiraClient).transitionIssueStatus(10L, "CNPM", "CNPM-501", "IN_PROGRESS");

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, 501L, req))
                .isInstanceOf(RuntimeException.class);

        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);

        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogRepository, atLeastOnce()).save(captor.capture());
        SyncLog lastLog = captor.getValue();
        assertThat(lastLog.getStatus()).isEqualTo(SyncLogStatus.FAILED);
        assertThat(lastLog.getErrorCode()).isEqualTo("JIRA_UNAVAILABLE");
    }

    @Test
    @DisplayName("6. Task chua lien ket Jira chi cap nhat local va khong goi Jira")
    void updateTaskStatus_noJiraMapping_updatesLocalOnly() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        taskService.updateTaskStatus(10L, 501L, req);

        verify(jiraClient, never()).transitionIssueStatus(anyLong(), anyString(), anyString(), anyString());
        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.NOT_SYNCED);
    }

    @Test
    @DisplayName("7. Bo gan assignee gui accountId null")
    void updateTaskAssignee_unassign_sendsNullAccountIdToJira() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskAssigneeUpdateRequest req = new TaskAssigneeUpdateRequest();
        req.setAssigneeUserId(null);

        taskService.updateTaskAssignee(10L, 501L, req);

        verify(jiraClient, times(1)).updateIssueAssignee(10L, "CNPM", "CNPM-501", null);
        assertThat(mockTask.getAssigneeUserId()).isNull();
        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
    }
}