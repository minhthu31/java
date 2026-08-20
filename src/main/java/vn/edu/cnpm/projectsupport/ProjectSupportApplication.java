package vn.edu.cnpm.projectsupport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class ProjectSupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectSupportApplication.class, args);
    }
}
