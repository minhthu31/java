package vn.edu.cnpm.projectsupport.security;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service("projectAuthorization")
@Transactional(readOnly = true)
public class ProjectAuthorizationService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectAuthorizationService(
            CurrentUserService currentUserService,
            ProjectRepository projectRepository,
            TaskRepository taskRepository) {
        this.currentUserService = currentUserService;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    public boolean isCurrentUser(Long userId) {
        return userId != null
                && currentUserService.findCurrentUser()
                        .map(User::getId)
                        .filter(userId::equals)
                        .isPresent();
    }

    public Long currentUserId() {
        return currentUserService.findCurrentUser()
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Không xác định được người dùng hiện tại"));
    }

    public boolean canViewRequirements(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId) || isLecturer(user, projectId))
                .orElse(false);
    }

    public boolean canManageRequirements(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId))
                .orElse(false);
    }

    public boolean canViewTasks(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId)
                        || isLecturer(user, projectId)
                        || isMember(user, projectId))
                .orElse(false);
    }

    public boolean canViewTask(Long projectId, Long taskId) {
        Optional<User> current = currentUserService.findCurrentUser();
        if (current.isEmpty()) {
            return false;
        }
        User user = current.get();
        if (isLeader(user, projectId) || isLecturer(user, projectId)) {
            return taskBelongsToProject(taskId, projectId);
        }
        return isAssignedMember(user, projectId, taskId);
    }

    public boolean canManageTasks(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId))
                .orElse(false);
    }

    public boolean canUpdateTask(Long projectId, Long taskId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId) || isAssignedMember(user, projectId, taskId))
                .orElse(false);
    }

    public boolean isCurrentUserTeamMember(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> role(user) == RoleCode.TEAM_MEMBER && isMember(user, projectId))
                .orElse(false);
    }

    public boolean isCurrentUserLeader(Long projectId) {
        return currentUserService.findCurrentUser()
                .map(user -> isLeader(user, projectId))
                .orElse(false);
    }

    public boolean canViewSrs(Long projectId) {
        return canViewRequirements(projectId);
    }

    public boolean canGenerateSrs(Long projectId) {
        return canManageRequirements(projectId);
    }

    private boolean isLeader(User user, Long projectId) {
        return role(user) == RoleCode.TEAM_LEADER
                && projectRepository.countActiveLeader(projectId, user.getId()) > 0;
    }

    private boolean isLecturer(User user, Long projectId) {
        return role(user) == RoleCode.LECTURER
                && projectRepository.countAssignedLecturer(projectId, user.getId()) > 0;
    }

    private boolean isMember(User user, Long projectId) {
        return role(user) == RoleCode.TEAM_MEMBER
                && projectRepository.countActiveMember(projectId, user.getId()) > 0;
    }

    private boolean isAssignedMember(User user, Long projectId, Long taskId) {
        if (!isMember(user, projectId)) {
            return false;
        }
        return taskRepository.findById(taskId)
                .filter(task -> projectId.equals(task.getProjectId()))
                .map(Task::getAssigneeUserId)
                .filter(user.getId()::equals)
                .isPresent();
    }

    private boolean taskBelongsToProject(Long taskId, Long projectId) {
        return taskRepository.findById(taskId)
                .map(Task::getProjectId)
                .filter(projectId::equals)
                .isPresent();
    }

    private RoleCode role(User user) {
        return user.getRole().getCode();
    }
}
