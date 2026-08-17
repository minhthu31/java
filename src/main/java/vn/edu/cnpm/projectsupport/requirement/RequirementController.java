package vn.edu.cnpm.projectsupport.requirement;

import vn.edu.cnpm.projectsupport.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @PostMapping
    public ResponseEntity<RequirementResponse> create(
            @Valid @RequestBody RequirementCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requirementService.create(request, currentUser.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RequirementResponse> update(
            @PathVariable String id,
            @Valid @RequestBody RequirementUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(requirementService.update(id, request, currentUser.getId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RequirementResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody RequirementStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(requirementService.updateStatus(id, request, currentUser.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        requirementService.delete(id, hardDelete, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequirementResponse> getDetail(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(requirementService.getDetail(id, currentUser.getId()));
    }

    @GetMapping
    public ResponseEntity<Page<RequirementResponse>> getList(
            RequirementFilterRequest filter,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ResponseEntity.ok(requirementService.getList(filter, pageable, currentUser.getId()));
    }
}