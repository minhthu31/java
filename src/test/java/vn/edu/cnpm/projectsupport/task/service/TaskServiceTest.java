package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
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
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private CreateTaskRequest validRequest;
    private TaskStatusUpdateRequest updateRequest;
    private final Long leaderId = 10L;
    private final Long memberId = 20L;
    private final Long projectId = 1L;
    private final Long taskId = 100L;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTaskRequest();
        validRequest.setTitle("Viết Unit Test cho Task Service");
        validRequest.setDescription("Mô tả chi tiết task");
        validRequest.setAcceptanceCriteria("Phủ > 80% code coverage");
        validRequest.setIssueType(TaskIssueType.TASK);
        validRequest.setPriority(TaskPriority.MEDIUM);
        validRequest.setRequirementId(5L);
        validRequest.setAssigneeUserId(memberId);

        updateRequest = new TaskStatusUpdateRequest();
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Tạo Task thành công")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse createdTask = taskService.createTask(leaderId, validRequest);

        assertNotNull(createdTask);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Member cập nhật trạng thái hợp lệ")
    void updateTaskStatus_Success_WhenAssignedMember() {
        Task mockTask = new Task(projectId, "Test Task", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(memberId);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse updatedTask = taskService.updateTaskStatusByMember(projectId, taskId, memberId, updateRequest);

        assertNotNull(updatedTask);
        verify(taskRepository, times(1)).save(mockTask);
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố tình cập nhật Task của người khác (sai quyền)")
    void updateTaskStatus_ThrowsException_WhenNotAssignedMember() {
        Task mockTask = new Task(projectId, "Test Task", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        mockTask.setAssigneeUserId(memberId);
        mockTask.setStatus(TaskStatus.TO_DO);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(mockTask));

        Long otherMemberId = 30L;
        assertThrows(
            RuntimeException.class,
            () -> taskService.updateTaskStatusByMember(projectId, taskId, otherMemberId, updateRequest)
        );

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi cập nhật trạng thái của Task không tồn tại")
    void updateTaskStatus_ThrowsException_WhenTaskNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> taskService.updateTaskStatusByMember(projectId, taskId, memberId, updateRequest)
        );

        verify(taskRepository, never()).save(any());
    }
}
