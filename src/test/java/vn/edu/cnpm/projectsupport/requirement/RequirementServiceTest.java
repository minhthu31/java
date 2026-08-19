package vn.edu.cnpm.projectsupport.requirement;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

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
    }
}
