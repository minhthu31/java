package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequirementCreateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(min = 1, max = 255, message = "Tiêu đề phải từ 1 đến 255 ký tự")
    private String title;

    private String description;

    @Size(max = 255, message = "Actor không được vượt quá 255 ký tự")
    private String actor;

    private Priority priority;

    private String precondition;

    private String mainFlow;

    private String alternativeFlow;

    private String exceptionFlow;

    private String postcondition;

    private RequirementStatus status;

    public RequirementCreateRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
    }
}