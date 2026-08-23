package vn.edu.cnpm.projectsupport.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.project.domain.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

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
