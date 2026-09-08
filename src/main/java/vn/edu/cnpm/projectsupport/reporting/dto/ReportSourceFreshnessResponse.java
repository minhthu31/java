package vn.edu.cnpm.projectsupport.reporting.dto;

import java.time.Instant;

/** Freshness metadata lets clients distinguish zero activity from missing sync data. */
public record ReportSourceFreshnessResponse(
        ReportSource source,
        ReportSourceStatus status,
        Instant lastSyncedAt) {
}
