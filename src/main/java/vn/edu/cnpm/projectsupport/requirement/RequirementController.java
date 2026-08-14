package vn.edu.cnpm.projectsupport.requirement;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/requirements")
public class RequirementController {

    // Lấy danh sách requirement theo bộ lọc
    @GetMapping
    public ResponseEntity<ApiResponse<List<RequirementResponse>>> getRequirements(
            @PathVariable Long projectId,
            @ModelAttribute RequirementFilterRequest filter) {
        // Controller chỉ trả cấu trúc envelope, không chứa logic nghiệp vụ
        List<RequirementResponse> responses = Collections.emptyList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Tạo mới requirement
    @PostMapping
    public ResponseEntity<ApiResponse<RequirementResponse>> createRequirement(
            @PathVariable Long projectId,
            @Valid @RequestBody RequirementCreateRequest request) {
        RequirementResponse response = new RequirementResponse();
        response.setProjectId(projectId);
        response.setTitle(request.getTitle());
        response.setActor(request.getActor());
        response.setPriority(request.getPriority());
        response.setStatus(RequirementStatus.DRAFT);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // Lấy chi tiết requirement theo ID
    @GetMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<RequirementResponse>> getRequirementById(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        RequirementResponse response = new RequirementResponse();
        response.setId(requirementId);
        response.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //Cập nhật nội dung requirement
    @PutMapping("/{requirementId}")
    public ResponseEntity<ApiResponse<RequirementResponse>> updateRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementUpdateRequest request) {
        RequirementResponse response = new RequirementResponse();
        response.setId(requirementId);
        response.setProjectId(projectId);
        response.setTitle(request.getTitle());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //  Cập nhật trạng thái requirement
    @PatchMapping("/{requirementId}/status")
    public ResponseEntity<ApiResponse<RequirementResponse>> updateRequirementStatus(
            @PathVariable Long projectId,
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementStatusUpdateRequest request) {
        RequirementResponse response = new RequirementResponse();
        response.setId(requirementId);
        response.setProjectId(projectId);
        response.setStatus(request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Xóa requirement
    @DeleteMapping("/{requirementId}")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        return ResponseEntity.noContent().build();
    }
}
