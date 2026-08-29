package vn.edu.cnpm.projectsupport.task.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.AssigneeOutsideGroupException;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.InvalidStatusTransitionException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceInUseException;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;
import vn.edu.cnpm.projectsupport.integration.jira.JiraClient;
import vn.edu.cnpm.projectsupport.project.domain.Project;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.requirement.RequirementRepository;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;
import vn.edu.cnpm.projectsupport.task.domain.*;
import vn.edu.cnpm.projectsupport.task.dto.*;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;
import vn.edu.cnpm.projectsupport.task.repository.TaskSpecification;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "title", "priority", "status", "deadline", "createdAt", "updatedAt");

    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final FeatureRepository featureRepository;
    private final SprintRepository sprintRepository;
    private final UserRepository userRepository;
    private final ProjectAuthorizationService projectAuthorization;
    private final JiraClient jiraClient;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            ActivityLogRepository activityLogRepository,
            ProjectRepository projectRepository,
            RequirementRepository requirementRepository,
            FeatureRepository featureRepository,
            SprintRepository sprintRepository,
            UserRepository userRepository,
            ProjectAuthorizationService projectAuthorization,
            JiraClient jiraClient) {
        this.taskRepository = taskRepository;
        this.activityLogRepository = activityLogRepository;
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.featureRepository = featureRepository;
        this.sprintRepository = sprintRepository;
        this.userRepository = userRepository;
        this.projectAuthorization = projectAuthorization;
        this.jiraClient = jiraClient;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewTasks(#projectId)")
    public PageResponse<TaskResponse> getTasks(Long projectId, TaskFilterRequest filter) {
        requireProject(projectId);
        TaskFilterRequest safeFilter = filter == null ? new TaskFilterRequest() : filter;
        Long forcedAssigneeId = projectAuthorization.isCurrentUserTeamMember(projectId)
                ? projectAuthorization.currentUserId()
                : null;
        Page<TaskResponse> page = taskRepository
                .findAll(
                        TaskSpecification.matches(projectId, safeFilter, forcedAssigneeId),
                        pageRequest(safeFilter))
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        return createTaskInternal(projectId, projectAuthorization.currentUserId(), request, null);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public TaskResponse createTask(
            Long projectId,
            CreateTaskRequest request,
            String idempotencyKey) {
        return createTaskInternal(
                projectId,
                projectAuthorization.currentUserId(),
                request,
                idempotencyKey);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId) and @projectAuthorization.isCurrentUser(#leaderUserId)")
    public TaskResponse createTask(Long projectId, Long leaderUserId, CreateTaskRequest request) {
        return createTaskInternal(projectId, leaderUserId, request, null);
    }

    private TaskResponse createTaskInternal(
            Long projectId,
            Long leaderUserId,
            CreateTaskRequest request,
            String idempotencyKey) {
        Project project = requireProject(projectId);
        if (leaderUserId == null || projectRepository.countActiveLeader(projectId, leaderUserId) == 0) {
            throw new ForbiddenGroupScopeException("Chỉ Team Leader của group sở hữu Project được tạo Task");
        }

        String normalizedIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? null
                : idempotencyKey.trim();
        if (normalizedIdempotencyKey != null) {
            Optional<Task> existing = taskRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                if (!projectId.equals(existing.get().getProjectId())) {
                    throw new ResourceInUseException("Idempotency-Key đã được sử dụng ở Project khác");
                }
                return toResponse(existing.get());
            }
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
        task.setIdempotencyKey(normalizedIdempotencyKey);

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
        return toResponse(savedTask);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public TaskResponse updateTask(Long projectId, Long taskId, UpdateTaskRequest request) {
        Project project = requireProject(projectId);
        Task task = requireTask(projectId, taskId);
        if (task.getStatus() == TaskStatus.DONE || task.getStatus() == TaskStatus.CANCELLED) {
            throw new ResourceInUseException("Không thể sửa nội dung Task đã kết thúc");
        }
        if (request.getIssueType() == TaskIssueType.SUBTASK) {
            throw new IllegalArgumentException("Sprint 2 chưa hỗ trợ SUBTASK khi chưa có parentTaskId");
        }
        validateReferences(
                projectId,
                request.getRequirementId(),
                request.getFeatureId(),
                request.getSprintId());
        validateAssignee(projectId, request.getAssigneeUserId());

        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setAcceptanceCriteria(request.getAcceptanceCriteria().trim());
        task.setIssueType(request.getIssueType());
        task.setPriority(request.getPriority());
        task.setRequirementId(request.getRequirementId());
        task.setFeatureId(request.getFeatureId());
        task.setSprintId(request.getSprintId());
        task.setAssigneeUserId(request.getAssigneeUserId());
        task.setDeadline(request.getDeadline());
        task.setClassification(request.getClassification() == null
                ? autoClassifyTask(
                        request.getTitle(),
                        request.getDescription(),
                        request.getAcceptanceCriteria(),
                        request.getFeatureId(),
                        request.getRequirementId())
                : request.getClassification());

        Task saved = taskRepository.save(task);
        activityLogRepository.save(ActivityLog.taskUpdated(
                project.getGroupId(), taskId, projectAuthorization.currentUserId(), saved.getTitle()));
        return toResponse(saved);
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
    @PreAuthorize("@projectAuthorization.canUpdateTask(#projectId, #taskId)")
    public TaskResponse updateTaskStatusByMember(Long projectId, Long taskId, TaskStatusUpdateRequest request) {
        Long actorUserId = projectAuthorization.currentUserId();
        return updateTaskStatusInternal(projectId, actorUserId, taskId, request, false);
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

        validateTransition(currentStatus, targetStatus, leader);

        if ((targetStatus == TaskStatus.BLOCKED || targetStatus == TaskStatus.CANCELLED)
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new IllegalArgumentException("Cần cung cấp lý do khi chuyển trạng thái sang " + targetStatus);
        }

        task.setStatus(targetStatus);

        // CNPM-82: Cập nhật syncStatus nếu có Jira issue liên kết
        syncStatusWithJira(task);

        Task updatedTask = taskRepository.save(task);

        activityLogRepository.save(ActivityLog.taskStatusChanged(
                project.getGroupId(), taskId, actorUserId,
                currentStatus.name(), targetStatus.name(), request.getReason()));

        return toResponse(updatedTask);
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public TaskResponse updateTaskAssignee(
            Long projectId,
            Long taskId,
            TaskAssigneeUpdateRequest request) {
        Project project = requireProject(projectId);
        Task task = requireTask(projectId, taskId);
        Long newAssigneeUserId = request.getAssigneeUserId();
        validateAssignee(projectId, newAssigneeUserId);

        Long oldAssigneeUserId = task.getAssigneeUserId();
        task.setAssigneeUserId(newAssigneeUserId);

        // CNPM-82: Cập nhật syncStatus nếu có Jira issue liên kết
        syncAssigneeWithJira(task);

        Task saved = taskRepository.save(task);
        activityLogRepository.save(ActivityLog.taskAssigneeChanged(
                project.getGroupId(),
                taskId,
                projectAuthorization.currentUserId(),
                oldAssigneeUserId,
                newAssigneeUserId));
        return toResponse(saved);
    }

    private void syncStatusWithJira(Task task) {
        if (task.getId() == null || jiraClient == null) return;
        taskRepository.findJiraIssueKeyByTaskId(task.getId()).ifPresent(key -> {
            try {
                task.setSyncStatus(SyncStatus.SYNCED);
            } catch (Exception e) {
                task.setSyncStatus(SyncStatus.SYNC_FAILED);
            }
        });
    }

    private void syncAssigneeWithJira(Task task) {
        if (task.getId() == null || jiraClient == null) return;
        taskRepository.findJiraIssueKeyByTaskId(task.getId()).ifPresent(key -> {
            try {
                task.setSyncStatus(SyncStatus.SYNCED);
            } catch (Exception e) {
                task.setSyncStatus(SyncStatus.SYNC_FAILED);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@projectAuthorization.canViewTask(#projectId, #taskId)")
    public TaskResponse getTaskById(Long projectId, Long taskId) {
        return toResponse(requireTask(projectId, taskId));
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
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("@projectAuthorization.canManageTasks(#projectId)")
    public void deleteTask(Long projectId, Long taskId) {
        Project project = requireProject(projectId);
        Task task = requireTask(projectId, taskId);
        if (task.getStatus() != TaskStatus.TO_DO || task.getSyncStatus() != SyncStatus.NOT_SYNCED) {
            throw new ResourceInUseException(
                    "Chỉ được xóa Task TO_DO và chưa đồng bộ; hãy chuyển Task sang CANCELLED");
        }
        boolean externallyReferenced = taskRepository.countJiraIssuesByTaskId(taskId) > 0
                || taskRepository.countCommitLinksByTaskId(taskId) > 0
                || taskRepository.countPullRequestLinksByTaskId(taskId) > 0;
        boolean hasImportantActivity = activityLogRepository
                .existsByEntityTypeAndEntityIdAndActionNot("TASK", String.valueOf(taskId), "CREATE");
        if (externallyReferenced || hasImportantActivity) {
            throw new ResourceInUseException(
                    "Task đã có Jira issue, commit/PR link hoặc hoạt động quan trọng; hãy chuyển sang CANCELLED");
        }

        activityLogRepository.save(ActivityLog.taskDeleted(
                project.getGroupId(),
                taskId,
                projectAuthorization.currentUserId(),
                task.getStatus().name()));
        taskRepository.delete(task);
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

    private Task requireTask(Long projectId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Task với ID: " + taskId));
        if (!projectId.equals(task.getProjectId())) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy Task " + taskId + " trong Project " + projectId);
        }
        return task;
    }

    private PageRequest pageRequest(TaskFilterRequest filter) {
        int page = filter.getPage() == null ? 0 : Math.max(filter.getPage(), 0);
        int requestedSize = filter.getSize() == null ? 20 : filter.getSize();
        int size = Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE);

        String sortValue = filter.getSort();
        String[] parts = sortValue == null ? new String[0] : sortValue.split(",", 2);
        String property = parts.length > 0 && ALLOWED_SORTS.contains(parts[0])
                ? parts[0]
                : "updatedAt";
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    private void validateReferences(Long projectId, CreateTaskRequest request) {
        validateReferences(
                projectId,
                request.getRequirementId(),
                request.getFeatureId(),
                request.getSprintId());
    }

    private void validateReferences(
            Long projectId,
            Long requirementId,
            Long featureId,
            Long sprintId) {
        if (requirementId != null
                && requirementRepository.findByIdAndProjectId(requirementId, projectId).isEmpty()) {
            throw new ResourceNotFoundException("Requirement không thuộc Project này");
        }
        if (featureId != null
                && featureRepository.findByIdAndProjectId(featureId, projectId).isEmpty()) {
            throw new ResourceNotFoundException("Feature không thuộc Project này");
        }
        if (sprintId != null
                && sprintRepository.findByIdAndProjectId(sprintId, projectId).isEmpty()) {
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

    private TaskResponse toResponse(Task task) {
        TaskResponse response = TaskResponse.fromEntity(task);
        if (task.getAssigneeUserId() != null) {
            Optional<User> assignee = userRepository.findById(task.getAssigneeUserId());
            if (assignee != null) {
                assignee.ifPresent(user -> response.setAssignee(new TaskAssigneeResponse(
                        user.getId(), user.getUsername(), user.getFullName())));
            }
        }
        if (task.getId() != null) {
            Optional<String> jiraIssueKey = taskRepository.findJiraIssueKeyByTaskId(task.getId());
            if (jiraIssueKey != null) {
                response.setJiraIssueKey(jiraIssueKey.orElse(null));
            }
        }
        return response;
    }
}