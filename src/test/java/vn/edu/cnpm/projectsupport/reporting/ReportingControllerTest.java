package vn.edu.cnpm.projectsupport.reporting;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.edu.cnpm.projectsupport.reporting.dto.ProjectProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.service.ProgressReportService;

@ExtendWith(MockitoExtension.class)
class ReportingControllerTest {

    @Mock
    private ProgressReportService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReportingController(service))
                .build();
    }

    @Test
    void progressEndpointBindsSprintAndMemberFilters() throws Exception {
        var response = new ProjectProgressResponse(
                1L,
                20L,
                7L,
                3L,
                30L,
                18L,
                5L,
                60.0,
                List.of(),
                Instant.now());

        when(service.getProgress(
                eq(1L),
                org.mockito.ArgumentMatchers.any(ReportFilterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/1/reports/progress")
                        .param("sprintId", "20")
                        .param("memberId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.totalRequirements").value(20))
                .andExpect(jsonPath("$.data.totalFeatures").value(7))
                .andExpect(jsonPath("$.data.totalSprints").value(3))
                .andExpect(jsonPath("$.data.totalTasks").value(30))
                .andExpect(jsonPath("$.data.completedTasks").value(18))
                .andExpect(jsonPath("$.data.overdueTasks").value(5))
                .andExpect(jsonPath("$.data.progressPercent").value(60.0));

        ArgumentCaptor<ReportFilterRequest> captor =
                ArgumentCaptor.forClass(ReportFilterRequest.class);

        verify(service).getProgress(eq(1L), captor.capture());

        org.assertj.core.api.Assertions.assertThat(captor.getValue().sprintId())
                .isEqualTo(20L);

        org.assertj.core.api.Assertions.assertThat(captor.getValue().memberId())
                .isEqualTo(7L);
    }
}