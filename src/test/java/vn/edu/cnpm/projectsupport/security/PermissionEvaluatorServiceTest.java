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
        assertTrue(permissionEvaluator.isLecturerAssignedToProject("LECTURER_01", 101L));
        assertFalse(permissionEvaluator.isLecturerAssignedToProject("LECTURER_01", 999L));
        assertFalse(permissionEvaluator.isLecturerAssignedToProject("LECTURER_OTHER", 101L));
    }

    @Test
    @DisplayName("Permission Check - TEAM_LEADER chỉ quản lý dữ liệu thuộc nhóm mình")
    void testTeamLeaderGroupScope() {
        assertTrue(permissionEvaluator.isLeaderOfGroup("LEADER_01", 50L));
        assertFalse(permissionEvaluator.isLeaderOfGroup("LEADER_01", 99L));
        assertFalse(permissionEvaluator.isLeaderOfGroup("LEADER_OTHER", 50L));
    }

    @Test
    @DisplayName("Permission Check - TEAM_MEMBER chỉ xem và cập nhật Task nếu là Assignee")
    void testTaskAssigneePermission() {
        assertTrue(permissionEvaluator.isTaskAssignee("MEMBER_01", "MEMBER_01"));
        assertFalse(permissionEvaluator.isTaskAssignee("MEMBER_01", "MEMBER_02"));
    }

    @Test
    @DisplayName("Permission Check - TEAM_MEMBER không được sửa Task của người khác")
    void testMemberCannotModifyOtherTask() {
        assertFalse(permissionEvaluator.isTaskAssignee("MEMBER_01", "MEMBER_02"));
    }

    @Test
    @DisplayName("Permission Check - ADMIN không thực hiện thao tác trên tài nguyên học thuật")
    void testAdminAcademicResourceRestriction() {
        assertFalse(permissionEvaluator.canAdminModifyAcademicResource("ADMIN"));
        assertFalse(permissionEvaluator.canAdminModifyAcademicResource("ROLE_ADMIN"));
    }
}
