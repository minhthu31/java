package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private String description;
    private Priority priority;
    private RequirementStatus status;

    // Constructor phục vụ cho unit test / khởi tạo nhanh
    public RequirementCreateRequest(String projectId, String title, Priority priority, RequirementStatus status) {
        this.projectId = projectId;
        this.title = title;
        this.priority = priority;
        this.status = status;
    }
}