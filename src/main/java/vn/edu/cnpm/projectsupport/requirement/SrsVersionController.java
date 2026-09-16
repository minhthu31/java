package vn.edu.cnpm.projectsupport.requirement;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/srs/versions")
public class SrsVersionController {
    private final SrsVersionService service;
    public SrsVersionController(SrsVersionService service){this.service=service;}
    @GetMapping @PreAuthorize("@projectAuthorization.canViewSrs(#projectId)")
    public ApiResponse<?> versions(@PathVariable Long projectId){return ApiResponse.success(service.versions(projectId));}
    @GetMapping("/{version}") @PreAuthorize("@projectAuthorization.canViewSrs(#projectId)")
    public ApiResponse<?> version(@PathVariable Long projectId,@PathVariable String version){return ApiResponse.success(service.version(projectId,version));}
    @PostMapping @PreAuthorize("@projectAuthorization.canGenerateSrs(#projectId)")
    public ApiResponse<?> generate(@PathVariable Long projectId){return ApiResponse.success(service.generate(projectId));}
}
