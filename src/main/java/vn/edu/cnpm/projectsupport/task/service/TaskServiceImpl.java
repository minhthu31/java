package vn.edu.cnpm.projectsupport.task.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskClassification;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskPriority;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import vn.edu.cnpm.projectsupport.task.dto.CreateTaskRequest;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional
    public Task createTask(Long projectId, Long leaderUserId, CreateTaskRequest request) {
        if (request.getAcceptanceCriteria() == null || request.getAcceptanceCriteria().isBlank()) {
            throw new IllegalArgumentException("Acceptance Criteria không được để trống.");
        }

       
        TaskIssueType issueType = request.getIssueType() != null ? request.getIssueType() : TaskIssueType.TASK;
        TaskPriority priority = request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM;

        Task task = new Task(projectId, request.getTitle(), request.getAcceptanceCriteria(), issueType, priority);

        task.setDescription(request.getDescription());
        task.setRequirementId(request.getRequirementId());
        task.setFeatureId(request.getFeatureId());
        task.setSprintId(request.getSprintId());
        task.setAssigneeUserId(request.getAssigneeUserId());
        task.setDeadline(request.getDeadline());

        // Phân loại Task
        TaskClassification classification = request.getClassification() != null
                ? request.getClassification()
                : autoClassifyTask(request.getAcceptanceCriteria());

        task.setClassification(classification);
        task.setStatus(TaskStatus.TO_DO);

        log.info("[TASK_CLASSIFICATION] Task '{}' (Project ID: {}) classified as: {}",
                request.getTitle(), projectId, classification);

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTaskStatusByMember(Long memberUserId, Long taskId, TaskStatus status, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Task với ID: " + taskId));

        if (memberUserId != null && task.getAssigneeUserId() != null) {
            if (!memberUserId.equals(task.getAssigneeUserId())) {
                throw new IllegalStateException("Bạn không có quyền cập nhật Task của người khác.");
            }
        }

        TaskStatus currentStatus = task.getStatus();

        // Quy tắc chuyển trạng thái
        if (status == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Team Member không được quyền tự CANCELLED Task.");
        }
        if (currentStatus == TaskStatus.TO_DO && status == TaskStatus.DONE) {
            throw new IllegalStateException("Team Member không được chuyển trực tiếp từ TO_DO sang DONE.");
        }

        // Kiểm tra bắt buộc truyền lý do khi BLOCKED hoặc CANCELLED
        if ((status == TaskStatus.BLOCKED || status == TaskStatus.CANCELLED) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Cần cung cấp lý do khi chuyển trạng thái sang " + status);
        }

        task.setStatus(status);

        log.info("[ACTIVITY_LOG] Task ID: {} | Member ID: {} | Status changed: {} -> {} | Reason: {}",
                taskId, memberUserId, currentStatus, status, reason != null ? reason : "N/A");

        return taskRepository.save(task);
    }

    private TaskClassification autoClassifyTask(String criteria) {
        if (criteria == null || criteria.isBlank()) return TaskClassification.OTHER;

        String lower = criteria.toLowerCase();

        if (lower.contains("test") || lower.contains("coverage") || lower.contains("kiểm thử")) {
            return TaskClassification.AUTO_TEST;
        }
        if (lower.contains("log") || lower.contains("logging") || lower.contains("trace") || lower.contains("monitor")) {
            return TaskClassification.AUTO_LOG;
        }
        if (lower.contains("feature") || lower.contains("chức năng") || lower.contains("tính năng")) {
            return TaskClassification.NEW_FEATURE;
        }

        return TaskClassification.OTHER;
    }
}