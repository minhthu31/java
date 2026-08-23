package vn.edu.cnpm.projectsupport.requirement;

feature/CNPM-68-Write-Requirement-and-Task-Service
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository; main

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

 feature/CNPM-68-Write-Requirement-and-Task-Service
    @InjectMocks
    private RequirementServiceImpl requirementService;

    private RequirementCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RequirementCreateRequest();
        validRequest.setTitle("Quản lý yêu cầu Sprint 2");
        validRequest.setDescription("Mô tả chi tiết requirement cho hệ thống");
    }

    @Test
    @DisplayName("Test tạo Requirement hợp lệ")
    void createRequirement_Success() {
        Long projectId = 1L;
        Requirement mockSaved = new Requirement();
        mockSaved.setId(100L);
        mockSaved.setProjectId(projectId);
        mockSaved.setTitle(validRequest.getTitle());
        mockSaved.setDescription(validRequest.getDescription());

        when(requirementRepository.save(any(Requirement.class))).thenReturn(mockSaved);

        RequirementResponse response = requirementService.createRequirement(projectId, validRequest);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Quản lý yêu cầu Sprint 2", response.getTitle());
        verify(requirementRepository, times(1)).save(any(Requirement.class));
    }

    @Test
    @DisplayName("Test dữ liệu không hợp lệ - Ném lỗi khi ID không tồn tại")
    void getRequirementById_ThrowsException_WhenNotFound() {
        Long projectId = 1L;
        Long nonExistentId = 999L;

        when(requirementRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(
            ResourceNotFoundException.class,
            () -> requirementService.getRequirementById(projectId, nonExistentId)
        );
        verify(requirementRepository, times(1)).findById(nonExistentId);
=======
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    private RequirementService service;

    @BeforeEach
    void setUp() {
        service = new RequirementService(requirementRepository, projectRepository, taskRepository);
    }

    @Test
    void createUsesPathProjectAndDefaultsToDraft() {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("  Login requirement  ");
        request.setActor("Member");
        request.setPriority(Priority.HIGH);
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.save(any(Requirement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RequirementResponse response = service.createRequirement(10L, request);

        assertEquals(10L, response.getProjectId());
        assertEquals("Login requirement", response.getTitle());
        assertEquals(RequirementStatus.DRAFT, response.getStatus());
        ArgumentCaptor<Requirement> captor = ArgumentCaptor.forClass(Requirement.class);
        verify(requirementRepository).save(captor.capture());
        assertEquals("Member", captor.getValue().getActor());
    }

    @Test
    void createRejectsUnknownProject() {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Requirement");
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.createRequirement(999L, request));
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void statusTransitionFollowsContract() {
        Requirement requirement = new Requirement(10L, "Requirement");
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));
        when(requirementRepository.save(requirement)).thenReturn(requirement);

        RequirementStatusUpdateRequest request =
                new RequirementStatusUpdateRequest(RequirementStatus.APPROVED);
        RequirementResponse response = service.updateStatus(10L, 20L, request);

        assertEquals(RequirementStatus.APPROVED, response.getStatus());
    }

    @Test
    void archivedRequirementIsTerminal() {
        Requirement requirement = new Requirement(10L, "Requirement");
        requirement.setStatus(RequirementStatus.ARCHIVED);
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));

        RequirementStatusUpdateRequest request =
                new RequirementStatusUpdateRequest(RequirementStatus.DRAFT);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updateStatus(10L, 20L, request));
    }

    @Test
    void deleteRejectsRequirementReferencedByTask() {
        Requirement requirement = new Requirement(10L, "Requirement");
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));
        when(taskRepository.existsByRequirementId(20L)).thenReturn(true);

        assertThrows(ResourceInUseException.class,
                () -> service.deleteRequirement(10L, 20L));
        verify(requirementRepository, never()).delete(any(Requirement.class));
 main
    }
}
