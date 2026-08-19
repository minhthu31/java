package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequirementCreateRequest {

    @NotBlank(message = "must not be blank")
    @Size(min = 1, max = 255, message = "title must be between 1 and 255 characters")
    private String title;

    private String description;

    @Size(max = 255, message = "actor must not exceed 255 characters")
    private String actor;

    private String priority;
    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String exceptionFlow;
    private String postcondition;
    private String status;

    public RequirementCreateRequest() {}

    public RequirementCreateRequest(String title, String description, String actor, Priority priority) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority != null ? priority.name() : null;
    }

    public RequirementCreateRequest(String title, String description, String actor, Priority priority, RequirementStatus status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority != null ? priority.name() : null;
        this.status = status != null ? status.name() : null;
    }

    public RequirementCreateRequest(String title, String description, String actor, String priority, String status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority;
        this.status = status;
    }

    public RequirementCreateRequest(String title, String description, String actor, Priority priority, String precondition, RequirementStatus status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority != null ? priority.name() : null;
        this.precondition = precondition;
        this.status = status != null ? status.name() : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String actor;
        private String priority;
        private String precondition;
        private String mainFlow;
        private String alternativeFlow;
        private String exceptionFlow;
        private String postcondition;
        private String status;

        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder actor(String actor) { this.actor = actor; return this; }
        public Builder priority(Priority priority) { this.priority = priority != null ? priority.name() : null; return this; }
        public Builder priority(String priority) { this.priority = priority; return this; }
        public Builder precondition(String precondition) { this.precondition = precondition; return this; }
        public Builder mainFlow(String mainFlow) { this.mainFlow = mainFlow; return this; }
        public Builder alternativeFlow(String alternativeFlow) { this.alternativeFlow = alternativeFlow; return this; }
        public Builder exceptionFlow(String exceptionFlow) { this.exceptionFlow = exceptionFlow; return this; }
        public Builder postcondition(String postcondition) { this.postcondition = postcondition; return this; }
        public Builder status(RequirementStatus status) { this.status = status != null ? status.name() : null; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public RequirementCreateRequest build() {
            RequirementCreateRequest req = new RequirementCreateRequest();
            req.setTitle(this.title);
            req.setDescription(this.description);
            req.setActor(this.actor);
            req.setPriority(this.priority);
            req.setPrecondition(this.precondition);
            req.setMainFlow(this.mainFlow);
            req.setAlternativeFlow(this.alternativeFlow);
            req.setExceptionFlow(this.exceptionFlow);
            req.setPostcondition(this.postcondition);
            req.setStatus(this.status);
            return req;
        }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setPriority(Priority priority) { this.priority = priority != null ? priority.name() : null; }

    public String getPrecondition() { return precondition; }
    public void setPrecondition(String precondition) { this.precondition = precondition; }

    public String getMainFlow() { return mainFlow; }
    public void setMainFlow(String mainFlow) { this.mainFlow = mainFlow; }

    public String getAlternativeFlow() { return alternativeFlow; }
    public void setAlternativeFlow(String alternativeFlow) { this.alternativeFlow = alternativeFlow; }

    public String getExceptionFlow() { return exceptionFlow; }
    public void setExceptionFlow(String exceptionFlow) { this.exceptionFlow = exceptionFlow; }

    public String getPostcondition() { return postcondition; }
    public void setPostcondition(String postcondition) { this.postcondition = postcondition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public void setStatus(RequirementStatus status) { this.status = status != null ? status.name() : null; }
}
