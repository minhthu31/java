package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;

public interface JiraIssueRepository extends JpaRepository<JiraIssue, Long> {

    Optional<JiraIssue> findByTaskId(Long taskId);

    Optional<JiraIssue> findByJiraIssueId(String jiraIssueId);

    Optional<JiraIssue> findByJiraIssueKey(String jiraIssueKey);
}
