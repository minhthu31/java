package vn.edu.cnpm.projectsupport.reporting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.cnpm.projectsupport.reporting.dto.MemberContributionResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportDataStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSource;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceFreshnessResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSourceStatus;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.TaskMetricsResponse;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

@ExtendWith(MockitoExtension.class)
class ReportControllerRbacTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/reports/summary - Trả về báo cáo hợp lệ chuẩn 200 OK")
    void reportSummaryEndpointReturnsOk() throws Exception {
        var statusMap = new EnumMap<TaskStatus, Long>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusMap.put(status, 0L);
        }
        statusMap.put(TaskStatus.DONE, 3L);

        ReportSummaryResponse mockResponse = new ReportSummaryResponse(
                1L,
                null,
                null,
                null,
                null,
                Instant.parse("2026-09-09T02:00:00Z"),
                new TaskMetricsResponse(3, 3, 0, statusMap),
                List.of(new MemberContributionResponse(7L, "member.test", "Test Member", 5, 2, 3)),
                ReportDataStatus.COMPLETE,
                List.of(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null)),
                List.of()
        );

        when(reportService.getSummaryReport(eq(1L), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/projects/1/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(1L))
                .andExpect(jsonPath("$.data.taskMetrics.totalTasks").value(3))
                .andExpect(jsonPath("$.data.taskMetrics.completedTasks").value(3))
                .andExpect(jsonPath("$.data.dataStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.memberContributions[0].username").value("member.test"));
    }
}