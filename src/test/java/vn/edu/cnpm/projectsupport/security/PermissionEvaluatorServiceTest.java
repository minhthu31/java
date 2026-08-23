package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionEvaluatorServiceTest {

    private PermissionEvaluatorService permissionEvaluator;

    @BeforeEach
    void setUp() {
        permissionEvaluator = new PermissionEvaluatorService();
    }

    @Test
    @DisplayName("Permission Check - LECTURER chỉ được truy cập Project được phân công")
    void testLecturerProjectScope() {
        assertTrue(permissionEvaluator.isLecturerAssignedToProject("lecturer1", 101L));
        assertFalse(permissionEvaluator.isLecturerAssignedToProject("lecturer1", 999L));
    }

    @Test
    @DisplayName("Permission Check - TEAM_LEADER chỉ quản lý dữ liệu thuộc nhóm mình")
    void testTeamLeaderGroupScope() {
        assertTrue(permissionEvaluator.isLeaderOfGroup("leader1", 50L));
        assertFalse(permissionEvaluator.isLeaderOfGroup("leader1", 99L));
    }

    @Test
    @DisplayName("Permission Check - TEAM_MEMBER chỉ xem và cập nhật Task nếu là Assignee")
    void testTaskAssigneePermission() {
        assertTrue(permissionEvaluator.isTaskAssignee("member1", "member1"));
        assertFalse(permissionEvaluator.isTaskAssignee("member1", "member2"));
    }

    @Test
    @DisplayName("Permission Check - TEAM_MEMBER không được sửa Task của người khác")
    void testMemberCannotModifyOtherTask() {
        assertFalse(permissionEvaluator.isTaskAssignee("member1", "member2"));
    }

    @Test
    @DisplayName("Permission Check - ADMIN không thực hiện thao tác trên tài nguyên học thuật theo matrix")
    void testAdminAcademicResourceRestriction() {
        assertFalse(permissionEvaluator.canAdminModifyAcademicResource("ADMIN"));
    }
}
