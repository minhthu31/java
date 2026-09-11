package vn.edu.cnpm.projectsupport.reporting;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ProjectProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.service.ProgressReportService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/reports")
public class ReportingController {
    private final ProgressReportService service;

    public ReportingController(ProgressReportService service) {
        this.service = service;
    }

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<ProjectProgressResponse>> progress(
            @PathVariable Long projectId,
            @Valid @ModelAttribute ReportFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(service.getProgress(projectId, filter)));
    }
}
