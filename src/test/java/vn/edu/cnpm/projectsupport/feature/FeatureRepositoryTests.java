package vn.edu.cnpm.projectsupport.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.cnpm.projectsupport.feature.domain.Feature;
import vn.edu.cnpm.projectsupport.feature.repository.FeatureRepository;

@DataJpaTest
@ActiveProfiles("test")
class FeatureRepositoryTests {

    @Autowired FeatureRepository featureRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpProject() {
        jdbcTemplate.update("INSERT INTO student_groups (id, code, name) VALUES (9001, 'TEST-FEATURE', 'Feature Test Group')");
        jdbcTemplate.update("INSERT INTO projects (id, group_id, name) VALUES (9101, 9001, 'Feature Test Project')");
    }

    @Test
    void savesAndFindsFeatureByProject() {
        Feature feature = featureRepository.saveAndFlush(new Feature(9101L, "Login"));

        assertThat(featureRepository.findByProjectId(9101L)).containsExactly(feature);
    }

    @Test
    void projectIdAndNameAreRequired() {
        assertThatThrownBy(() -> featureRepository.saveAndFlush(new Feature(null, "Login")))
                .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> featureRepository.saveAndFlush(new Feature(9101L, null)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void jiraEpicKeyIsUniqueWithinProject() {
        featureRepository.saveAndFlush(feature("FEAT-1"));
        assertThatThrownBy(() -> featureRepository.saveAndFlush(feature("FEAT-1")))
                .isInstanceOf(Exception.class);
    }

    private Feature feature(String key) {
        Feature feature = new Feature(9101L, "Login");
        feature.setJiraEpicKey(key);
        return feature;
    }
}
