package vn.edu.cnpm.projectsupport.integration.jira;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public interface JiraHttpTransport {

    JiraHttpResponse get(
            String url,
            Map<String, String> headers,
            Duration timeout)
            throws IOException, InterruptedException;

    JiraHttpResponse post(
            String url,
            Map<String, String> headers,
            String body,
            Duration timeout)
            throws IOException, InterruptedException;

    JiraHttpResponse put(
            String url,
            Map<String, String> headers,
            String body,
            Duration timeout)
            throws IOException, InterruptedException;
}