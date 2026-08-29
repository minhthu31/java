package vn.edu.cnpm.projectsupport.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskStatusAssigneeUpdateTests {

    private static final long GROUP_ID = 8200L;
    private static final long PROJECT_ID = 8201L;
    private static final long LEADER_ID = 8202L;
    private static final long MEMBER_A_ID = 8203L;
    private static final long MEMBER_B_ID = 8204L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JiraClient jiraClient;

    @MockitoBean(name = "projectAuthorization")
    private ProjectAuthorizationService projectAuthorization;

    private Long taskId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM group_members WHERE group_id = ?", GROUP_ID);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", PROJECT_ID);
        jdbcTemplate.update("DELETE FROM student_groups WHERE id = ?", GROUP_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?, ?)", LEADER_ID, MEMBER_A_ID, MEMBER_B_ID);

        Long defaultRoleId = 1L;
        try {
            var roles = jdbcTemplate.query("SELECT id FROM roles LIMIT 1", (rs, rowNum) -> rs.getLong("id"));
            if (!roles.isEmpty()) {
                defaultRoleId = roles.get(0);
            }
        } catch (Exception ignored) {}

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, role_id, password_hash) VALUES (?, 'leader82', 'leader@cnpm.vn', 'Leader 82', ?, '$2a$10$defaultHashForTestingPurposeOnly')",
                LEADER_ID, defaultRoleId);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, role_id, password_hash) VALUES (?, 'member82a', 'membera@cnpm.vn', 'Member A 82', ?, '$2a$10$defaultHashForTestingPurposeOnly')",
                MEMBER_A_ID, defaultRoleId);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, role_id, password_hash) VALUES (?, 'member82b', 'memberb@cnpm.vn', 'Member B 82', ?, '$2a$10$defaultHashForTestingPurposeOnly')",
                MEMBER_B_ID, defaultRoleId);

        jdbcTemplate.update("INSERT INTO student_groups (id, code, name) VALUES (?, 'CNPM-82', 'Group 82')", GROUP_ID);
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (?, ?, 'Project 82')", PROJECT_ID, GROUP_ID);

        jdbcTemplate.update("INSERT INTO group_members (group_id, user_id) VALUES (?, ?)", GROUP_ID, LEADER_ID);
        jdbcTemplate.update("INSERT INTO group_members (group_id, user_id) VALUES (?, ?)", GROUP_ID, MEMBER_A_ID);
        jdbcTemplate.update("INSERT INTO group_members (group_id, user_id) VALUES (?, ?)", GROUP_ID, MEMBER_B_ID);

        Task task = new Task(PROJECT_ID, "Task 82 Initial", "Acceptance Criteria", TaskIssueType.TASK, TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TO_DO);
        task.setAssigneeUserId(MEMBER_A_ID);
        task.setSyncStatus(SyncStatus.NOT_SYNCED);
        taskId = taskRepository.save(task).getId();
    }

    @Test
    @DisplayName("Team Member cập nhật hợp lệ trạng thái Task được giao cho mình (TO_DO -> IN_PROGRESS)")
    void memberUpdatesAssignedTaskStatusSuccessfully() throws Exception {
        when(projectAuthorization.currentUserId()).thenReturn(MEMBER_A_ID);
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(false);
        when(projectAuthorization.canUpdateTask(PROJECT_ID, taskId)).thenReturn(true);

        String json = "{\"status\": \"IN_PROGRESS\"}";

        mockMvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/status", PROJECT_ID, taskId)
                        .with(user("member82a").roles("TEAM_MEMBER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        Task updated = taskRepository.findById(taskId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Chuyển trạng thái sai quy tắc ma trận trạng thái (TO_DO -> DONE) bị từ chối 422 Unprocessable Entity")
    void invalidStatusTransitionIsRejected() throws Exception {
        when(projectAuthorization.currentUserId()).thenReturn(LEADER_ID);
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);
        when(projectAuthorization.canUpdateTask(PROJECT_ID, taskId)).thenReturn(true);

        String json = "{\"status\": \"DONE\"}";

        mockMvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/status", PROJECT_ID, taskId)
                        .with(user("leader82").roles("TEAM_LEADER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Team Leader cập nhật người thực hiện (Assignee) thành công")
    void leaderUpdatesAssigneeSuccessfully() throws Exception {
        when(projectAuthorization.currentUserId()).thenReturn(LEADER_ID);
        when(projectAuthorization.isCurrentUserLeader(PROJECT_ID)).thenReturn(true);
        when(projectAuthorization.canManageTasks(PROJECT_ID)).thenReturn(true);

        String json = "{\"assigneeUserId\": " + MEMBER_B_ID + "}";

        mockMvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/assignee", PROJECT_ID, taskId)
                        .with(user("leader82").roles("TEAM_LEADER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeUserId").value(MEMBER_B_ID));

        Task updated = taskRepository.findById(taskId).orElseThrow();
        assertThat(updated.getAssigneeUserId()).isEqualTo(MEMBER_B_ID);
    }
}