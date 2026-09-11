package vn.edu.cnpm.projectsupport.integration.github.repository;

public interface ReportCommitActivityProjection {

    Long getActivityId();

    Long getUserId();

    Long getTaskId();
}