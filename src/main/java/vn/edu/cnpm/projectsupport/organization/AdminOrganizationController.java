package vn.edu.cnpm.projectsupport.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.CreatePersonRequest;
import vn.edu.cnpm.projectsupport.organization.OrganizationDtos.GroupRequest;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizationController {
    private final OrganizationService service;
    public AdminOrganizationController(OrganizationService service) { this.service = service; }

    @GetMapping("/groups") public ApiResponse<?> groups() { return ApiResponse.success(service.groupsForAdmin()); }
    @PostMapping("/groups") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createGroup(@Valid @RequestBody GroupRequest request) { return ApiResponse.success(service.createGroup(request)); }
    @PutMapping("/groups/{groupId}") public ApiResponse<?> updateGroup(@PathVariable Long groupId, @Valid @RequestBody GroupRequest request) { return ApiResponse.success(service.updateGroup(groupId, request)); }
    @GetMapping("/lecturers") public ApiResponse<?> lecturers() { return ApiResponse.success(service.lecturers()); }
    @PostMapping("/lecturers") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createLecturer(@Valid @RequestBody CreatePersonRequest request) { return ApiResponse.success(service.createLecturer(request)); }
    @PutMapping("/groups/{groupId}/lecturers/{lecturerId}") public ApiResponse<?> assignLecturer(@PathVariable Long groupId, @PathVariable Long lecturerId) { service.assignLecturer(groupId, lecturerId); return ApiResponse.success(null); }
    @DeleteMapping("/groups/{groupId}/lecturers/{lecturerId}") public ApiResponse<?> unassignLecturer(@PathVariable Long groupId, @PathVariable Long lecturerId) { service.unassignLecturer(groupId, lecturerId); return ApiResponse.success(null); }
    @GetMapping("/groups/{groupId}/members") public ApiResponse<?> members(@PathVariable Long groupId) { return ApiResponse.success(service.members(groupId)); }
    @PostMapping("/groups/{groupId}/members") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> createMember(@PathVariable Long groupId, @Valid @RequestBody CreatePersonRequest request) { return ApiResponse.success(service.createMember(groupId, request)); }
    @PutMapping("/groups/{groupId}/members/{userId}") public ApiResponse<?> assignMember(@PathVariable Long groupId, @PathVariable Long userId) { service.assignMember(groupId, userId); return ApiResponse.success(null); }
    @DeleteMapping("/groups/{groupId}/members/{userId}") public ApiResponse<?> removeMember(@PathVariable Long groupId, @PathVariable Long userId) { service.removeMember(groupId, userId); return ApiResponse.success(null); }
}
