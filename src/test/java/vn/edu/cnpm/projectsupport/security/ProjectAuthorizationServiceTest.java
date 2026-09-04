package vn.edu.cnpm.projectsupport.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectAuthorizationServiceTest {

    @Autowired
    private ProjectAuthorizationService authorization;

    @Autowired
    private RequirementService requirementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long projectId;
    private Long otherProjectId;
    private Long assignedTaskId;
    private Long unassignedTaskId;

    @BeforeEach
    void setUpScope() {
        Long leaderId = userId("leader.test");
        Long memberId = userId("member.test");
        Long lecturerId = userId("lecturer.test");

        jdbcTemplate.update("""
                INSERT INTO student_groups (code, name, leader_user_id, status)
                VALUES ('AUTH-G1', 'Authorized Group', ?, 'ACTIVE')
                """, leaderId);
        jdbcTemplate.update("""
                INSERT INTO student_groups (code, name, status)
                VALUES ('AUTH-G2', 'Other Group', 'ACTIVE')
                """);

        Long groupId = id("SELECT id FROM student_groups WHERE code = 'AUTH-G1'");
        Long otherGroupId = id("SELECT id FROM student_groups WHERE code = 'AUTH-G2'");

        jdbcTemplate.update("""
                INSERT INTO group_members (group_id, user_id, member_role, status)
                VALUES (?, ?, 'TEAM_MEMBER', 'ACTIVE')
                """, groupId, memberId);
        jdbcTemplate.update("""
                INSERT INTO group_lecturers (group_id, lecturer_user_id)
                VALUES (?, ?)
                """, groupId, lecturerId);
        jdbcTemplate.update("""
                INSERT INTO projects (group_id, name, status)
                VALUES (?, 'Authorized Project', 'ACTIVE')
                """, groupId);
        jdbcTemplate.update("""
                INSERT INTO projects (group_id, name, status)
                VALUES (?, 'Other Project', 'ACTIVE')
                """, otherGroupId);

        projectId = id("SELECT id FROM projects WHERE name = 'Authorized Project'");
        otherProjectId = id("SELECT id FROM projects WHERE name = 'Other Project'");

        jdbcTemplate.update("""
                INSERT INTO tasks (
                    project_id, assignee_user_id, title, acceptance_criteria,
                    issue_type, priority, status, sync_status)
                VALUES (?, ?, 'Assigned Task', 'Complete it', 'TASK', 'HIGH', 'TO_DO', 'NOT_SYNCED')
                """, projectId, memberId);
        jdbcTemplate.update("""
                INSERT INTO tasks (
                    project_id, title, acceptance_criteria,
                    issue_type, priority, status, sync_status)
                VALUES (?, 'Unassigned Task', 'Complete it', 'TASK', 'HIGH', 'TO_DO', 'NOT_SYNCED')
                """, projectId);
        assignedTaskId = id("SELECT id FROM tasks WHERE title = 'Assigned Task'");
        unassignedTaskId = id("SELECT id FROM tasks WHERE title = 'Unassigned Task'");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leaderCanManageOnlyOwnedProject() {
        authenticate("leader.test", "TEAM_LEADER");

        assertTrue(authorization.canViewRequirements(projectId));
        assertTrue(authorization.canManageRequirements(projectId));
        assertTrue(authorization.canViewTasks(projectId));
        assertTrue(authorization.canViewTask(projectId, assignedTaskId));
        assertTrue(authorization.canManageTasks(projectId));
        assertTrue(authorization.canGenerateSrs(projectId));
        assertFalse(authorization.canManageRequirements(otherProjectId));
        assertFalse(authorization.canViewTasks(otherProjectId));
    }

    @Test
    void lecturerHasReadOnlyAccessToAssignedGroup() {
        authenticate("lecturer.test", "LECTURER");

        assertTrue(authorization.canViewRequirements(projectId));
        assertTrue(authorization.canViewTasks(projectId));
        assertTrue(authorization.canViewTask(projectId, assignedTaskId));
        assertTrue(authorization.canViewSrs(projectId));
        assertFalse(authorization.canManageRequirements(projectId));
        assertFalse(authorization.canManageTasks(projectId));
        assertFalse(authorization.canViewRequirements(otherProjectId));
        assertFalse(authorization.canViewTasks(otherProjectId));
    }

    @Test
    void memberCanOnlyViewAndUpdateAssignedTask() {
        authenticate("member.test", "TEAM_MEMBER");

        assertTrue(authorization.canViewTasks(projectId));
        assertTrue(authorization.canViewTask(projectId, assignedTaskId));
        assertTrue(authorization.canUpdateTask(projectId, assignedTaskId));
        assertFalse(authorization.canViewTask(projectId, unassignedTaskId));
        assertFalse(authorization.canUpdateTask(projectId, unassignedTaskId));
        assertFalse(authorization.canViewTasks(otherProjectId));
        assertTrue(authorization.isCurrentUserTeamMember(projectId));
        assertFalse(authorization.isCurrentUserLeader(projectId));
        assertFalse(authorization.canViewRequirements(projectId));
        assertFalse(authorization.canViewSrs(projectId));
    }

    @Test
    void adminCannotModifyAcademicResources() {
        authenticate("admin.test", "ADMIN");

        assertFalse(authorization.canViewRequirements(projectId));
        assertFalse(authorization.canManageRequirements(projectId));
        assertFalse(authorization.canViewTasks(projectId));
        assertFalse(authorization.canUpdateTask(projectId, assignedTaskId));
        assertFalse(authorization.canGenerateSrs(projectId));
    }

    @Test
    void unauthenticatedUserHasNoProjectAccess() {
        SecurityContextHolder.clearContext();

        assertFalse(authorization.canViewRequirements(projectId));
        assertFalse(authorization.canManageRequirements(projectId));
        assertFalse(authorization.canViewTasks(projectId));
        assertFalse(authorization.canViewTask(projectId, assignedTaskId));
        assertFalse(authorization.canUpdateTask(projectId, assignedTaskId));
        assertThrows(AccessDeniedException.class, authorization::currentUserId);
    }

    @Test
    void methodSecurityRejectsMemberAndAllowsLeader() {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Authorization requirement");

        authenticate("member.test", "TEAM_MEMBER");
        assertThrows(AccessDeniedException.class,
                () -> requirementService.createRequirement(projectId, request));

        authenticate("leader.test", "TEAM_LEADER");
        requirementService.createRequirement(projectId, request);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM requirements WHERE project_id = ?", Long.class, projectId);
        assertTrue(count != null && count == 1L);
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Long userId(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private Long id(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
