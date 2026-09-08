package vn.edu.cnpm.projectsupport.reporting.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/** Optional filters shared by the project progress and contribution report. */
public record ReportFilterRequest(
        @Positive Long sprintId,
        @Positive Long memberId,
        Instant from,
        Instant to) {

    @AssertTrue(message = "from must be earlier than to")
    public boolean isTimeRangeValid() {
        return from == null || to == null || from.isBefore(to);
    }
}
