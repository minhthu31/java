package vn.edu.cnpm.projectsupport.reporting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.cnpm.projectsupport.reporting.dto.*;
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
    @DisplayName("LECTURER / LEADER có quyền truy cập trả về HTTP 200 OK với đầy đủ dữ liệu")
    void authorizedUser_Returns200WithFullData() throws Exception {
        var statusMap = new EnumMap<TaskStatus, Long>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) statusMap.put(status, 0L);
        statusMap.put(TaskStatus.DONE, 3L);

        ReportSummaryResponse mockResponse = new ReportSummaryResponse(
                1L, null, null, null, null,
                Instant.parse("2026-09-09T02:00:00Z"),
                new TaskMetricsResponse(3, 3, 0, statusMap),
                List.of(new MemberContributionResponse(7L, "member.test", "Test Member", 5, 2, 3)),
                ReportDataStatus.COMPLETE,
                List.of(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null)),
                List.of()
        );

        when(reportService.getSummaryReport(eq(1L), any(), eq("lecturer.test"))).thenReturn(mockResponse);

        Principal principal = () -> "lecturer.test";

        mockMvc.perform(get("/api/v1/projects/1/reports/summary").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(1L))
                .andExpect(jsonPath("$.data.taskMetrics.totalTasks").value(3))
                .andExpect(jsonPath("$.data.dataStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.data.memberContributions[0].username").value("member.test"));
    }

    @Test
    @DisplayName("TEAM_MEMBER xem báo cáo bị ép scope về chính mình")
    void teamMember_ForcedScopeToSelf() throws Exception {
        var statusMap = new EnumMap<TaskStatus, Long>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) statusMap.put(status, 0L);

        ReportSummaryResponse mockResponse = new ReportSummaryResponse(
                1L, null, 7L, null, null,
                Instant.parse("2026-09-09T02:00:00Z"),
                new TaskMetricsResponse(1, 0, 0, statusMap),
                List.of(new MemberContributionResponse(7L, "member.test", "Test Member", 2, 1, 1)),
                ReportDataStatus.COMPLETE,
                List.of(new ReportSourceFreshnessResponse(ReportSource.LOCAL_TASK, ReportSourceStatus.CURRENT, null)),
                List.of()
        );

        when(reportService.getSummaryReport(eq(1L), any(), eq("member.test"))).thenReturn(mockResponse);

        Principal principal = () -> "member.test";

        // Truyền memberId khác nhưng trả về đúng memberId bị ép (7L)
        mockMvc.perform(get("/api/v1/projects/1/reports/summary")
                .param("memberId", "999")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(7L))
                .andExpect(jsonPath("$.data.memberContributions[0].memberId").value(7L));
    }

    @Test
    @DisplayName("User không thuộc Project bị từ chối quyền HTTP 403 Forbidden")
    void forbiddenUser_Returns403() throws Exception {
        when(reportService.getSummaryReport(eq(1L), any(), eq("outsider.user")))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "REPORT_ACCESS_DENIED"));

        Principal principal = () -> "outsider.user";

        mockMvc.perform(get("/api/v1/projects/1/reports/summary").principal(principal))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chưa xác thực (Unauthenticated) bị từ chối HTTP 401 Unauthorized")
    void unauthenticated_Returns401() throws Exception {
        when(reportService.getSummaryReport(eq(1L), any(), eq(null)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/projects/1/reports/summary"))
                .andExpect(status().isUnauthorized());
    }
}