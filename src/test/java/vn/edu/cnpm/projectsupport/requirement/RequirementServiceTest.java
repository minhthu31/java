package vn.edu.cnpm.projectsupport.requirement;

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
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

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
    }
}
