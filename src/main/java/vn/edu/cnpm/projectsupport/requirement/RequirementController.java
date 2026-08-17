package vn.edu.cnpm.projectsupport.requirement;

import java.util.List;

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

import jakarta.validation.Valid;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/requirements")
@Validated
public class RequirementController {

    private final RequirementService requirementService;

    // Constructor Injection
    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    // 1. Tạo Requirement (201 Created) - Chỉ Leader
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('LEADER', 'TEAM_LEADER')")
    public ApiResponse<RequirementResponse> createRequirement(
            @PathVariable Long projectId,
            @Valid @RequestBody RequirementCreateRequest request) {
        RequirementResponse response = requirementService.createRequirement(projectId, request);
        return ApiResponse.success(response);
    }

    // 2. Lấy danh sách Requirement có lọc (200 OK)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER', 'TEAM_LEADER', 'MEMBER', 'TEAM_MEMBER')")
    public ApiResponse<List<RequirementResponse>> getRequirements(
            @PathVariable Long projectId,
            @ModelAttribute RequirementFilterRequest filterRequest) {
        List<RequirementResponse> responses = requirementService.getRequirements(projectId, filterRequest);
        return ApiResponse.success(responses);
    }

    // 3. Lấy chi tiết Requirement theo ID (200 OK)
    @GetMapping("/{requirementId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'LEADER', 'TEAM_LEADER', 'MEMBER', 'TEAM_MEMBER')")
    public ApiResponse<RequirementResponse> getRequirementById(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        RequirementResponse response = requirementService.getRequirementById(projectId, requirementId);
        return ApiResponse.success(response);
    }

    // 4. Cập nhật Requirement (200 OK) - Chỉ Leader
    @PutMapping("/{requirementId}")
    @PreAuthorize("hasAnyRole('LEADER', 'TEAM_LEADER')")
    public ApiResponse<RequirementResponse> updateRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementUpdateRequest request) {
        RequirementResponse response = requirementService.updateRequirement(projectId, requirementId, request);
        return ApiResponse.success(response);
    }

    // 5. Cập nhật trạng thái Requirement (200 OK) - Chỉ Leader
    @PatchMapping("/{requirementId}/status")
    @PreAuthorize("hasAnyRole('LEADER', 'TEAM_LEADER')")
    public ApiResponse<RequirementResponse> updateRequirementStatus(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementStatusUpdateRequest request) {
        RequirementResponse response = requirementService.updateStatus(projectId, requirementId, request);
        return ApiResponse.success(response);
    }

    // 6. Xóa Requirement (204 No Content) - Chỉ Leader
    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('LEADER', 'TEAM_LEADER')")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        requirementService.deleteRequirement(projectId, requirementId);
        return ResponseEntity.noContent().build();
    }
}