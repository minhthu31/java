package vn.edu.cnpm.projectsupport.project;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    public ProjectMemberController(ProjectMemberService projectMemberService) {
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<List<ProjectMemberResponse>> getActiveMembers(
            @PathVariable Long projectId) {
        return ApiResponse.success(projectMemberService.getActiveMembers(projectId));
    }
}
