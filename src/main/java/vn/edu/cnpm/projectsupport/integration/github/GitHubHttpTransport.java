package vn.edu.cnpm.projectsupport.integration.github;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

public interface GitHubHttpTransport {

    GitHubHttpResponse get(
            String url,
            Map<String, String> headers,
            Duration timeout) throws IOException, InterruptedException;
}
