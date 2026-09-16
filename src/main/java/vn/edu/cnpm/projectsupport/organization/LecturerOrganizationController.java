package vn.edu.cnpm.projectsupport.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.CreatePersonRequest;

@RestController
@RequestMapping("/api/v1/lecturer/groups")
@PreAuthorize("hasRole('LECTURER')")
public class LecturerOrganizationController {
    private final OrganizationService service;
    public LecturerOrganizationController(OrganizationService service) { this.service = service; }
    @GetMapping public ApiResponse<?> groups() { return ApiResponse.success(service.groupsForLecturer()); }
    @GetMapping("/{groupId}/members") public ApiResponse<?> members(@PathVariable Long groupId) { return ApiResponse.success(service.members(groupId)); }
    @PostMapping("/{groupId}/members") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createMember(@PathVariable Long groupId, @Valid @RequestBody CreatePersonRequest request) { return ApiResponse.success(service.createMember(groupId, request)); }
    @PutMapping("/{groupId}/members/{userId}") public ApiResponse<?> assignMember(@PathVariable Long groupId, @PathVariable Long userId) { service.assignMember(groupId, userId); return ApiResponse.success(null); }
    @DeleteMapping("/{groupId}/members/{userId}") public ApiResponse<?> removeMember(@PathVariable Long groupId, @PathVariable Long userId) { service.removeMember(groupId, userId); return ApiResponse.success(null); }
}
