package vn.edu.cnpm.projectsupport.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import vn.edu.cnpm.projectsupport.group.domain.StudentGroup;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.project.domain.Feature;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.domain.Sprint;
import vn.edu.cnpm.projectsupport.requirement.domain.Requirement;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private Project project;
    private Project otherProject;
    private User assignee;
    private User otherAssignee;

    @BeforeEach
    void setUp() {
        StudentGroup group = new StudentGroup();
        group.setCode("G1");
        group.setName("Group 1");
        entityManager.persist(group);

        project = newProject(group, "Project A");
        otherProject = newProject(group, "Project B");

        assignee = userRepository.findByUsernameIgnoreCase("member.test").orElseThrow();
        otherAssignee = userRepository.findByUsernameIgnoreCase("leader.test").orElseThrow();

        entityManager.flush();
    }

    private Project newProject(StudentGroup group, String name) {
        Project value = new Project();
        value.setGroup(group);
        value.setName(name);
        value.setStatus("ACTIVE");
        return entityManager.persist(value);
    }

    private Requirement newRequirement(Project project) {
        Requirement value = new Requirement();
        value.setProject(project);
        value.setTitle("Requirement " + project.getName());
        value.setStatus("DRAFT");
        return entityManager.persist(value);
    }

    private Feature newFeature(Project project) {
        Feature value = new Feature();
        value.setProject(project);
        value.setName("Feature " + project.getName());
        return entityManager.persist(value);
    }

    private Sprint newSprint(Project project) {
        Sprint value = new Sprint();
        value.setProject(project);
        value.setName("Sprint " + project.getName());
        value.setState("ACTIVE");
        return entityManager.persist(value);
    }

    private Task newTask(Project taskProject, User taskAssignee) {
        Task task = new Task();
        task.setProject(taskProject);
        task.setRequirement(newRequirement(taskProject));
        task.setFeature(newFeature(taskProject));
        task.setSprint(newSprint(taskProject));
        task.setAssignee(taskAssignee);
        task.setTitle("Task title");
        task.setAcceptanceCriteria("Acceptance criteria");
        task.setIssueType(TaskIssueType.TASK);
        task.setPriority(TaskPriority.MEDIUM);
        task.setClassification(TaskClassification.FEATURE_RELATED);
        return task;
    }

    @Test
    void taskLinksProjectRequirementFeatureSprintAndAssignee() {
        Task saved = taskRepository.saveAndFlush(newTask(project, assignee));
        entityManager.clear();

        Task reloaded = taskRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getRequirement().getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getFeature().getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getSprint().getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getAssignee().getId()).isEqualTo(assignee.getId());
    }

    @Test
    void taskCannotBePersistedWithoutProject() {
        Task task = new Task();
        task.setTitle("Orphan task");
        task.setAcceptanceCriteria("Acceptance criteria");
        task.setIssueType(TaskIssueType.TASK);
        task.setPriority(TaskPriority.MEDIUM);

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskCannotLinkRequirementFromAnotherProject() {
        Task task = newTask(project, assignee);
        task.setRequirement(newRequirement(otherProject));

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskCannotLinkFeatureFromAnotherProject() {
        Task task = newTask(project, assignee);
        task.setFeature(newFeature(otherProject));

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskCannotLinkSprintFromAnotherProject() {
        Task task = newTask(project, assignee);
        task.setSprint(newSprint(otherProject));

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskHasContractDefaults() {
        Task task = newTask(project, assignee);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TO_DO);
        assertThat(task.getSyncStatus()).isEqualTo(SyncStatus.NOT_SYNCED);
    }

    @Test
    void taskRequiredFieldsAreMappedAsNotNull() {
        Task task = newTask(project, assignee);
        task.setTitle(null);

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskAcceptanceCriteriaCannotBeNull() {
        Task task = newTask(project, assignee);
        task.setAcceptanceCriteria(null);

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskIssueTypeCannotBeNull() {
        Task task = newTask(project, assignee);
        task.setIssueType(null);

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void taskPriorityCannotBeNull() {
        Task task = newTask(project, assignee);
        task.setPriority(null);

        assertThrows(Exception.class, () -> taskRepository.saveAndFlush(task));
    }

    @Test
    void shouldFindTasksByProject() {
        taskRepository.saveAndFlush(newTask(project, assignee));
        taskRepository.saveAndFlush(newTask(otherProject, assignee));

        List<Task> result = taskRepository.findByProject_Id(project.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProject().getId()).isEqualTo(project.getId());
    }

    @Test
    void shouldFindTasksByAssignee() {
        taskRepository.saveAndFlush(newTask(project, assignee));
        taskRepository.saveAndFlush(newTask(project, otherAssignee));

        List<Task> result = taskRepository.findByAssignee_Id(assignee.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignee().getId()).isEqualTo(assignee.getId());
    }

    @Test
    void shouldFindTasksByStatus() {
        Task done = newTask(project, assignee);
        done.setStatus(TaskStatus.DONE);
        taskRepository.saveAndFlush(done);

        Task todo = newTask(project, assignee);
        todo.setStatus(TaskStatus.TO_DO);
        taskRepository.saveAndFlush(todo);

        List<Task> result = taskRepository.findByStatus(TaskStatus.DONE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void shouldFindTasksByIssueType() {
        Task bug = newTask(project, assignee);
        bug.setIssueType(TaskIssueType.BUG);
        taskRepository.saveAndFlush(bug);

        taskRepository.saveAndFlush(newTask(project, assignee));

        List<Task> result = taskRepository.findByIssueType(TaskIssueType.BUG);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIssueType()).isEqualTo(TaskIssueType.BUG);
    }

    @Test
    void shouldFindTasksByProjectAndAssignee() {
        taskRepository.saveAndFlush(newTask(project, assignee));
        taskRepository.saveAndFlush(newTask(project, otherAssignee));
        taskRepository.saveAndFlush(newTask(otherProject, assignee));

        List<Task> result = taskRepository.findByProject_IdAndAssignee_Id(project.getId(), assignee.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindTasksByProjectAndStatus() {
        Task done = newTask(project, assignee);
        done.setStatus(TaskStatus.DONE);
        taskRepository.saveAndFlush(done);

        taskRepository.saveAndFlush(newTask(project, assignee));

        Task otherDone = newTask(otherProject, assignee);
        otherDone.setStatus(TaskStatus.DONE);
        taskRepository.saveAndFlush(otherDone);

        List<Task> result = taskRepository.findByProject_IdAndStatus(project.getId(), TaskStatus.DONE);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindTasksByProjectAndIssueType() {
        Task bug = newTask(project, assignee);
        bug.setIssueType(TaskIssueType.BUG);
        taskRepository.saveAndFlush(bug);

        taskRepository.saveAndFlush(newTask(project, assignee));

        Task otherBug = newTask(otherProject, assignee);
        otherBug.setIssueType(TaskIssueType.BUG);
        taskRepository.saveAndFlush(otherBug);

        List<Task> result = taskRepository.findByProject_IdAndIssueType(project.getId(), TaskIssueType.BUG);

        assertThat(result).hasSize(1);
    }
}
