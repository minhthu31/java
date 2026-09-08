package vn.edu.cnpm.projectsupport.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import vn.edu.cnpm.projectsupport.reporting.dto.MemberContributionResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSource;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceFreshnessResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.TaskMetricsResponse;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

class ReportContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsOpenEndedAndIncreasingTimeRanges() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ReportFilterRequest(null, null, null, null))).isEmpty();
            assertThat(validator.validate(new ReportFilterRequest(
                    2L,
                    7L,
                    Instant.parse("2026-09-01T00:00:00Z"),
                    Instant.parse("2026-09-08T00:00:00Z")))).isEmpty();
        }
    }

    @Test
    void rejectsNonPositiveIdsAndInvalidTimeRanges() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new ReportFilterRequest(
                    0L,
                    -1L,
                    Instant.parse("2026-09-08T00:00:00Z"),
                    Instant.parse("2026-09-01T00:00:00Z")));
            assertThat(violations).hasSize(3);
        }
    }

    @Test
    void serializesTheAgreedCamelCaseEnvelopePayload() throws Exception {
        var byStatus = new EnumMap<TaskStatus, Long>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, status == TaskStatus.DONE ? 3L : 0L);
        }
        var response = new ReportSummaryResponse(
                1L,
                4L,
                null,
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-08T00:00:00Z"),
                Instant.parse("2026-09-08T01:00:00Z"),
                new TaskMetricsResponse(3, 3, 0, byStatus),
                List.of(new MemberContributionResponse(7L, "member.test", "Test Member", 5, 2, 3)),
                ReportDataStatus.COMPLETE,
                List.of(new ReportSourceFreshnessResponse(
                        ReportSource.GITHUB,
                        ReportSourceStatus.CURRENT,
                        Instant.parse("2026-09-08T00:30:00Z"))),
                List.of());

        String json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"projectId\":1")
                .contains("\"completedTasks\":3")
                .contains("\"pullRequests\":2")
                .contains("\"dataStatus\":\"COMPLETE\"")
                .doesNotContain("password", "token", "secret");
    }
}
