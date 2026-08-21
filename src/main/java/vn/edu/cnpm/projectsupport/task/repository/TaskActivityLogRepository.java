package vn.edu.cnpm.projectsupport.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.task.domain.TaskActivityLog;

public interface TaskActivityLogRepository extends JpaRepository<TaskActivityLog, Long> {
}