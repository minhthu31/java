package vn.edu.cnpm.projectsupport.sprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;
import vn.edu.cnpm.projectsupport.sprint.repository.SprintRepository;

@DataJpaTest
@ActiveProfiles("test")
class SprintRepositoryTests {

    @Autowired SprintRepository sprintRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpProject() {
        jdbcTemplate.update("INSERT INTO student_groups (id, code, name) VALUES (9201, 'TEST-SPRINT', 'Sprint Test Group')");
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (9301, 9201, 'Sprint Test Project')");
    }

    @Test
    void savesAndFindsSprintByProject() {
        Sprint sprint = sprintRepository.saveAndFlush(new Sprint(9301L, "Sprint 1", "FUTURE"));

        assertThat(sprintRepository.findByProjectId(9301L)).containsExactly(sprint);
    }

    @Test
    void projectIdNameAndStateAreRequired() {
        assertThatThrownBy(() -> sprintRepository.saveAndFlush(new Sprint(null, "Sprint 1", "FUTURE")))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> sprintRepository.saveAndFlush(new Sprint(9301L, null, "FUTURE")))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> sprintRepository.saveAndFlush(new Sprint(9301L, "Sprint 1", null)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void jiraSprintIdIsUniqueWithinProject() {
        sprintRepository.saveAndFlush(sprint(1001L));
        assertThatThrownBy(() -> sprintRepository.saveAndFlush(sprint(1001L)))
                .isInstanceOf(Exception.class);
    }

    private Sprint sprint(Long jiraId) {
        Sprint sprint = new Sprint(9301L, "Sprint 1", "FUTURE");
        sprint.setJiraSprintId(jiraId);
        return sprint;
    }
}
