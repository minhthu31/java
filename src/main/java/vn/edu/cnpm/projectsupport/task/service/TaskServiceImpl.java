package vn.edu.cnpm.projectsupport.task.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.exception.AssigneeOutsideGroupException;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final FeatureRepository featureRepository;
    private final SprintRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthorization;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            ActivityLogRepository activityLogRepository,
            ProjectRepository projectRepository,
            RequirementRepository requirementRepository,
            FeatureRepository featureRepository,
            SprintRepository sprintRepository,
            ProjectAuthorizationService projectAuthorization) {
        this.taskRepository = taskRepository;
        this.activityLogRepository = activityLogRepository;
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.featureRepository = featureRepository;
        this.sprintRepository = sprintRepository;
        this.projectAuthorization = projectAuthorization;
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        return createTaskInternal(projectId, projectAuthorization.currentUserId(), request);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId) and @projectAuthorization.isCurrentUser(#leaderUserId)")
    public TaskResponse createTask(Long projectId, Long leaderUserId, CreateTaskRequest request) {
        return createTaskInternal(projectId, leaderUserId, request);
    }

    private TaskResponse createTaskInternal(Long projectId, Long leaderUserId, CreateTaskRequest request) {
        Project project = requireProject(projectId);
        if (leaderUserId == null || projectRepository.countActiveLeader(projectId, leaderUserId) == 0) {
            throw new ForbiddenGroupScopeException("Chỉ Team Leader của group sở hữu Project được tạo Task");
        }
        validateReferences(projectId, request);
        validateAssignee(projectId, request.getAssigneeUserId());
        if (request.getIssueType() == TaskIssueType.SUBTASK) {
            throw new IllegalArgumentException("Sprint 2 chưa hỗ trợ tạo SUBTASK khi chưa có parentTaskId");
        }

        Task task = new Task(
                projectId,
                request.getTitle(),
                request.getAcceptanceCriteria(),
                request.getIssueType(),
                request.getPriority());

        task.setDescription(request.getDescription());
        task.setRequirementId(request.getRequirementId());
        task.setFeatureId(request.getFeatureId());
        task.setSprintId(request.getSprintId());
        task.setAssigneeUserId(request.getAssigneeUserId());
        task.setDeadline(request.getDeadline());

        TaskClassification classification = request.getClassification() != null
                ? request.getClassification()
                : autoClassifyTask(
                        request.getTitle(),
                        request.getDescription(),
                        request.getAcceptanceCriteria(),
                        request.getFeatureId(),
                        request.getRequirementId());

        task.setClassification(classification);
        task.setStatus(TaskStatus.TO_DO);

        Task savedTask = taskRepository.save(task);
        activityLogRepository.save(ActivityLog.taskCreated(
                project.getGroupId(), savedTask.getId(), leaderUserId));
        return TaskResponse.fromEntity(savedTask);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canUpdateTask(#projectId, #taskId)")
    public TaskResponse updateTaskStatus(
            Long projectId, Long taskId, TaskStatusUpdateRequest request) {
        Long actorUserId = projectAuthorization.currentUserId();
        boolean leader = projectAuthorization.isCurrentUserLeader(projectId);
        return updateTaskStatusInternal(projectId, actorUserId, taskId, request, leader);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canUpdateTask(#projectId, #taskId) and @projectAuthorization.isCurrentUser(#memberUserId)")
    public TaskResponse updateTaskStatusByMember(Long projectId, Long memberUserId, Long taskId, TaskStatusUpdateRequest request) {
        return updateTaskStatusInternal(projectId, memberUserId, taskId, request, false);
    }

    private TaskResponse updateTaskStatusInternal(
            Long projectId,
            Long actorUserId,
            Long taskId,
            TaskStatusUpdateRequest request,
            boolean leader) {
        Project project = requireProject(projectId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Task với ID: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Task " + taskId + " trong Project " + projectId);
        }

        // Bắt buộc Task phải được gán và đúng Assignee
        if (!leader && (actorUserId == null
                || task.getAssigneeUserId() == null
                || !actorUserId.equals(task.getAssigneeUserId()))) {
            throw new ForbiddenGroupScopeException("Bạn không có quyền cập nhật Task này");
        }
        if (!leader && projectRepository.countActiveMember(projectId, actorUserId) == 0) {
            throw new ForbiddenGroupScopeException("Thành viên không còn hoạt động trong group của Project");
        }

        TaskStatus currentStatus = task.getStatus();
        TaskStatus targetStatus = request.getStatus();

        // Kiểm tra Ma trận Trạng thái (Transition Matrix)
        validateTransition(currentStatus, targetStatus, leader);

        // Lý do bắt buộc khi BLOCKED hoặc CANCELLED
        if ((targetStatus == TaskStatus.BLOCKED || targetStatus == TaskStatus.CANCELLED)
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new IllegalArgumentException("Cần cung cấp lý do khi chuyển trạng thái sang " + targetStatus);
        }

        task.setStatus(targetStatus);
        Task updatedTask = taskRepository.save(task);

        activityLogRepository.save(ActivityLog.taskStatusChanged(
                project.getGroupId(), taskId, actorUserId,
                currentStatus.name(), targetStatus.name(), request.getReason()));

        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewTask(#projectId, #taskId)")
    public TaskResponse getTaskById(Long projectId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Task ID: " + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new ResourceNotFoundException("Không tìm thấy Task " + taskId + " trong Project " + projectId);
        }
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewTasks(#projectId)")
    public List<TaskResponse> getTasksByProject(Long projectId) {
        List<Task> tasks = projectAuthorization.isCurrentUserTeamMember(projectId)
                ? taskRepository.findByProjectIdAndAssigneeUserId(
                        projectId, projectAuthorization.currentUserId())
                : taskRepository.findByProjectId(projectId);
        return tasks.stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    private void validateTransition(TaskStatus from, TaskStatus to, boolean isLeader) {
        if (from == TaskStatus.DONE || from == TaskStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Không thể chuyển trạng thái từ trạng thái kết thúc: " + from);
        }
        if (!isLeader && to == TaskStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Team Member không được quyền tự CANCELLED Task");
        }

        boolean valid = switch (from) {
            case TO_DO -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.BLOCKED || to == TaskStatus.CANCELLED;
            case IN_PROGRESS -> to == TaskStatus.TO_DO || to == TaskStatus.IN_REVIEW || to == TaskStatus.BLOCKED || to == TaskStatus.CANCELLED;
            case IN_REVIEW -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.DONE || to == TaskStatus.BLOCKED;
            case BLOCKED -> to == TaskStatus.TO_DO || to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException("Chuyển trạng thái không hợp lệ: " + from + " -> " + to);
        }
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Project với ID: " + projectId));
    }

    private void validateReferences(Long projectId, CreateTaskRequest request) {
        if (request.getRequirementId() != null
                && requirementRepository.findByIdAndProjectId(request.getRequirementId(), projectId).isEmpty()) {
            throw new ResourceNotFoundException("Requirement không thuộc Project này");
        }
        if (request.getFeatureId() != null
                && featureRepository.findByIdAndProjectId(request.getFeatureId(), projectId).isEmpty()) {
            throw new ResourceNotFoundException("Feature không thuộc Project này");
        }
        if (request.getSprintId() != null
                && sprintRepository.findByIdAndProjectId(request.getSprintId(), projectId).isEmpty()) {
            throw new ResourceNotFoundException("Sprint không thuộc Project này");
        }
    }

    private void validateAssignee(Long projectId, Long assigneeUserId) {
        if (assigneeUserId != null && projectRepository.countActiveMember(projectId, assigneeUserId) == 0) {
            throw new AssigneeOutsideGroupException("Assignee phải là thành viên ACTIVE của group sở hữu Project");
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
