package vn.edu.cnpm.projectsupport.integration.github;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class GitHubConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GitHubHttpTransport gitHubHttpTransport() {
        return new JdkGitHubHttpTransport();
    }

    @Bean
    @ConditionalOnMissingBean
    public GitHubRestClient gitHubRestClient(GitHubHttpTransport transport) {
        return new GitHubRestClient(transport, new ObjectMapper());
    }
}