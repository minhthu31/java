package vn.edu.cnpm.projectsupport.integration.github.repository;

public interface ReportPullRequestActivityProjection {

    Long getActivityId();

    Long getUserId();

    Long getTaskId();
}