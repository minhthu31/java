package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/requirements")
@Validated
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER')")
    public ApiResponse<PageResponse<RequirementResponse>> getRequirements(
            @PathVariable Long projectId,
            @ModelAttribute RequirementFilterRequest filter) {
        return ApiResponse.success(requirementService.getRequirements(projectId, filter));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<RequirementResponse> createRequirement(
            @PathVariable Long projectId,
            @Valid @RequestBody RequirementCreateRequest request) {
        return ApiResponse.success(requirementService.createRequirement(projectId, request));
    }

    @GetMapping("/{requirementId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'TEAM_LEADER')")
    public ApiResponse<RequirementResponse> getRequirementById(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        return ApiResponse.success(requirementService.getRequirementById(projectId, requirementId));
    }

    @PutMapping("/{requirementId}")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<RequirementResponse> updateRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementUpdateRequest request) {
        return ApiResponse.success(requirementService.updateRequirement(projectId, requirementId, request));
    }

    @PatchMapping("/{requirementId}/status")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<RequirementResponse> updateRequirementStatus(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementStatusUpdateRequest request) {
        return ApiResponse.success(requirementService.updateStatus(projectId, requirementId, request));
    }

    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        requirementService.deleteRequirement(projectId, requirementId);
        return ResponseEntity.noContent().build();
    }
}
