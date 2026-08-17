package vn.edu.cnpm.projectsupport.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {
    
    boolean existsByGroupIdAndUserIdAndRole(String groupId, String userId, GroupRole role);

    boolean existsByGroupIdAndUserId(String groupId, String userId);

    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);
}
