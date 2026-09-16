package vn.edu.cnpm.projectsupport.autotest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRunStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class AutomationIncidentServiceTest {
    @Mock private TaskRepository taskRepository;
    private AutomationIncidentService service;

    @BeforeEach
    void setUp() {
        service = new AutomationIncidentService(taskRepository);
    }

    @Test
    void failedCheckCreatesAnAutoTestTaskWithTraceability() {
        when(taskRepository.findByIdempotencyKey("auto-check-501")).thenReturn(Optional.empty());

        service.recordCheckResult(7L, 501L, "backend-tests", "abc123", "https://github/check/501",
                GitHubCheckRunStatus.FAILURE);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task task = taskCaptor.getValue();
        assertThat(task.getProjectId()).isEqualTo(7L);
        assertThat(task.getClassification()).isEqualTo(TaskClassification.AUTO_TEST);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TO_DO);
        assertThat(task.getIdempotencyKey()).isEqualTo("auto-check-501");
        assertThat(task.getDescription()).contains("abc123", "https://github/check/501");
    }

    @Test
    void successfulCheckDoesNotCreateTask() {
        service.recordCheckResult(7L, 502L, "backend-tests", "def456", "https://github/check/502",
                GitHubCheckRunStatus.SUCCESS);
        verify(taskRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repeatedFailureIsIdempotent() {
        when(taskRepository.findByIdempotencyKey("auto-check-503")).thenReturn(Optional.of(org.mockito.Mockito.mock(Task.class)));
        service.recordCheckResult(7L, 503L, "backend-tests", "ghi789", "https://github/check/503",
                GitHubCheckRunStatus.CANCELLED);
        verify(taskRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
