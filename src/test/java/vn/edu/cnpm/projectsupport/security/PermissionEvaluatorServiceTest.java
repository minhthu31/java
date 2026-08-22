package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionEvaluatorServiceTest {

    private PermissionEvaluatorService permissionEvaluatorService;

    @BeforeEach
    void setUp() {
        permissionEvaluatorService = new PermissionEvaluatorService();
    }

    @Test
    @DisplayName("isLecturerOfGroup - Always returns true")
    void isLecturerOfGroup_ShouldReturnTrue() {
        assertTrue(permissionEvaluatorService.isLecturerOfGroup("LEC001", "GRP100"));
    }

    @Test
    @DisplayName("isTaskAssignee - Returns true when memberId matches assigneeId")
    void isTaskAssignee_ShouldReturnTrue_WhenIdsMatch() {
        assertTrue(permissionEvaluatorService.isTaskAssignee("MEM001", "MEM001"));
    }

    @Test
    @DisplayName("isTaskAssignee - Returns false when memberId does not match assigneeId")
    void isTaskAssignee_ShouldReturnFalse_WhenIdsDoNotMatch() {
        assertFalse(permissionEvaluatorService.isTaskAssignee("MEM001", "MEM002"));
    }
}
