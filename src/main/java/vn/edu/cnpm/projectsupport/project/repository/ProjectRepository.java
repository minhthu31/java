package vn.edu.cnpm.projectsupport.project.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.project.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    interface ActiveMemberProjection {
        Long getId();
        String getUsername();
        String getFullName();
    }

    @Query(value = """
            SELECT DISTINCT p.id
              FROM projects p
              JOIN student_groups g ON g.id = p.group_id
              LEFT JOIN group_members gm
                     ON gm.group_id = g.id
                    AND gm.user_id = :userId
                    AND gm.status = 'ACTIVE'
              LEFT JOIN group_lecturers gl
                     ON gl.group_id = g.id
                    AND gl.lecturer_user_id = :userId
             WHERE p.status = 'ACTIVE'
               AND g.status = 'ACTIVE'
               AND (g.leader_user_id = :userId
                    OR gm.user_id IS NOT NULL
                    OR gl.lecturer_user_id IS NOT NULL)
             ORDER BY p.id
             LIMIT 1
            """, nativeQuery = true)
    Optional<Long> findFirstAccessibleProjectId(@Param("userId") Long userId);

    @Query(value = """
            SELECT u.id AS id,
                   u.username AS username,
                   u.full_name AS fullName
              FROM projects p
              JOIN group_members gm ON gm.group_id = p.group_id
              JOIN users u ON u.id = gm.user_id
             WHERE p.id = :projectId
               AND p.status = 'ACTIVE'
               AND gm.status = 'ACTIVE'
               AND u.status = 'ACTIVE'
             ORDER BY u.full_name, u.id
            """, nativeQuery = true)
    List<ActiveMemberProjection> findActiveMembers(@Param("projectId") Long projectId);

    @Query(value = """
            SELECT COUNT(*)
              FROM projects p
              JOIN student_groups g ON g.id = p.group_id
             WHERE p.id = :projectId
               AND g.leader_user_id = :userId
               AND g.status = 'ACTIVE'
            """, nativeQuery = true)
    long countActiveLeader(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM projects p
              JOIN group_members gm ON gm.group_id = p.group_id
             WHERE p.id = :projectId
               AND gm.user_id = :userId
               AND gm.status = 'ACTIVE'
            """, nativeQuery = true)
    long countActiveMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query(value = """
            SELECT COUNT(*)
              FROM projects p
              JOIN group_lecturers gl ON gl.group_id = p.group_id
             WHERE p.id = :projectId
               AND gl.lecturer_user_id = :userId
            """, nativeQuery = true)
    long countAssignedLecturer(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
