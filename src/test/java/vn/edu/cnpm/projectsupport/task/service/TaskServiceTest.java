package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.dto.TaskResponse;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private FeatureRepository featureRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectAuthorizationService projectAuthorization;

    @InjectMocks
    private TaskServiceImpl taskService;

    private CreateTaskRequest validRequest;
    private TaskStatusUpdateRequest updateRequest;
    private final Long leaderId = 10L;
    private final Long memberId = 20L;
    private final Long otherMemberId = 30L;
    private final Long projectId = 1L;
    private final Long taskId = 100L;
    private Project mockProject;

    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        when(mockProject.getGroupId()).thenReturn(999L);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(projectRepository.countActiveLeader(projectId, leaderId)).thenReturn(1);
        when(projectRepository.countActiveMember(projectId, memberId)).thenReturn(1);
        when(projectAuthorization.currentUserId()).thenReturn(leaderId);
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        when(taskRepository.findJiraIssueKeyByTaskId(any())).thenReturn(Optional.empty());

        validRequest = new CreateTaskRequest();
        validRequest.setTitle("Viết Unit Test cho Task Service");
        validRequest.setDescription("Mô tả chi tiết task");
        validRequest.setAcceptanceCriteria("Phủ > 80% code coverage");
        validRequest.setIssueType(TaskIssueType.TASK);
        validRequest.setPriority(TaskPriority.MEDIUM);
        validRequest.setAssigneeUserId(memberId);

        updateRequest = new TaskStatusUpdateRequest();
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Tạo Task thành công, đúng phân loại AUTO_TEST và lưu DB")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse createdTask = taskService.createTask(projectId, leaderId, validRequest);

        assertNotNull(createdTask);
        assertEquals(TaskClassification.AUTO_TEST, createdTask.getClassification());
        assertEquals(TaskStatus.TO_DO, createdTask.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi không tìm thấy Project")
    void createTask_ThrowsException_WhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> taskService.createTask(projectId, leaderId, validRequest)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Member cập nhật trạng thái hợp lệ (TO_DO -> IN_PROGRESS)")
    void updateTaskStatus_Success_WhenAssignedMember() {
        Task mockTask = new Task(projectId, "Test Task", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(memberId);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse updatedTask = taskService.updateTaskStatusByMember(projectId, memberId, taskId, updateRequest);

        assertNotNull(updatedTask);
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        verify(taskRepository, times(1)).save(mockTask);
        verify(activityLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố tình cập nhật Task của người khác (sai quyền)")
    void updateTaskStatus_ThrowsException_WhenNotAssignedMember() {
        Task mockTask = new Task(projectId, "Test Task", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(memberId);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));

        assertThrows(
            ForbiddenGroupScopeException.class,
            () -> taskService.updateTaskStatusByMember(projectId, otherMemberId, taskId, updateRequest)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi cập nhật trạng thái của Task không tồn tại")
    void updateTaskStatus_ThrowsException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> taskService.updateTaskStatusByMember(projectId, memberId, taskId, updateRequest)
        );

        verify(taskRepository, never()).save(any());
    }
}
