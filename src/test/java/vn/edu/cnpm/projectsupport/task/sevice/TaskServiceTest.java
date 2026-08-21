package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private CreateTaskRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTaskRequest();
        validRequest.setTitle("Viết Unit Test cho Task Service");
        validRequest.setDescription("Mô tả chi tiết task");
        validRequest.setAcceptanceCriteria("Phủ > 80% code coverage");
        validRequest.setIssueType(TaskIssueType.TASK);
        validRequest.setPriority(TaskPriority.HIGH);
        validRequest.setFeatureId(10L);
        validRequest.setAssigneeUserId(100L);
    }

    @Test
    @DisplayName("Tạo Task thành công với syncStatus mặc định là NOT_SYNCED")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.createTask(1L, 1000L, validRequest);

        assertNotNull(createdTask);
        assertEquals(SyncStatus.NOT_SYNCED, createdTask.getSyncStatus());
        assertEquals(TaskStatus.TO_DO, createdTask.getStatus());
        assertEquals(TaskPriority.HIGH, createdTask.getPriority());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Ném lỗi khi Acceptance Criteria bị trống")
    void createTask_ThrowsException_WhenAcceptanceCriteriaIsEmpty() {
        validRequest.setAcceptanceCriteria("");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskService.createTask(1L, 1000L, validRequest)
        );

        assertEquals("Acceptance Criteria không được để trống.", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Member cập nhật trạng thái Task thành công")
    void updateTaskStatus_Success_WhenAssignedToMember() {
        Task mockTask = new Task(1L, "Viết Unit Test", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(100L);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task updatedTask = taskService.updateTaskStatusByMember(100L, 1L, TaskStatus.IN_PROGRESS, null);

        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố tình cập nhật Task của người khác")
    void updateTaskStatus_ThrowsException_WhenNotAssignedMember() {
        Task mockTask = new Task(1L, "Viết Unit Test", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(100L);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> taskService.updateTaskStatusByMember(200L, 1L, TaskStatus.IN_PROGRESS, null)
        );

        assertEquals("Bạn không có quyền cập nhật Task của người khác.", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi chuyển sang BLOCKED nhưng không truyền lý do")
    void updateTaskStatus_ThrowsException_WhenBlockedWithoutReason() {
        Task mockTask = new Task(1L, "Viết Unit Test", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(100L);
        mockTask.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskService.updateTaskStatusByMember(100L, 1L, TaskStatus.BLOCKED, "")
        );

        assertEquals("Cần cung cấp lý do khi chuyển trạng thái sang BLOCKED", exception.getMessage());
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố gắng chuyển từ TO_DO sang DONE trực tiếp")
    void updateTaskStatus_ThrowsException_WhenDirectFromToDoToDone() {
        Task mockTask = new Task(1L, "Viết Unit Test", "Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(100L);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> taskService.updateTaskStatusByMember(100L, 1L, TaskStatus.DONE, null)
        );

        assertEquals("Team Member không được chuyển trực tiếp từ TO_DO sang DONE.", exception.getMessage());
    }
}