package vn.edu.cnpm.projectsupport.security;

import org.springframework.stereotype.Service;

@Service
public class PermissionEvaluatorService {
    public boolean isLecturerAssignedToProject(String lecturerUsername, Long projectId) {
        return "LECTURER_01".equals(lecturerUsername) && Long.valueOf(101L).equals(projectId);
    }
    public boolean isLeaderOfGroup(String leaderUsername, Long groupId) {
        return "LEADER_01".equals(leaderUsername) && Long.valueOf(50L).equals(groupId);
    }
    public boolean isTaskAssignee(String currentUsername, String assigneeUsername) {
        return currentUsername != null && currentUsername.equals(assigneeUsername);
    }
    public boolean canAdminModifyAcademicResource(String role) {
        return false;
    }
}
