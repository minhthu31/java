package vn.edu.cnpm.projectsupport.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;

import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.AssigneeOutsideGroupException;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClientException;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
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

    // ==========================================
    // 24 TEST CASES GỐC CỦA DỰ ÁN
    // ==========================================

    @Test
    @DisplayName("1. Leader tao Task thanh cong")
    void createTask_asLeader_success() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(projectRepository.countActiveMember(10L, 20L)).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("New Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.HIGH);
        req.setAssigneeUserId(20L);

        TaskResponse res = taskService.createTask(10L, req);

        assertThat(res).isNotNull();
        assertThat(res.getTitle()).isEqualTo("New Task");
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("2. Tao Task kem Idempotency-Key thanh cong")
    void createTask_withIdempotencyKey_success() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(taskRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task Idempotency");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);

        TaskResponse res = taskService.createTask(10L, req, "KEY-123");

        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("3. Tao Task voi Idempotency-Key da ton tai trong cung Project tra ve Task cu")
    void createTask_withExistingIdempotencyKey_returnsExistingTask() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(taskRepository.findByIdempotencyKey("KEY-EXIST")).thenReturn(Optional.of(mockTask));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Duplicate Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);

        TaskResponse res = taskService.createTask(10L, req, "KEY-EXIST");

        assertThat(res).isNotNull();
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("4. Tao Task voi Idempotency-Key thuoc Project khac nem loi")
    void createTask_withExistingIdempotencyKeyInDifferentProject_throwsResourceInUse() {
        Task otherProjectTask = new Task(99L, "Other Task", "AC", TaskIssueType.TASK, TaskPriority.LOW);

        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(taskRepository.findByIdempotencyKey("KEY-DIFF")).thenReturn(Optional.of(otherProjectTask));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);

        assertThatThrownBy(() -> taskService.createTask(10L, req, "KEY-DIFF"))
                .isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("5. Khong phai Leader tao Task nem ForbiddenGroupScopeException")
    void createTask_notLeader_throwsForbidden() {
        when(projectAuthorization.currentUserId()).thenReturn(2L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 2L)).thenReturn(0L);

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(ForbiddenGroupScopeException.class);
    }

    @Test
    @DisplayName("6. Tao SUBTASK nem IllegalArgumentException")
    void createTask_subtask_throwsIllegalArgument() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Subtask");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.SUBTASK);
        req.setPriority(TaskPriority.MEDIUM);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("7. Tao Task voi Assignee ngoai group nem AssigneeOutsideGroupException")
    void createTask_assigneeOutsideGroup_throwsAssigneeOutsideGroup() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(projectRepository.countActiveMember(10L, 99L)).thenReturn(0L);

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);
        req.setAssigneeUserId(99L);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(AssigneeOutsideGroupException.class);
    }

    @Test
    @DisplayName("8. Tao Task voi Requirement khong thuoc Project nem ResourceNotFoundException")
    void createTask_invalidRequirement_throwsNotFound() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(requirementRepository.findByIdAndProjectId(999L, 10L)).thenReturn(Optional.empty());

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Task");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);
        req.setRequirementId(999L);

        assertThatThrownBy(() -> taskService.createTask(10L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("9. Auto-classify Task theo tieu de test")
    void createTask_autoClassify_test() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Viet unit test cho TaskService");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.LOW);

        TaskResponse res = taskService.createTask(10L, req);
        assertThat(res.getClassification()).isEqualTo(TaskClassification.AUTO_TEST);
    }

    @Test
    @DisplayName("10. Auto-classify Task theo logging")
    void createTask_autoClassify_log() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(10L, 1L)).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Cau hinh logging va trace he thong");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.LOW);

        TaskResponse res = taskService.createTask(10L, req);
        assertThat(res.getClassification()).isEqualTo(TaskClassification.AUTO_LOG);
    }

    @Test
    @DisplayName("11. Update Task thanh cong")
    void updateTask_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setTitle("Updated Title");
        req.setDescription("Desc");
        req.setAcceptanceCriteria("New AC");
        req.setIssueType(TaskIssueType.BUG);
        req.setPriority(TaskPriority.HIGH);

        TaskResponse res = taskService.updateTask(10L, 501L, req);

        assertThat(res.getTitle()).isEqualTo("Updated Title");
        assertThat(res.getIssueType()).isEqualTo(TaskIssueType.BUG);
    }

    @Test
    @DisplayName("12. Update Task da ket thuc nem ResourceInUseException")
    void updateTask_alreadyDone_throwsResourceInUse() {
        mockTask.setStatus(TaskStatus.DONE);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));

        UpdateTaskRequest req = new UpdateTaskRequest();
        req.setTitle("Update");
        req.setAcceptanceCriteria("AC");
        req.setIssueType(TaskIssueType.TASK);
        req.setPriority(TaskPriority.MEDIUM);

        assertThatThrownBy(() -> taskService.updateTask(10L, 501L, req))
                .isInstanceOf(ResourceInUseException.class);
    }

    @Test
    @DisplayName("13. Member cap nhat status Task cua minh thanh cong")
    void updateTaskStatusByMember_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(projectRepository.countActiveMember(10L, 20L)).thenReturn(1L);
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.empty());
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse res = taskService.updateTaskStatusByMember(10L, 20L, 501L, req);

        assertThat(res.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("14. Member cap nhat Task cua nguoi khac nem ForbiddenGroupScopeException")
    void updateTaskStatusByMember_notAssignee_throwsForbidden() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateTaskStatusByMember(10L, 99L, 501L, req))
                .isInstanceOf(ForbiddenGroupScopeException.class);
    }

    @Test
    @DisplayName("15. Member tu y CANCELLED Task nem InvalidStatusTransitionException")
    void updateTaskStatusByMember_cancel_throwsInvalidTransition() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(projectRepository.countActiveMember(10L, 20L)).thenReturn(1L);

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.CANCELLED);
        req.setReason("Lý do hủy");

        assertThatThrownBy(() -> taskService.updateTaskStatusByMember(10L, 20L, 501L, req))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("16. Chuyen status sang BLOCKED ma thieu reason nem IllegalArgumentException")
    void updateTaskStatus_blockedWithoutReason_throwsIllegalArgument() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.BLOCKED);

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, 501L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("17. Chuyen status bat hop le nem InvalidStatusTransitionException")
    void updateTaskStatus_invalidTransition_throwsException() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.DONE); // TO_DO -> DONE không hợp lệ

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, 501L, req))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("18. Lay Task theo ID thanh cong")
    void getTaskById_success() {
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));

        TaskResponse res = taskService.getTaskById(10L, 501L);

        assertThat(res).isNotNull();
        assertThat(res.getTitle()).isEqualTo("Sample Task");
    }

    @Test
    @DisplayName("19. Lay Task thuoc Project khac nem ResourceNotFoundException")
    void getTaskById_wrongProject_throwsNotFound() {
        Task otherProjectTask = new Task(99L, "Title", "AC", TaskIssueType.TASK, TaskPriority.LOW);
        when(taskRepository.findById(501L)).thenReturn(Optional.of(otherProjectTask));

        assertThatThrownBy(() -> taskService.getTaskById(10L, 501L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("20. Lay danh sach Tasks theo Project cho Team Member chi tra Task cua minh")
    void getTasksByProject_asMember_returnsAssignedOnly() {
        when(projectAuthorization.isCurrentUserTeamMember(10L)).thenReturn(true);
        when(projectAuthorization.currentUserId()).thenReturn(20L);
        when(taskRepository.findByProjectIdAndAssigneeUserId(10L, 20L)).thenReturn(List.of(mockTask));

        List<TaskResponse> res = taskService.getTasksByProject(10L);

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("21. Lay danh sach Tasks theo Project cho Leader tra toan bo")
    void getTasksByProject_asLeader_returnsAll() {
        when(projectAuthorization.isCurrentUserTeamMember(10L)).thenReturn(false);
        when(taskRepository.findByProjectId(10L)).thenReturn(List.of(mockTask));

        List<TaskResponse> res = taskService.getTasksByProject(10L);

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("22. Phan trang Task thanh cong")
    void getTasks_withPagination_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(projectAuthorization.isCurrentUserTeamMember(10L)).thenReturn(false);
        when(taskRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(mockTask)));

        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        PageResponse<TaskResponse> res = taskService.getTasks(10L, filter);

        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("23. Xoa Task TO_DO chua dong bo thanh cong")
    void deleteTask_todoAndNotSynced_success() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.countJiraIssuesByTaskId(501L)).thenReturn(0L);
        when(taskRepository.countCommitLinksByTaskId(501L)).thenReturn(0L);
        when(taskRepository.countPullRequestLinksByTaskId(501L)).thenReturn(0L);
        when(activityLogRepository.existsByEntityTypeAndEntityIdAndActionNot(anyString(), anyString(), anyString())).thenReturn(false);

        taskService.deleteTask(10L, 501L);

        verify(taskRepository).delete(mockTask);
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("24. Xoa Task da lien ket Jira nem ResourceInUseException")
    void deleteTask_withJiraIssue_throwsResourceInUse() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.countJiraIssuesByTaskId(501L)).thenReturn(1L);

        assertThatThrownBy(() -> taskService.deleteTask(10L, 501L))
                .isInstanceOf(ResourceInUseException.class);
    }

    // ==========================================
    // 7 TEST CASES BỔ SUNG CHO CNPM-82
    // ==========================================

    @Test
    @DisplayName("CNPM-82: 1. Status hop le goi transitionIssueStatus dung mot lan")
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
    @DisplayName("CNPM-82: 2. Assignee dung external Jira account ID, khong dung username")
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
    @DisplayName("CNPM-82: 3. Thanh vien chua lien ket Jira tra ASSIGNEE_MAPPING_MISSING")
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
    @DisplayName("CNPM-82: 4. Transition khong ton tai tra dung error code")
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
    @DisplayName("CNPM-82: 5. Jira loi tao Task SYNC_FAILED va SyncLog FAILED thuc su duoc commit")
    void updateTaskStatus_jiraFails_savesSyncFailedAndLogFailed() {
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(10L)).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(mockProject));
        when(taskRepository.findById(501L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.findJiraIssueKeyByTaskId(501L)).thenReturn(Optional.of("CNPM-501"));
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);

        doThrow(new JiraClientException("JIRA_UNAVAILABLE"))
                .when(jiraClient).transitionIssueStatus(10L, "CNPM", "CNPM-501", "IN_PROGRESS");

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        assertThatThrownBy(() -> taskService.updateTaskStatus(10L, 501L, req))
                .isInstanceOf(RuntimeException.class);

        assertThat(mockTask.getStatus()).isEqualTo(TaskStatus.TO_DO);
        assertThat(mockTask.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);
        verify(jdbcClient, atLeastOnce()).sql(anyString());
    }

    @Test
    @DisplayName("CNPM-82: 6. Task chua lien ket Jira chi cap nhat local va khong goi Jira")
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
    @DisplayName("CNPM-82: 7. Bo gan assignee gui accountId null")
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