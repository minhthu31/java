package vn.edu.cnpm.projectsupport.reporting.service;

import vn.edu.cnpm.projectsupport.reporting.dto.ProjectProgressResponse;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportFilterRequest;
import vn.edu.cnpm.projectsupport.reporting.dto.ReportSummaryResponse;

public interface ProgressReportService {
    ReportSummaryResponse getSummary(Long projectId, ReportFilterRequest filter);
    ProjectProgressResponse getProgress(Long projectId, ReportFilterRequest filter);
}
