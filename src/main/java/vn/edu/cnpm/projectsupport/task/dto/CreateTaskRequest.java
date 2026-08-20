package vn.edu.cnpm.projectsupport.task.dto;

import vn.edu.cnpm.projectsupport.task.enums.TaskType;

public class CreateTaskRequest {
    private String title;
    private String description;
    private String acceptanceCriteria;
    private TaskType type;
    private String projectId;
    private String featureId;
    private String assigneeId;

    public CreateTaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getFeatureId() { return featureId; }
    public void setFeatureId(String featureId) { this.featureId = featureId; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
}