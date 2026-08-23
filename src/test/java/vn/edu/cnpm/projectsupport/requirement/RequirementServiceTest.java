package vn.edu.cnpm.projectsupport.requirement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.BadRequestException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.dto.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.dto.RequirementResponse;
import vn.edu.cnpm.projectsupport.requirement.entity.Requirement;
import vn.edu.cnpm.projectsupport.requirement.enums.RequirementStatus;
import vn.edu.cnpm.projectsupport.requirement.repository.RequirementRepository;
import vn.edu.cnpm.projectsupport.requirement.service.RequirementServiceImpl;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private RequirementServiceImpl requirementService;

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
    @DisplayName("Tạo Requirement thành công với trạng thái mặc định là DRAFT")
    void createRequirement_Success_WithDefaultStatusDraft() {
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(invocation -> {
            Requirement req = invocation.getArgument(0);
            req.setId(requirementId);
            return req;
        });

        RequirementResponse response = requirementService.createRequirement(projectId, validRequest);

        assertNotNull(response);
        assertEquals(requirementId, response.getId());
        assertEquals(RequirementStatus.DRAFT, response.getStatus());
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
    @DisplayName("Ném lỗi khi Requirement không thuộc Project được chỉ định")
    void getRequirementById_ThrowsException_WhenRequirementNotBelongToProject() {
        Requirement requirement = new Requirement();
        requirement.setId(requirementId);
        requirement.setProjectId(2L);

        when(requirementRepository.findById(requirementId)).thenReturn(Optional.of(requirement));

        assertThrows(
            BadRequestException.class,
            () -> requirementService.getRequirementById(projectId, requirementId)
        );
    }

    @Test
    @DisplayName("Chuyển trạng thái Requirement hợp lệ (DRAFT -> APPROVED)")
    void updateStatus_Success_ValidTransition() {
        Requirement requirement = new Requirement();
        requirement.setId(requirementId);
        requirement.setProjectId(projectId);
        requirement.setStatus(RequirementStatus.DRAFT);

        when(requirementRepository.findByIdAndProjectId(requirementId, projectId))
                .thenReturn(Optional.of(requirement));
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(i -> i.getArgument(0));

        RequirementResponse response = requirementService.updateStatus(projectId, requirementId, RequirementStatus.APPROVED);

        assertEquals(RequirementStatus.APPROVED, response.getStatus());
        verify(requirementRepository, times(1)).save(requirement);
    }

    @Test
    @DisplayName("Ném lỗi khi chuyển trạng thái Requirement không hợp lệ (APPROVED -> DRAFT)")
    void updateStatus_ThrowsException_InvalidTransition() {
        Requirement requirement = new Requirement();
        requirement.setId(requirementId);
        requirement.setProjectId(projectId);
        requirement.setStatus(RequirementStatus.APPROVED);

        when(requirementRepository.findByIdAndProjectId(requirementId, projectId))
                .thenReturn(Optional.of(requirement));

        assertThrows(
            IllegalStateException.class,
            () -> requirementService.updateStatus(projectId, requirementId, RequirementStatus.DRAFT)
        );
        verify(requirementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không cho phép xóa Requirement khi đang có Task liên kết")
    void deleteRequirement_ThrowsException_WhenTasksExist() {
        when(requirementRepository.existsByIdAndProjectId(requirementId, projectId)).thenReturn(true);
        when(taskRepository.existsByRequirementId(requirementId)).thenReturn(true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> requirementService.deleteRequirement(projectId, requirementId)
        );

        assertEquals("Không thể xóa Requirement đang có Task liên kết.", exception.getMessage());
        verify(requirementRepository, never()).deleteById(anyLong());
    }
}
