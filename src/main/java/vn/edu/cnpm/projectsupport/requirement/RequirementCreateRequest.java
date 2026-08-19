package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementCreateRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private String description;

    @Size(max = 255, message = "Actor must not exceed 255 characters")
    private String actor;

    private Priority priority; // Cho phép null khi tạo bản nháp (draft)

    private String precondition;
    private String mainFlow;
    private String alternativeFlow;
    private String exceptionFlow;
    private String postcondition;

    @Pattern(regexp = "^(DRAFT)?$", message = "Status on creation must be DRAFT or empty")
    private String status;
}
