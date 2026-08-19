package vn.edu.cnpm.projectsupport.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.entity.Task;
import vn.edu.cnpm.projectsupport.task.enums.SyncStatus;
import vn.edu.cnpm.projectsupport.task.enums.TaskStatus;
import vn.edu.cnpm.projectsupport.task.enums.TaskType;
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
        validRequest.setType(TaskType.TEST);
        validRequest.setProjectId("PROJ_01");
        validRequest.setFeatureId("FEAT_01");
        validRequest.setAssigneeId("MEMBER_01");
    }

    @Test
    @DisplayName("Tạo Task thành công và giao đúng người")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task createdTask = taskService.createTask("LEADER_01", validRequest);

        assertNotNull(createdTask);
        assertEquals("LEADER_01", createdTask.getCreatedById());
        assertEquals("MEMBER_01", createdTask.getAssigneeId());
        assertEquals(SyncStatus.NOT_SYNCED, createdTask.getSyncStatus());
        assertEquals(TaskStatus.TODO, createdTask.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Ném lỗi khi Acceptance Criteria bị trống")
    void createTask_ThrowsException_WhenAcceptanceCriteriaIsEmpty() {
        validRequest.setAcceptanceCriteria("");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> taskService.createTask("LEADER_01", validRequest)
        );

        assertEquals("Acceptance Criteria không được để trống.", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Member cập nhật trạng thái Task thành công khi đúng người được giao")
    void updateTaskStatus_Success_WhenAssignedToMember() {
        Task mockTask = new Task();
        mockTask.setId("TASK_01");
        mockTask.setAssigneeId("MEMBER_01");
        mockTask.setStatus(TaskStatus.TODO);

        when(taskRepository.findById("TASK_01")).thenReturn(Optional.of(mockTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task updatedTask = taskService.updateTaskStatusByMember("MEMBER_01", "TASK_01", TaskStatus.IN_PROGRESS);

        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Ném lỗi khi Member cố tình cập nhật Task của người khác")
    void updateTaskStatus_ThrowsException_WhenNotAssignedMember() {
        Task mockTask = new Task();
        mockTask.setId("TASK_01");
        mockTask.setAssigneeId("MEMBER_01");
        mockTask.setStatus(TaskStatus.TODO);

        when(taskRepository.findById("TASK_01")).thenReturn(Optional.of(mockTask));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> taskService.updateTaskStatusByMember("MEMBER_02", "TASK_01", TaskStatus.IN_PROGRESS)
        );

        assertEquals("Bạn không có quyền cập nhật Task của người khác.", exception.getMessage());
        verify(taskRepository, never()).save(any());
    }
}
