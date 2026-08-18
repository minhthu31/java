package vn.edu.cnpm.projectsupport.group;

import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

    @Override
    public boolean isLeader(String groupId, String userId) {
        // TODO: Cập nhật logic kiểm tra Leader thực tế từ Repository
        return true;
    }

    @Override
    public boolean isLecturer(String groupId, String userId) {
        // TODO: Cập nhật logic kiểm tra Giảng viên thực tế từ Repository
        return false;
    }

    @Override
    public boolean isMember(String groupId, String userId) {
        // TODO: Cập nhật logic kiểm tra Thành viên thực tế từ Repository
        return true;
    }

    @Override
    public boolean hasAccess(String groupId, String userId) {
        // TODO: Cập nhật logic kiểm tra quyền truy cập thực tế
        return isLeader(groupId, userId) || isLecturer(groupId, userId) || isMember(groupId, userId);
    }
}