package vn.edu.cnpm.projectsupport.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    @Query("SELECT p.groupId FROM Project p WHERE p.id = :projectId")
    Optional<String> findGroupIdById(@Param("projectId") String projectId);
}
