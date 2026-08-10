package vn.edu.cnpm.projectsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.jwt.secret=401b63e22363964121a328323a2d20741facd722d56214d1f60087413063f915",
    "app.jwt.expiration-ms=86400000"
})
class ProjectSupportApplicationTests {

    
    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void contextLoads() {
       
    }
}
