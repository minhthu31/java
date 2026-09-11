package vn.edu.cnpm.projectsupport.reporting;

import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportEnvelope<ReportSummaryResponse>> getProjectReportSummary(
            @PathVariable("projectId") Long projectId,
            @Valid @ModelAttribute ReportFilterRequest filter,
            @AuthenticationPrincipal User currentUser) {

        ReportSummaryResponse summary = reportService.getSummaryReport(projectId, filter, currentUser);
        return ResponseEntity.ok(new ReportEnvelope<>(summary, summary.asOf()));
    }

    public record ReportEnvelope<T>(T data, Instant timestamp) {}
}