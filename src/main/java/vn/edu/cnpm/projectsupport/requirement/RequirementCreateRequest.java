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

    private Priority priority;
    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String exceptionFlow;
    private String postcondition;
    private RequirementStatus status;

    public RequirementCreateRequest() {}

    // Constructor 4 tham số
    public RequirementCreateRequest(String title, String description, String actor, Priority priority) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority;
    }

    // Constructor 5 tham số (Fix lỗi dòng 139)
    public RequirementCreateRequest(String title, String description, String actor, Priority priority, RequirementStatus status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority;
        this.status = status;
    }

    // Constructor 6 tham số
    public RequirementCreateRequest(String title, String description, String actor, Priority priority, String precondition, RequirementStatus status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority;
        this.precondition = precondition;
        this.status = status;
    }

    // Constructor đầy đủ
    public RequirementCreateRequest(String title, String description, String actor, Priority priority,
                                    String precondition, String mainFlow, String alternativeFlow,
                                    String exceptionFlow, String postcondition, RequirementStatus status) {
        this.title = title;
        this.description = description;
        this.actor = actor;
        this.priority = priority;
        this.precondition = precondition;
        this.mainFlow = mainFlow;
        this.alternativeFlow = alternativeFlow;
        this.exceptionFlow = exceptionFlow;
        this.postcondition = postcondition;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private String description;
        private String actor;
        private Priority priority;
        private String precondition;
        private String mainFlow;
        private String alternativeFlow;
        private String exceptionFlow;
        private String postcondition;
        private RequirementStatus status;

        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder actor(String actor) { this.actor = actor; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder priority(String priority) {
            if (priority != null) {
                try { this.priority = Priority.valueOf(priority); } catch (Exception ignored) {}
            }
            return this;
        }
        public Builder precondition(String precondition) { this.precondition = precondition; return this; }
        public Builder mainFlow(String mainFlow) { this.mainFlow = mainFlow; return this; }
        public Builder alternativeFlow(String alternativeFlow) { this.alternativeFlow = alternativeFlow; return this; }
        public Builder exceptionFlow(String exceptionFlow) { this.exceptionFlow = exceptionFlow; return this; }
        public Builder postcondition(String postcondition) { this.postcondition = postcondition; return this; }
        public Builder status(RequirementStatus status) { this.status = status; return this; }
        public Builder status(String status) {
            if (status != null) {
                try { this.status = RequirementStatus.valueOf(status); } catch (Exception ignored) {}
            }
            return this;
        }

        public RequirementCreateRequest build() {
            return new RequirementCreateRequest(title, description, actor, priority, precondition, mainFlow, alternativeFlow, exceptionFlow, postcondition, status);
        }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setPriority(String priority) {
        if (priority != null) {
            try { this.priority = Priority.valueOf(priority); } catch (Exception ignored) {}
        }
    }

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

    public RequirementStatus getStatus() { return status; }
    public void setStatus(RequirementStatus status) { this.status = status; }
    public void setStatus(String status) {
        if (status != null) {
            try { this.status = RequirementStatus.valueOf(status); } catch (Exception ignored) {}
        }
    }
}
