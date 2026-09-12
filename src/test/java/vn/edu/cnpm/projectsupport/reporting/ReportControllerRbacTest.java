package vn.edu.cnpm.projectsupport.reporting;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerRbacTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReportService reportService;
    @MockitoBean(name = "projectAuthorization") ProjectAuthorizationService projectAuthorization;

    @Test
    void teamMemberCannotViewAnotherProject() throws Exception {
        when(projectAuthorization.canViewReports(1L)).thenReturn(false);

        mockMvc.perform(get("/api/v1/projects/1/reports/summary").with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void equalFromAndToIsRejectedByApi() throws Exception {
        when(projectAuthorization.canViewReports(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/projects/1/reports/summary")
                        .param("from", "2026-09-08T00:00:00Z")
                        .param("to", "2026-09-08T00:00:00Z")
                        .with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowedRoleCanCallReportApi() throws Exception {
        when(projectAuthorization.canViewReports(1L)).thenReturn(true);
        when(reportService.getProjectSummary(eq(1L), any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/projects/1/reports/summary").with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isOk());
    }
}
