package vn.edu.cnpm.projectsupport.reporting;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ProjectProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.service.ProgressReportService;
import vn.edu.cnpm.projectsupport.reporting.service.CommitQualityService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/reports")
public class ReportingController {
    private final ProgressReportService service;
    private final CommitQualityService commitQualityService;

    public ReportingController(ProgressReportService service, CommitQualityService commitQualityService) {
        this.service = service;
        this.commitQualityService = commitQualityService;
    }

    @GetMapping("/commit-quality")
    @PreAuthorize("@projectAuthorization.canViewReports(#projectId)")
    public ApiResponse<?> commitQuality(@PathVariable Long projectId,
            @RequestParam(required = false) Long memberId) {
        return ApiResponse.success(commitQualityService.report(projectId, memberId));
    }

    @GetMapping("/progress")
    @PreAuthorize("@projectAuthorization.canViewReports(#projectId)")
    public ResponseEntity<ApiResponse<ProjectProgressResponse>> progress(
            @PathVariable Long projectId,
            @Valid @ModelAttribute ReportFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(service.getProgress(projectId, filter)));
    }
}
