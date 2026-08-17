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
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.project.domain.Feature;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.domain.Sprint;
import vn.edu.cnpm.projectsupport.requirement.domain.Requirement;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

/**
 * Integration tests for {@link TaskRepository}. Persists real related entities
 * (project, requirement, feature, sprint, assignee) through {@link TestEntityManager}
 * so the filtering queries and the Task -> Project mapping are exercised against an
 * actual schema (Hibernate ddl-auto=create-drop from the entity mappings, matching
 * the V1 migration) instead of an always-empty database.
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private Project project;
    private Project otherProject;
    private User assignee;
    private User otherAssignee;
    private Requirement requirement;
    private Feature feature;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        Role role = entityManager.persist(new Role(RoleCode.TEAM_MEMBER, "Team Member"));

        StudentGroup group = new StudentGroup();
        group.setCode("G1");
        group.setName("Group 1");
        entityManager.persist(group);

        project = new Project();
        project.setGroup(group);
        project.setName("Project A");
        project.setStatus("ACTIVE");
        entityManager.persist(project);

        otherProject = new Project();
        otherProject.setGroup(group);
        otherProject.setName("Project B");
        otherProject.setStatus("ACTIVE");
        entityManager.persist(otherProject);

        assignee = entityManager.persist(new User(role, "alice", "alice@x.vn", "hash", "Alice"));
        otherAssignee = entityManager.persist(new User(role, "bob", "bob@x.vn", "hash", "Bob"));

        requirement = new Requirement();
        requirement.setProject(project);
        requirement.setTitle("Login requirement");
        requirement.setStatus("DRAFT");
        entityManager.persist(requirement);

        feature = new Feature();
        feature.setProject(project);
        feature.setName("Authentication");
        entityManager.persist(feature);

        sprint = new Sprint();
        sprint.setProject(project);
        sprint.setName("Sprint 1");
        sprint.setState("ACTIVE");
        entityManager.persist(sprint);

        entityManager.flush();
    }

    private Task newTask(Project taskProject, User taskAssignee, TaskStatus status, TaskIssueType issueType) {
        Task task = new Task();
        task.setProject(taskProject);
        task.setRequirement(requirement);
        task.setFeature(feature);
        task.setSprint(sprint);
        task.setAssignee(taskAssignee);
        task.setTitle("Task title");
        task.setAcceptanceCriteria("Acceptance criteria");
        task.setIssueType(issueType);
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(status);
        return task;
    }

    @Test
    void taskLinksProjectRequirementFeatureSprintAndAssignee() {
        Task saved = taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        entityManager.clear();

        Task reloaded = taskRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getProject().getId()).isEqualTo(project.getId());
        assertThat(reloaded.getRequirement().getId()).isEqualTo(requirement.getId());
        assertThat(reloaded.getFeature().getId()).isEqualTo(feature.getId());
        assertThat(reloaded.getSprint().getId()).isEqualTo(sprint.getId());
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
    void shouldFindTasksByProject() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(otherProject, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByProject_Id(project.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProject().getId()).isEqualTo(project.getId());
    }

    @Test
    void shouldFindTasksByAssignee() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(project, otherAssignee, TaskStatus.TO_DO, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByAssignee_Id(assignee.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignee().getId()).isEqualTo(assignee.getId());
    }

    @Test
    void shouldFindTasksByStatus() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.DONE, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByStatus(TaskStatus.DONE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void shouldFindTasksByIssueType() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.BUG));
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByIssueType(TaskIssueType.BUG);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIssueType()).isEqualTo(TaskIssueType.BUG);
    }

    @Test
    void shouldFindTasksByProjectAndAssignee() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(project, otherAssignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(otherProject, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByProject_IdAndAssignee_Id(project.getId(), assignee.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindTasksByProjectAndStatus() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.DONE, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(otherProject, assignee, TaskStatus.DONE, TaskIssueType.TASK));

        List<Task> result = taskRepository.findByProject_IdAndStatus(project.getId(), TaskStatus.DONE);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldFindTasksByProjectAndIssueType() {
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.BUG));
        taskRepository.saveAndFlush(newTask(project, assignee, TaskStatus.TO_DO, TaskIssueType.TASK));
        taskRepository.saveAndFlush(newTask(otherProject, assignee, TaskStatus.TO_DO, TaskIssueType.BUG));

        List<Task> result = taskRepository.findByProject_IdAndIssueType(project.getId(), TaskIssueType.BUG);

        assertThat(result).hasSize(1);
    }
}
