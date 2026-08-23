package vn.edu.cnpm.projectsupport.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;
import vn.edu.cnpm.projectsupport.audit.repository.ActivityLogRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityLogRepositoryTest {

    @Autowired
    private ActivityLogRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsTaskActivityInSharedActivityLogsTable() {
        Long leaderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'leader.test'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO student_groups (code, name, leader_user_id, status)
                VALUES ('ACTIVITY-TEST', 'Activity Test Group', ?, 'ACTIVE')
                """, leaderId);
        Long groupId = jdbcTemplate.queryForObject(
                "SELECT id FROM student_groups WHERE code = 'ACTIVITY-TEST'", Long.class);

        ActivityLog saved = repository.saveAndFlush(
                ActivityLog.taskStatusChanged(
                        groupId, 501L, leaderId, "TO_DO", "IN_PROGRESS", "Started"));

        assertNotNull(saved.getId());
        assertEquals("TASK", saved.getEntityType());
        assertEquals("501", saved.getEntityId());
        assertEquals("IN_PROGRESS", saved.getNewValue().get("status"));
    }
}
