package vn.edu.cnpm.projectsupport.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementCreateRequest {

    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String actor;
    private String description;
    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String exceptionFlow;
    private String postcondition;
    private Priority priority;
    private RequirementStatus status;

    @JsonIgnore
    @AssertTrue(message = "Creation status must be null, DRAFT or OPEN")
    public boolean isValidCreationStatus() {
        return status == null || status == RequirementStatus.DRAFT || status == RequirementStatus.OPEN;
    }
}