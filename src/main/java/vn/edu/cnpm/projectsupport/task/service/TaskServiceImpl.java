package vn.edu.cnpm.projectsupport.task.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
import vn.edu.cnpm.projectsupport.task.repository.TaskActivityLogRepository;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskActivityLogRepository activityLogRepository;

    public TaskServiceImpl(TaskRepository taskRepository, TaskActivityLogRepository activityLogRepository) {
        this.taskRepository = taskRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    @Transactional
    public TaskResponse createTask(Long projectId, Long leaderUserId, CreateTaskRequest request) {
        Task task = new Task(projectId, request.getTitle(), request.getAcceptanceCriteria(), request.getIssueType(), request.getPriority());
        
        task.setDescription(request.getDescription());
        task.setRequirementId(request.getRequirementId());
        task.setFeatureId(request.getFeatureId());
        task.setSprintId(request.getSprintId());
        task.setAssigneeUserId(request.getAssigneeUserId());
        task.setDeadline(request.getDeadline());

        TaskClassification classification = request.getClassification() != null 
                ? request.getClassification() 
                : autoClassifyTask(request.getTitle(), request.getDescription(), request.getAcceptanceCriteria(), request.getFeatureId(), request.getRequirementId());
        
        task.setClassification(classification);
        task.setStatus(TaskStatus.TO_DO);

        Task savedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatusByMember(Long projectId, Long memberUserId, Long taskId, TaskStatusUpdateRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Task với ID: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task không thuộc Project này.");
        }

        // Bắt buộc Task phải được gán và đúng Assignee
        if (memberUserId == null || task.getAssigneeUserId() == null || !memberUserId.equals(task.getAssigneeUserId())) {
            throw new IllegalStateException("Bạn không có quyền cập nhật Task này.");
        }

        TaskStatus currentStatus = task.getStatus();
        TaskStatus targetStatus = request.getStatus();

        // Kiểm tra Ma trận Trạng thái (Transition Matrix)
        validateTransition(currentStatus, targetStatus, false);

        // Lý do bắt buộc khi BLOCKED hoặc CANCELLED
        if ((targetStatus == TaskStatus.BLOCKED || targetStatus == TaskStatus.CANCELLED) 
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new IllegalArgumentException("Cần cung cấp lý do khi chuyển trạng thái sang " + targetStatus);
        }

        task.setStatus(targetStatus);
        Task updatedTask = taskRepository.save(task);

        // Ghi Activity Log vào Database
        TaskActivityLog activityLog = new TaskActivityLog(taskId, memberUserId, currentStatus, targetStatus, request.getReason());
        activityLogRepository.save(activityLog);

        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long projectId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Task ID: " + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task không thuộc Project này.");
        }
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    private void validateTransition(TaskStatus from, TaskStatus to, boolean isLeader) {
        if (from == TaskStatus.DONE || from == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Không thể chuyển trạng thái từ trạng thái kết thúc: " + from);
        }
        if (!isLeader && to == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Team Member không được quyền tự CANCELLED Task.");
        }

        boolean valid = switch (from) {
            case TO_DO -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.BLOCKED || to == TaskStatus.CANCELLED;
            case IN_PROGRESS -> to == TaskStatus.TO_DO || to == TaskStatus.IN_REVIEW || to == TaskStatus.BLOCKED || to == TaskStatus.CANCELLED;
            case IN_REVIEW -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.DONE || to == TaskStatus.BLOCKED;
            case BLOCKED -> to == TaskStatus.TO_DO || to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Chuyển trạng thái không hợp lệ: " + from + " -> " + to);
        }
    }

    private TaskClassification autoClassifyTask(String title, String desc, String criteria, Long featureId, Long reqId) {
        String combined = ((title != null ? title : "") + " " 
                        + (desc != null ? desc : "") + " " 
                        + (criteria != null ? criteria : "")).toLowerCase();

        if (combined.contains("test") || combined.contains("coverage") || combined.contains("kiểm thử")) {
            return TaskClassification.AUTO_TEST;
        }
        // Sử dụng Regex word boundary để tránh nhận nhầm từ "logic", "login", "dialog", "catalog"
        if (combined.matches(".*\\b(log|logs|logging|trace|monitor)\\b.*")) {
            return TaskClassification.AUTO_LOG;
        }
        if (combined.contains("new feature") || combined.contains("tạo mới tính năng")) {
            return TaskClassification.NEW_FEATURE;
        }
        if (featureId != null || reqId != null || combined.contains("feature") || combined.contains("tính năng")) {
            return TaskClassification.FEATURE_RELATED;
        }

        return TaskClassification.OTHER;
    }
}