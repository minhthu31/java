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

    private String description;
    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String exceptionFlow;
    private String postcondition;

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

    public RequirementCreateRequest(String title, String actor, String description, String precondition,
                                  String mainFlow, String alternativeFlow, String exceptionFlow,
                                  String postcondition, Priority priority, RequirementStatus status) {
        this.title = title;
        this.actor = actor;
        this.description = description;
        this.precondition = precondition;
        this.mainFlow = mainFlow;
        this.alternativeFlow = alternativeFlow;
        this.exceptionFlow = exceptionFlow;
        this.postcondition = postcondition;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
    }

    public String getMainFlow() {
        return mainFlow;
    }

    public void setMainFlow(String mainFlow) {
        this.mainFlow = mainFlow;
    }

    public String getAlternativeFlow() {
        return alternativeFlow;
    }

    public void setAlternativeFlow(String alternativeFlow) {
        this.alternativeFlow = alternativeFlow;
    }

    public String getExceptionFlow() {
        return exceptionFlow;
    }

    public void setExceptionFlow(String exceptionFlow) {
        this.exceptionFlow = exceptionFlow;
    }

    public String getPostcondition() {
        return postcondition;
    }

    public void setPostcondition(String postcondition) {
        this.postcondition = postcondition;
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