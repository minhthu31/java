package vn.edu.cnpm.projectsupport.reporting;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;

@RestController
@Validated
@RequestMapping("/api/v1/projects/{projectId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @PreAuthorize("@projectAuthorization.canViewReports(#projectId)")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(
            @PathVariable("projectId") Long projectId,
            @Valid @ModelAttribute ReportFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProjectSummary(projectId, filter)));
    }
}