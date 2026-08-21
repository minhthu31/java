package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.feature.domain.Feature;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

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
                sprintRepository);
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

    // --- TEST CLASSIFICATION ---

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

    // --- TEST UPDATE STATUS & TRANSITIONS ---

    @Test
    @DisplayName("Chuyển trạng thái hợp lệ và ghi Activity Log vào DB")
    void updateStatus_Success_SavesActivityLog() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        TaskResponse res = taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq);

        assertEquals(TaskStatus.IN_PROGRESS, res.getStatus());
        verify(activityLogRepository, times(1)).save(any(ActivityLog.class));
    }

    @Test
    @DisplayName("Lỗi khi Task không thuộc Project yêu cầu")
    void updateStatus_ThrowsException_ProjectMismatch() {
        Task mockTask = createMockTask(1L, 999L, 100L, TaskStatus.TO_DO); // Project ID = 999
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ResourceNotFoundException.class,
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq));
    }

    @Test
    @DisplayName("Từ chối cập nhật khi Task chưa gán Assignee")
    void updateStatus_ThrowsException_UnassignedTask() {
        Task mockTask = createMockTask(1L, 1L, null, TaskStatus.TO_DO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ForbiddenGroupScopeException.class,
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq));
    }

    @Test
    @DisplayName("Từ chối khi Member cố tình cập nhật Task của người khác")
    void updateStatus_ThrowsException_WrongAssignee() {
        Task mockTask = createMockTask(1L, 1L, 200L, TaskStatus.TO_DO); // Assignee = 200
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(ForbiddenGroupScopeException.class,
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq)); // Current user = 100
    }

    @Test
    @DisplayName("Thiếu lý do khi chuyển sang BLOCKED")
    void updateStatus_ThrowsException_BlockedWithoutReason() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.BLOCKED);
        updateReq.setReason(""); // Lý do rỗng

        assertThrows(IllegalArgumentException.class, 
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq));
    }

    @Test
    @DisplayName("Team Member không được quyền tự CANCELLED Task")
    void updateStatus_ThrowsException_MemberCannotCancel() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.CANCELLED);
        updateReq.setReason("Hủy bỏ task");

        assertThrows(InvalidStatusTransitionException.class,
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq));
    }

    @Test
    @DisplayName("Không thể chuyển trạng thái khi Task đã ở trạng thái kết thúc (DONE)")
    void updateStatus_ThrowsException_TerminalState() {
        Task mockTask = createMockTask(1L, 1L, 100L, TaskStatus.DONE);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        updateReq.setStatus(TaskStatus.IN_PROGRESS);

        assertThrows(InvalidStatusTransitionException.class,
            () -> taskService.updateTaskStatusByMember(1L, 100L, 1L, updateReq));
    }

    private Task createMockTask(Long taskId, Long projectId, Long assigneeId, TaskStatus status) {
        Task task = new Task(projectId, "Title", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        task.setAssigneeUserId(assigneeId);
        task.setStatus(status);
        return task;
    }
}
