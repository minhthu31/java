package vn.edu.cnpm.projectsupport.reporting.dto;

import java.time.Instant;
import java.util.List;

/** Aggregate report contract shared by the reporting backend and dashboard. */
public record ReportSummaryResponse(
        Long projectId,
        Long sprintId,
        Long memberId,
        Instant from,
        Instant to,
        Instant asOf,
        TaskMetricsResponse taskMetrics,
        List<MemberContributionResponse> memberContributions,
        ReportDataStatus dataStatus,
        List<ReportSourceFreshnessResponse> sources,
        List<String> warnings) {
}
