package vn.edu.cnpm.projectsupport.group;

public interface GroupService {
    boolean isLeader(String groupId, String userId);
    boolean isLecturer(String groupId, String userId);
    boolean isMember(String groupId, String userId);
    boolean hasAccess(String groupId, String userId);
}
