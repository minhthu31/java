package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestCommit;

public interface GitHubPullRequestCommitRepository extends JpaRepository<GitHubPullRequestCommit, Long> {

    List<GitHubPullRequestCommit> findByPullRequestIdOrderByCommitOrderAsc(Long pullRequestId);
}
