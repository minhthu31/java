package vn.edu.cnpm.projectsupport.integration.jira;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JdkJiraHttpTransport implements JiraHttpTransport {
    private final HttpClient httpClient;

    public JdkJiraHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public JiraHttpResponse get(String url, Map<String, String> headers, Duration timeout)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = httpClient.send(
                builder.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, String> responseHeaders = response.headers().map().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        entry -> String.join(",", entry.getValue()),
                        (left, right) -> left));
        return new JiraHttpResponse(response.statusCode(), response.body(), responseHeaders);
    }
}
