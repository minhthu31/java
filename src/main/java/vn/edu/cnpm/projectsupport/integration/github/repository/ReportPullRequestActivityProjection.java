package vn.edu.cnpm.projectsupport.integration.github.repository;

import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;

public interface ReportPullRequestActivityProjection {

    Long getActivityId();

    Long getUserId();

    Long getTaskId();

    GitHubPullRequestState getState();
}