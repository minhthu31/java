package vn.edu.cnpm.projectsupport.autotest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRunStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

/** Converts failed automated checks into idempotent local remediation tasks. */
@Service
public class AutomationIncidentService {
    private final TaskRepository taskRepository;
    public AutomationIncidentService(TaskRepository taskRepository) { this.taskRepository = taskRepository; }

    @Transactional
    public void recordCheckResult(Long projectId, Long externalCheckId, String checkName,
            String commitSha, String htmlUrl, GitHubCheckRunStatus status) {
        if (status != GitHubCheckRunStatus.FAILURE && status != GitHubCheckRunStatus.CANCELLED) return;
        String key = "auto-check-" + externalCheckId;
        if (taskRepository.findByIdempotencyKey(key).isPresent()) return;
        Task task = new Task(projectId, "Khắc phục CI: " + checkName,
                "Check run phải chạy SUCCESS và bằng chứng được lưu trên GitHub.",
                TaskIssueType.TASK, TaskPriority.HIGH);
        task.setDescription("Tự động tạo từ check run thất bại. Commit: " + commitSha + ". Chi tiết: " + htmlUrl);
        task.setClassification(TaskClassification.AUTO_TEST);
        task.setStatus(TaskStatus.TO_DO);
        task.setIdempotencyKey(key);
        taskRepository.save(task);
    }
}
