package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.activitylog.service.ActivityLogService;
import vn.edu.cnpm.projectsupport.common.exception.BadRequestException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.service.ProjectMemberService;

// Import Task và các enum/DTO trực tiếp từ package task
import vn.edu.cnpm.projectsupport.task.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.Task;
import vn.edu.cnpm.projectsupport.task.SyncStatus;
import vn.edu.cnpm.projectsupport.task.TaskStatus;
import vn.edu.cnpm.projectsupport.task.TaskType;
import vn.edu.cnpm.projectsupport.task.TaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private CreateTaskRequest validRequest;
    private final Long leaderId = 10L;
    private final Long memberId = 20L;
    private final Long otherMemberId = 30L;
    private final Long projectId = 1L;
    private final Long taskId = 100L;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTaskRequest();
        validRequest.setTitle("Viết Unit Test cho Task Service");
        validRequest.setDescription("Mô tả chi tiết task");
        validRequest.setAcceptanceCriteria("Phủ > 80% code coverage");
        validRequest.setType(TaskType.TEST);
        validRequest.setProjectId(projectId);
        validRequest.setRequirementId(5L);
        validRequest.setAssigneeId(memberId);
    }

    @Test
    @DisplayName("Tạo Task thành công, đúng phân loại, mặc định NOT_SYNCED và ghi log")
    void createTask_Success_WithTaskTypeAndActivityLog() {
        when(projectMemberService.isMemberOfProject(memberId, projectId)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(taskId);
            return task;
        });

        Task createdTask = taskService.createTask(leaderId, validRequest);

        assertNotNull(createdTask);
        assertEquals(leaderId, createdTask.getCreatedById());
        assertEquals(TaskType.TEST, createdTask.getType());
        assertEquals(SyncStatus.NOT_SYNCED, createdTask.getSyncStatus());
        assertEquals(TaskStatus.TODO, createdTask.getStatus());

        verify(taskRepository, times(1)).save(any(Task.class));
        verify(activityLogService, times(1)).logActivity(eq(leaderId), anyString());
    }

    @Test
    @DisplayName("Ném lỗi khi Assignee không thuộc Project")
    void createTask_ThrowsException_WhenAssigneeNotInProject() {
        when(projectMemberService.isMemberOfProject(memberId, projectId)).thenReturn(false);

        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> taskService.createTask(leaderId, validRequest)
        );

        assertEquals("Người được giao việc không thuộc dự án này.", exception.getMessage());
        verify(taskRepository, never()).save(any());
        verify(activityLogService, never()).logActivity(anyLong(), anyString());
    }

    @Test
    @DisplayName("Member cập nhật trạng thái hợp lệ và hệ thống ghi Activity Log")
    void updateTaskStatus_Success_WhenAssignedMember_AndLogsActivity() {
        Task mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setProjectId(projectId);
        mockTask.setAssigneeId(memberId);
        mockTask.setStatus(TaskStatus.TODO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task updatedTask = taskService.updateTaskStatusByMember(memberId, taskId, TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        verify(taskRepository, times(1)).save(mockTask);
        verify(activityLogService, times(1)).logActivity(eq(memberId), anyString());
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố tình cập nhật Task của người khác (sai quyền)")
    void updateTaskStatus_ThrowsException_WhenNotAssignedMember() {
        Task mockTask = new Task();
        mockTask.setId(taskId);
        mockTask.setProjectId(projectId);
        mockTask.setAssigneeId(memberId);
        mockTask.setStatus(TaskStatus.TODO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> taskService.updateTaskStatusByMember(otherMemberId, taskId, TaskStatus.IN_PROGRESS)
        );

        assertEquals("Bạn không có quyền cập nhật Task của người khác.", exception.getMessage());
        verify(taskRepository, never()).save(any());
        verify(activityLogService, never()).logActivity(anyLong(), anyString());
    }

    @Test
    @DisplayName("Ném lỗi khi cập nhật trạng thái của Task không tồn tại")
    void updateTaskStatus_ThrowsException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> taskService.updateTaskStatusByMember(memberId, taskId, TaskStatus.IN_PROGRESS)
        );

        verify(taskRepository, never()).save(any());
    }
}
