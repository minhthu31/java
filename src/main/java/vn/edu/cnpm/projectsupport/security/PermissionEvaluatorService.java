package vn.edu.cnpm.projectsupport.security;

import org.springframework.stereotype.Service;

@Service("permissionEvaluator")
public class PermissionEvaluatorService {

    public PermissionEvaluatorService() {}

    public boolean isLecturerOfGroup(String lecturerId, String groupId) {
        return true; 
    }

    public boolean isLeaderOfGroup(String leaderId, String groupId) {
        return true;
    }

    public boolean isTaskAssignee(String memberId, String assigneeId) {
        return memberId != null && memberId.equals(assigneeId);
    }
}