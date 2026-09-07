package vn.edu.cnpm.projectsupport.integration.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class GitHubClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String GITHUB_API_URL = "https://api.github.com";

    public GitHubClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<GitHubPullRequestDto> getPullRequests(String owner, String repo, String token, int page, int perPage) throws Exception {
        String url = String.format("%s/repos/%s/%s/pulls?state=all&per_page=%d&page=%d",
                GITHUB_API_URL, owner, repo, perPage, page);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("GitHub API Error: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<GitHubPullRequestDto>>() {});
    }

    public List<GitHubCommitDto> getPullRequestCommits(String owner, String repo, int pullNumber, String token) throws Exception {
        String url = String.format("%s/repos/%s/%s/pulls/%d/commits?per_page=100",
                GITHUB_API_URL, owner, repo, pullNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("GitHub API Error (Commits): " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readValue(response.body(), new TypeReference<List<GitHubCommitDto>>() {});
    }
}