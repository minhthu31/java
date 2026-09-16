package vn.edu.cnpm.projectsupport.sprint;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.sprint.dto.SprintResponse;
import vn.edu.cnpm.projectsupport.sprint.service.SprintService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/sprints")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @GetMapping
    @PreAuthorize("@projectAuthorization.canViewReports(#projectId)")
    public ApiResponse<List<SprintResponse>> getProjectSprints(
            @PathVariable Long projectId) {
        return ApiResponse.success(sprintService.getProjectSprints(projectId));
    }
}
