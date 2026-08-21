package vn.edu.cnpm.projectsupport.requirement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.project.ProjectRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private RequirementService requirementService;

    private String projectId;
    private String userId;

    @BeforeEach
    void setUp() {
        projectId = "proj-123";
        userId = "user-456";
    }

    @Test
    void create_WhenProjectExists_ShouldSucceed() {
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .projectId(projectId)
                .title("New Requirement")
                .description("Description")
                .priority(Priority.HIGH)
                .build();

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requirementRepository.save(any(Requirement.class)))
                .thenAnswer(invocation -> {
                    Requirement saved = invocation.getArgument(0);
                    saved.setId("req-1");
                    return saved;
                });

        RequirementResponse response = requirementService.create(request, userId);

        assertNotNull(response);
        assertEquals("req-1", response.getId());
        assertEquals("New Requirement", response.getTitle());
        verify(projectRepository).existsById(projectId);
        verify(requirementRepository).save(any(Requirement.class));
    }

    @Test
    void create_WhenProjectNotFound_ShouldThrowResourceNotFoundException() {
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .projectId(projectId)
                .build();

        when(projectRepository.existsById(projectId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, 
            () -> requirementService.create(request, userId));
            
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void delete_SoftDelete_ShouldSetIsDeletedTrue() {
        String reqId = "req-1";
        Requirement requirement = Requirement.builder()
                .id(reqId)
                .isDeleted(false)
                .build();

        when(requirementRepository.findByIdAndIsDeletedFalse(reqId))
                .thenReturn(Optional.of(requirement));

        requirementService.delete(reqId, false, userId);

        assertTrue(requirement.getIsDeleted());
        verify(requirementRepository).save(requirement);
    }
}