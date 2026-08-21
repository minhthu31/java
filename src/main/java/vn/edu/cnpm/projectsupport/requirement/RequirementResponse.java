package vn.edu.cnpm.projectsupport.requirement;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementResponse {

    private String id;
    private String projectId;
    private String title;
    private String description;
    private Priority priority;
    private RequirementStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}