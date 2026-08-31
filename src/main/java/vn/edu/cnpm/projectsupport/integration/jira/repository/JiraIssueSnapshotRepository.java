package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssueSnapshot;

public interface JiraIssueSnapshotRepository extends JpaRepository<JiraIssueSnapshot, Long> {
    Optional<JiraIssueSnapshot> findByProjectIdAndJiraIssueId(Long projectId, String jiraIssueId);
    Optional<JiraIssueSnapshot> findByProjectIdAndJiraIssueKey(Long projectId, String jiraIssueKey);
}
