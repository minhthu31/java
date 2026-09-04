package vn.edu.cnpm.projectsupport.project;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTests {

    @Autowired ProjectRepository projectRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void demoLeaderLecturerAndMemberResolveTheSameProjectContext() {
        Long projectId = jdbcTemplate.queryForObject(
                "SELECT id FROM projects WHERE name = 'CNPM Project Management Tool'",
                Long.class);

        for (String username : new String[] {"leader.test", "lecturer.test", "member.test"}) {
            Long userId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE username = ?", Long.class, username);
            assertThat(projectRepository.findFirstAccessibleProjectId(userId))
                    .contains(projectId);
        }
    }

    @Test
    void adminDoesNotReceiveAStudentProjectContext() {
        Long adminId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'admin.test'", Long.class);

        assertThat(projectRepository.findFirstAccessibleProjectId(adminId)).isEmpty();
    }
}
