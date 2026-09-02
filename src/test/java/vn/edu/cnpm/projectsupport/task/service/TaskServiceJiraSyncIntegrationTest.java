package vn.edu.cnpm.projectsupport.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClientException;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.TaskStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceJiraSyncIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JiraIssueRepository jiraIssueRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoBean
    private JiraClient jiraClient;

    @MockitoBean
    private ProjectAuthorizationService projectAuthorization;

    @Test
    @DisplayName("Integration: Khi Jira loi, Task syncStatus=SYNC_FAILED va SyncLog FAILED thuc su duoc persist vao DB")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateTaskStatus_jiraFails_persistsSyncLogToDatabase() {
        // 1. Setup Project va jira_project_key
        List<Project> projects = projectRepository.findAll();
        Project project = !projects.isEmpty() ? projects.get(0) : projectRepository.findById(10L).orElseThrow();
        Long projectId = project.getId();

        jdbcClient.sql("UPDATE projects SET jira_project_key = 'CNPM' WHERE id = :id")
                .param("id", projectId)
                .update();

        // 2. Tao Task trong DB test
        Task task = new Task(projectId, "Integration Sync Test Task", "AC Test", TaskIssueType.TASK, TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TO_DO);
        task.setSyncStatus(SyncStatus.SYNCED);
        task = taskRepository.save(task);
        Long taskId = task.getId();

        // 3. Tao JiraIssue mapping vao bang jira_issues
        String testIssueKey = "CNPM-IT-" + taskId;
        JiraIssue jiraIssue = new JiraIssue(taskId, "10999", testIssueKey, "https://jira.example.com/browse/" + testIssueKey, Instant.now());
        jiraIssueRepository.saveAndFlush(jiraIssue);

        // 4. Mock quyen Security theo yeu cau: canUpdateTask tra ve true de qua @PreAuthorize
        when(projectAuthorization.canUpdateTask(projectId, taskId)).thenReturn(true);
        when(projectAuthorization.currentUserId()).thenReturn(1L);
        when(projectAuthorization.isCurrentUserLeader(projectId)).thenReturn(true);

        // 5. Gia lap JiraClient nem loi JiraClientException
        doThrow(new JiraClientException("JIRA_UNAVAILABLE"))
                .when(jiraClient).transitionIssueStatus(anyLong(), anyString(), anyString(), anyString());

        TaskStatusUpdateRequest req = new TaskStatusUpdateRequest();
        req.setStatus(TaskStatus.IN_PROGRESS);

        // 6. Kiem tra chinh xac ngoai le la JiraClientException (khong bat chung RuntimeException)
        assertThatThrownBy(() -> taskService.updateTaskStatus(projectId, taskId, req))
                .isInstanceOf(JiraClientException.class);

        // 7. Verify method transitionIssueStatus da duoc goi
        verify(jiraClient).transitionIssueStatus(anyLong(), anyString(), anyString(), anyString());

        // 8. Kiem tra Task trong DB: status giu nguyen TO_DO va syncStatus tro thanh SYNC_FAILED
        Task reloadedTask = taskRepository.findById(taskId).orElseThrow();
        assertThat(reloadedTask.getStatus()).isEqualTo(TaskStatus.TO_DO);
        assertThat(reloadedTask.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);

        // 9. Kiem tra ban ghi SyncLog FAILED thuc su duoc insert vao DB that
        List<Map<String, Object>> logs = jdbcClient.sql("""
            SELECT provider, entity_type, entity_id, direction, status, error_code
            FROM sync_logs
            WHERE project_id = :pid AND entity_id = :key
            ORDER BY id DESC
        """)
        .param("pid", projectId)
        .param("key", testIssueKey)
        .query()
        .listOfRows();

        assertThat(logs).isNotEmpty();
        Map<String, Object> latestLog = logs.get(0);
        assertThat(String.valueOf(latestLog.get("PROVIDER"))).isEqualTo("JIRA");
        assertThat(String.valueOf(latestLog.get("DIRECTION"))).isEqualTo("EXPORT");
        assertThat(String.valueOf(latestLog.get("STATUS"))).isEqualTo("FAILED");
        assertThat(String.valueOf(latestLog.get("ERROR_CODE"))).isEqualTo("JIRA_UNAVAILABLE");
    }
}
