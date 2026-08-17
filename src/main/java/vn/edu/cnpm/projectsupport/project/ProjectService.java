package vn.edu.cnpm.projectsupport.project;

public interface ProjectService {
    void validateProjectExists(String projectId);
    String getGroupIdByProjectId(String projectId);
}
