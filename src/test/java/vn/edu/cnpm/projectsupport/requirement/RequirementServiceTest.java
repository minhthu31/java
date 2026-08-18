package vn.edu.cnpm.projectsupport.requirement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.common.exception.AccessDeniedException;
import vn.edu.cnpm.projectsupport.group.GroupService;
import vn.edu.cnpm.projectsupport.project.ProjectService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private GroupService groupService;

    @InjectMocks
    private RequirementService requirementService;

    private final String userId = "user-123";
    private final String projectId = "proj-123";
    private final String groupId = "group-123";
    private final String reqId = "req-123";

    @BeforeEach
    void setUp() {
        lenient().when(projectService.getGroupIdByProjectId(projectId)).thenReturn(groupId);
    }

    @Test
    void create_WhenUserIsLeader_ShouldSucceed() {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setProjectId(projectId);
        request.setTitle("New Requirement");
        request.setPriority(Priority.HIGH);

        when(groupService.isLeader(groupId, userId)).thenReturn(true);
        when(requirementRepository.save(any(Requirement.class))).thenAnswer(i -> {
            Requirement r = i.getArgument(0);
            r.setId(reqId);
            return r;
        });

        RequirementResponse response = requirementService.create(request, userId);

        assertNotNull(response);
        assertEquals(reqId, response.getId());
        verify(requirementRepository).save(any(Requirement.class));
    }

    @Test
    void create_WhenUserIsNotLeader_ShouldThrowAccessDeniedException() {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setProjectId(projectId);

        when(groupService.isLeader(groupId, userId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> requirementService.create(request, userId));
        verify(requirementRepository, never()).save(any());
    }

    @Test
    void delete_SoftDelete_ShouldSetIsDeletedTrue() {
        Requirement req = Requirement.builder().id(reqId).projectId(projectId).isDeleted(false).build();
        when(requirementRepository.findByIdAndIsDeletedFalse(reqId)).thenReturn(Optional.of(req));
        when(groupService.isLeader(groupId, userId)).thenReturn(true);

        requirementService.delete(reqId, false, userId);

        assertTrue(req.getIsDeleted());
        verify(requirementRepository).save(req);
    }

    @Test
    void getList_WithoutProjectId_ShouldThrowAccessDeniedException() {
        RequirementFilterRequest filter = new RequirementFilterRequest(); // projectId is null

        assertThrows(AccessDeniedException.class, () -> requirementService.getList(filter, null, userId));
    }
}