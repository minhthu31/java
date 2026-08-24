package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JiraProperties.class)
public class JiraConfiguration {
}
