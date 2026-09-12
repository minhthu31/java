package vn.edu.cnpm.projectsupport.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;

@SpringBootTest
@ActiveProfiles("test")
class ReportingIntegrationFlowTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long projectId;
    private Long sprintId;

    @BeforeEach
    void setUp() {
        projectId = jdbcTemplate.queryForObject("SELECT id FROM projects WHERE name = 'CNPM Project Management Tool' LIMIT 1", Long.class);
        sprintId = jdbcTemplate.queryForObject("SELECT id FROM sprints WHERE project_id = ? LIMIT 1", Long.class, projectId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM task_pr_links WHERE task_id IN (SELECT id FROM tasks WHERE project_id = ?)", projectId);
        jdbcTemplate.update("DELETE FROM task_commit_links WHERE task_id IN (SELECT id FROM tasks WHERE project_id = ?)", projectId);
    }

    @Test
    @WithMockUser(username = "leader.test")
    void testSummaryReportWithSprintAndGithubLinks() {
        ReportFilterRequest filter = new ReportFilterRequest(sprintId, null, null, null);
        ReportSummaryResponse summary = reportService.getProjectSummary(projectId, filter);

        assertThat(summary).isNotNull();
        assertThat(summary.projectId()).isEqualTo(projectId);
        assertThat(summary.sprintId()).isEqualTo(sprintId);
        assertThat(summary.memberContributions()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "leader.test")
    void testInvalidSprintThrowsResourceNotFound() {
        ReportFilterRequest filter = new ReportFilterRequest(99999999L, null, null, null);
        assertThatThrownBy(() -> reportService.getProjectSummary(projectId, filter))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "leader.test")
    void testInvalidTimeRangeThrowsException() {
        Instant point = Instant.parse("2026-09-08T00:00:00Z");
        ReportFilterRequest filter = new ReportFilterRequest(null, null, point, point);
        assertThatThrownBy(() -> reportService.getProjectSummary(projectId, filter))
                .isInstanceOf(IllegalArgumentException.class);
    }
}