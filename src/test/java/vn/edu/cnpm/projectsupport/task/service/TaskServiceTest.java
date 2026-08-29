package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.AssigneeOutsideGroupException;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.domain.Feature;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    private TaskServiceImpl taskService;

    private CreateTaskRequest createReq;
    private TaskStatusUpdateRequest updateReq;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(
                taskRepository,
                activityLogRepository,
                projectRepository,
                requirementRepository,
                featureRepository,
                sprintRepository,
                userRepository,
                projectAuthorization,
                jiraClient);

        createReq = new CreateTaskRequest();
        createReq.setTitle("Phát triển API Task");
        createReq.setAcceptanceCriteria("Hoàn tất giao diện và API");
        createReq.setIssueType(TaskIssueType.TASK);
        createReq.setPriority(TaskPriority.HIGH);

        updateReq = new TaskStatusUpdateRequest();

        lenient().when(projectRepository.findById(1L))
                .thenReturn(Optional.of(new Project(10L, "Project")));
        lenient().when(projectRepository.countActiveLeader(1L, 100L)).thenReturn(1L);
        lenient().when(projectRepository.countActiveMember(1L, 100L)).thenReturn(1L);
    }

    @Test
    @DisplayName("Tự động phân loại AUTO_TEST khi chứa từ khóa test")
    void createTask_AutoTest() {
        createReq.setAcceptanceCriteria("Viết unit test đạt coverage > 80%");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.createTask(1L, 100L, createReq);
        assertEquals(TaskClassification.AUTO_TEST, res.getClassification());
    }

    @Test
    @DisplayName("Tự động phân loại AUTO_LOG khi chứa từ khóa logging/monitor")
    void createTask_AutoLog() {
        createReq.setAcceptanceCriteria("Cấu hình logging monitor hệ thống");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.createTask(1L, 100L, createReq);
        assertEquals(TaskClassification.AUTO_LOG, res.getClassification());
    }

    @Test
    @DisplayName("Tự động phân loại NEW_FEATURE khi chứa từ khóa new feature")
    void createTask_NewFeature() {
        createReq.setTitle("Thêm new feature đăng nhập SSO");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.createTask(1L, 100L, createReq);
        assertEquals(TaskClassification.NEW_FEATURE, res.getClassification());
    }

    @Test
    @DisplayName("Tự động phân loại FEATURE_RELATED khi gắn featureId")
    void createTask_FeatureRelated() {
        createReq.setFeatureId(10L);
        when(featureRepository.findByIdAndProjectId(10L, 1L))
                .thenReturn(Optional.of(new Feature(1L, "Feature")));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.createTask(1L, 100L, createReq);
        assertEquals(TaskClassification.FEATURE_RELATED, res.getClassification());
    }

    @Test
    @DisplayName("Phân loại OTHER khi không dính từ khóa đặc biệt")
    void createTask_Other() {
        createReq.setTitle("Cập nhật tài liệu hướng dẫn");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.createTask(1L, 100L, createReq);
        assertEquals(TaskClassification.OTHER, res.getClassification());
    }

    @Test
    @DisplayName("Idempotency-Key trả về Task đã tạo thay vì tạo trùng")
    void createTask_IdempotencyKeyReturnsExistingTask() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findByIdempotencyKey("cnpm-61-create")).thenReturn(Optional.of(existing));

        TaskResponse response = taskService.createTask(1L, createReq, " cnpm-61-create ");

        assertEquals("Title", response.getTitle());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Từ chối Idempotency-Key đã được dùng ở Project khác")
    void createTask_IdempotencyKeyRejectsDifferentProject() {
        Task existing = createMockTask(1L, 2L, null, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findByIdempotencyKey("shared-key")).thenReturn(Optional.of(existing));

        assertThrows(ResourceInUseException.class,
                () -> taskService.createTask(1L, createReq, "shared-key"));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Từ chối tạo SUBTASK khi chưa có parentTaskId")
    void createTask_RejectsUnsupportedSubtask() {
        createReq.setIssueType(TaskIssueType.SUBTASK);

        assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(1L, 100L, createReq));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Team Leader cập nhật nội dung Task và ghi Activity Log")
    void updateTask_Success() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Task API đã cập nhật");
        request.setAcceptanceCriteria("Có đủ bảy endpoint");
        request.setIssueType(TaskIssueType.TASK);
        request.setPriority(TaskPriority.HIGH);
        request.setAssigneeUserId(200L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.countActiveMember(1L, 200L)).thenReturn(1L);
        when(taskRepository.save(existing)).thenReturn(existing);
        when(projectAuthorization.currentUserId()).thenReturn(100L);

        TaskResponse response = taskService.updateTask(1L, 1L, request);

        assertEquals("Task API đã cập nhật", response.getTitle());
        assertEquals(TaskPriority.HIGH, response.getPriority());
        assertEquals(200L, response.getAssigneeUserId());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("Từ chối gán Task cho người không thuộc group")
    void updateAssignee_RejectsUserOutsideGroup() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        TaskAssigneeUpdateRequest request = new TaskAssigneeUpdateRequest();
        request.setAssigneeUserId(999L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.countActiveMember(1L, 999L)).thenReturn(0L);

        assertThrows(
                AssigneeOutsideGroupException.class,
                () -> taskService.updateTaskAssignee(1L, 1L, request));
    }

    @Test
    @DisplayName("Gán Task cho thành viên ACTIVE và ghi Activity Log")
    void updateAssignee_AssignsActiveMemberAndLogsActivity() {
        Task existing = createMockTask(1L, 1L, 100L, TaskStatus.TO_DO);
        TaskAssigneeUpdateRequest request = new TaskAssigneeUpdateRequest();
        request.setAssigneeUserId(200L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.countActiveMember(1L, 200L)).thenReturn(1L);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.save(existing)).thenReturn(existing);

        TaskResponse response = taskService.updateTaskAssignee(1L, 1L, request);

        assertEquals(200L, response.getAssigneeUserId());
        verify(activityLogRepository).save(any(ActivityLog.class));
        verify(taskRepository).save(existing);
    }

    @Test
    @DisplayName("Team Member chỉ nhận danh sách Task được giao cho mình")
    void getTasksByProject_MemberOnlyReceivesAssignedTasks() {
        Task assignedTask = createMockTask(1L, 1L, 100L, TaskStatus.TO_DO);
        when(projectAuthorization.isCurrentUserTeamMember(1L)).thenReturn(true);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findByProjectIdAndAssigneeUserId(1L, 100L))
                .thenReturn(List.of(assignedTask));

        List<TaskResponse> responses = taskService.getTasksByProject(1L);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).getAssigneeUserId());
        verify(taskRepository).findByProjectIdAndAssigneeUserId(1L, 100L);
        verify(taskRepository, never()).findByProjectId(1L);
    }

    @Test
    @DisplayName("Không xóa Task đã liên kết commit hoặc PR")
    void deleteTask_RejectsExternallyReferencedTask() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.countCommitLinksByTaskId(1L)).thenReturn(1L);

        assertThrows(ResourceInUseException.class, () -> taskService.deleteTask(1L, 1L));
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Xóa Task TO_DO chưa đồng bộ và chưa có liên kết")
    void deleteTask_DeletesEligibleTaskAndLogsActivity() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectAuthorization.currentUserId()).thenReturn(100L);

        taskService.deleteTask(1L, 1L);

        verify(activityLogRepository).save(any(ActivityLog.class));
        verify(taskRepository).delete(existing);
    }

    @Test
    @DisplayName("Không xóa Task đã bắt đầu xử lý")
    void deleteTask_RejectsTaskThatIsNotToDo() {
        Task existing = createMockTask(1L, 1L, null, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(ResourceInUseException.class, () -> taskService.deleteTask(1L, 1L));

        verify(taskRepository, never()).countJiraIssuesByTaskId(1L);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("Chuyển trạng thái hợp lệ và ghi Activity Log vào DB")
    void updateStatus_Success_SavesActivityLog() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse res = taskService.updateTaskStatusByMember(1L, 1L, updateReq);

        assertEquals(TaskStatus.IN_PROGRESS, res.getStatus());
        verify(activityLogRepository, times(1)).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("Lỗi khi Task không thuộc Project yêu cầu")
    void updateStatus_ThrowsException_ProjectMismatch() {
        Task mockTask = createMockTask(1L, 999L, 100L, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Từ chối cập nhật khi Task chưa gán Assignee")
    void updateStatus_ThrowsException_UnassignedTask() {
        Task mockTask = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ForbiddenGroupScopeException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Từ chối khi Member cố tình cập nhật Task của người khác")
    void updateStatus_ThrowsException_WrongAssignee() {
        Task mockTask = createMockTask(1L, 1L, 200L, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ForbiddenGroupScopeException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Thiếu lý do khi chuyển sang BLOCKED")
    void updateStatus_ThrowsException_BlockedWithoutReason() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.IN_PROGRESS);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.BLOCKED);
        updateReq.setReason("");

        assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Team Member không được quyền tự CANCELLED Task")
    void updateStatus_ThrowsException_MemberCannotCancel() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.IN_PROGRESS);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.CANCELLED);
        updateReq.setReason("Hủy bỏ task");

        assertThrows(InvalidStatusTransitionException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Không thể chuyển trạng thái khi Task đã ở trạng thái kết thúc (DONE)")
    void updateStatus_ThrowsException_TerminalState() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.DONE);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(InvalidStatusTransitionException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));
    }

    @Test
    @DisplayName("Team Leader được hủy Task với lý do")
    void updateStatus_LeaderCanCancelTaskWithReason() {
        Task mockTask = createMockTask(1L, 1L, null, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(mockTask)).thenReturn(mockTask);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(projectAuthorization.isCurrentUserLeader(1L)).thenReturn(true);
        updateReq.setStatus(TaskStatus.CANCELLED);
        updateReq.setReason("Không còn nằm trong phạm vi Sprint");

        TaskResponse response = taskService.updateTaskStatus(1L, 1L, updateReq);

        assertEquals(TaskStatus.CANCELLED, response.getStatus());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("Thành viên không còn ACTIVE không được cập nhật Task")
    void updateStatus_RejectsInactiveAssignedMember() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.TO_DO);
        when(projectAuthorization.currentUserId()).thenReturn(100L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(projectRepository.countActiveMember(1L, 100L)).thenReturn(0L);
        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ForbiddenGroupScopeException.class,
                () -> taskService.updateTaskStatusByMember(1L, 1L, updateReq));

        verify(taskRepository, never()).save(any(Task.class));
    }

    private Task createMockTask(Long taskId, Long projectId, Long assigneeId, TaskStatus status) {
        Task task = new Task(projectId, "Title", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        task.setAssigneeUserId(assigneeId);
        task.setStatus(status);
        return task;
    }
}