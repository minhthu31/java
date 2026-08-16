package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequirementCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 255, message = "Actor must not exceed 255 characters")
    private String actor;

    private Priority priority;

    private RequirementStatus status;

    @AssertTrue(message = "Creation status must be null or DRAFT")
    public boolean isValidCreationStatus() {
        return status == null || status == RequirementStatus.DRAFT;
    }

    public RequirementCreateRequest() {
    }

    public RequirementCreateRequest(String title, String actor, Priority priority, RequirementStatus status) {
        this.title = title;
        this.actor = actor;
        this.priority = priority;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }
}