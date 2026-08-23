package vn.edu.cnpm.projectsupport.requirement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private RequirementService requirementService;

    private RequirementCreateRequest validRequest;
    private final Long projectId = 1L;
    private final Long requirementId = 100L;

    @BeforeEach
    void setUp() {
        validRequest = new RequirementCreateRequest();
        validRequest.setTitle("Quản lý yêu cầu Sprint 2");
        validRequest.setDescription("Mô tả chi tiết requirement cho hệ thống");
    }

    @Test
    @DisplayName("Tạo Requirement thành công với trạng thái mặc định")
    void createRequirement_Success() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequirementResponse response = requirementService.createRequirement(projectId, validRequest);

        assertNotNull(response);
        verify(requirementRepository, times(1)).save(any(Requirement.class));
    }

    @Test
    @DisplayName("Ném lỗi khi tạo Requirement với Project không tồn tại")
    void createRequirement_ThrowsException_WhenProjectNotFound() {
        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThrows(
            ResourceNotFoundException.class,
            () -> requirementService.createRequirement(projectId, validRequest)
        );
        verify(requirementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném lỗi khi Requirement không tồn tại")
    void getRequirementById_ThrowsException_WhenNotFound() {
        when(requirementRepository.findById(requirementId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> requirementService.getRequirementById(projectId, requirementId)
        );
    }

    @Test
    @DisplayName("Không cho phép xóa Requirement khi đang có Task liên kết")
    void deleteRequirement_ThrowsException_WhenTasksExist() {
        when(requirementRepository.existsById(requirementId)).thenReturn(true);
        when(taskRepository.existsByRequirementId(requirementId)).thenReturn(true);

        assertThrows(
            RuntimeException.class,
            () -> requirementService.deleteRequirement(projectId, requirementId)
        );

        verify(requirementRepository, never()).deleteById(anyLong());
    }
}
