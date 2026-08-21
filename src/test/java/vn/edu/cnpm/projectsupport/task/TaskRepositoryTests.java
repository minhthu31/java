package vn.edu.cnpm.projectsupport.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTests {

    @Autowired TaskRepository taskRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpProject() {
        jdbcTemplate.update("INSERT INTO student_groups (id, code, name) VALUES (9401, 'TEST-TASK', 'Task Test Group')");
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (9501, 9401, 'Task Test Project')");
    }

    @Test
    void saveAndFindTask() {
        Task task = task("Login API");
        task.setClassification(TaskClassification.NEW_FEATURE);
        task.setIdempotencyKey("idem-1");

        Task saved = taskRepository.saveAndFlush(task);
        Task found = taskRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getProjectId()).isEqualTo(9501L);
        assertThat(found.getRequirementId()).isNull();
        assertThat(found.getIssueType()).isEqualTo(TaskIssueType.TASK);
        assertThat(found.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(found.getClassification()).isEqualTo(TaskClassification.NEW_FEATURE);
        assertThat(found.getStatus()).isEqualTo(TaskStatus.TO_DO);
        assertThat(found.getSyncStatus()).isEqualTo(SyncStatus.NOT_SYNCED);
    }

    @Test
    void requirementIdCanBeNull() {
        Task saved = taskRepository.saveAndFlush(task("Without requirement"));
        assertThat(saved.getRequirementId()).isNull();
    }

    @Test
    void projectReferenceMustExist() {
        assertThatThrownBy(() -> taskRepository.saveAndFlush(
                new Task(999999L, "Invalid project", "Criteria", TaskIssueType.TASK, TaskPriority.HIGH)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void idempotencyKeyCannotBeDuplicated() {
        taskRepository.saveAndFlush(taskWithKey("First", "same-key"));
        assertThatThrownBy(() -> taskRepository.saveAndFlush(taskWithKey("Second", "same-key")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void requiredFieldsCannotBeNull() {
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(null, "Title", "Criteria", TaskIssueType.TASK, TaskPriority.HIGH)))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(9501L, null, "Criteria", TaskIssueType.TASK, TaskPriority.HIGH)))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(9501L, "Title", null, TaskIssueType.TASK, TaskPriority.HIGH)))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(9501L, "Title", "Criteria", null, TaskPriority.HIGH)))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> taskRepository.saveAndFlush(new Task(9501L, "Title", "Criteria", TaskIssueType.TASK, null)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void repositoryFiltersByProjectAssigneeStatusAndType() {
        Task task = taskWithKey("Login", "filter-key");
        Long assigneeId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'member.test'", Long.class);
        task.setAssigneeUserId(assigneeId);
        taskRepository.saveAndFlush(task);

        assertThat(taskRepository.findByProjectId(9501L)).hasSize(1);
        assertThat(taskRepository.findByAssigneeUserId(assigneeId)).hasSize(1);
        assertThat(taskRepository.findByStatus(TaskStatus.TO_DO)).hasSize(1);
        assertThat(taskRepository.findByIssueType(TaskIssueType.TASK)).hasSize(1);
    }

    private Task task(String title) {
        return new Task(9501L, title, "Acceptance criteria", TaskIssueType.TASK, TaskPriority.HIGH);
    }

    private Task taskWithKey(String title, String key) {
        Task task = task(title);
        task.setIdempotencyKey(key);
        return task;
    }
}
