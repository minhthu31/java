package vn.edu.cnpm.projectsupport.requirement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Test
    void listClampsPaginationAndFallsBackToSafeSort() {
        Requirement requirement = new Requirement(10L, "Requirement");
        RequirementFilterRequest filter = new RequirementFilterRequest();
        filter.setPage(-5);
        filter.setSize(500);
        filter.setSort("unknownField,asc");
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Requirement>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(requirement)));

        service.getRequirements(10L, filter);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(requirementRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Requirement>>any(),
                pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("updatedAt").getDirection());
    }

    @Test
    void getByIdRejectsRequirementOutsideProjectScope() {
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRequirementById(10L, 20L));

        verify(requirementRepository).findByIdAndProjectId(20L, 10L);
    }

    @Test
    void updateTrimsTitleAndPersistsContent() {
        Requirement requirement = new Requirement(10L, "Old title");
        RequirementUpdateRequest request = new RequirementUpdateRequest();
        request.setTitle("  Updated title  ");
        request.setDescription("Updated description");
        request.setActor("Team Leader");
        request.setPriority(Priority.HIGH);
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));
        when(requirementRepository.save(requirement)).thenReturn(requirement);

        RequirementResponse response = service.updateRequirement(10L, 20L, request);

        assertEquals("Updated title", response.getTitle());
        assertEquals("Updated description", response.getDescription());
        assertEquals("Team Leader", response.getActor());
        assertEquals(Priority.HIGH, response.getPriority());
        verify(requirementRepository).save(requirement);
    }

    @Test
    void deleteRemovesDraftRequirementWithoutTaskReferences() {
        Requirement requirement = new Requirement(10L, "Requirement");
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));
        when(taskRepository.existsByRequirementId(20L)).thenReturn(false);

        service.deleteRequirement(10L, 20L);

        verify(requirementRepository).delete(requirement);
    }

    @Test
    void deleteRejectsNonDraftRequirement() {
        Requirement requirement = new Requirement(10L, "Requirement");
        requirement.setStatus(RequirementStatus.APPROVED);
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(requirementRepository.findByIdAndProjectId(20L, 10L))
                .thenReturn(Optional.of(requirement));

        assertThrows(ResourceInUseException.class,
                () -> service.deleteRequirement(10L, 20L));

        verify(taskRepository, never()).existsByRequirementId(20L);
        verify(requirementRepository, never()).delete(any(Requirement.class));
    }
}
