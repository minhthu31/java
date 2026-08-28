package vn.edu.cnpm.projectsupport.integration.jira;

import java.io.IOException;
import java.util.Map;

public interface JiraHttpTransport {
    JiraHttpResponse get(String url, Map<String, String> headers, java.time.Duration timeout)
            throws IOException, InterruptedException;
}
