package vn.edu.cnpm.projectsupport.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class DemoDatabaseSeedIntegrationTest {

    private static final String DEMO_PULL_REQUESTS =
            "SELECT COUNT(*) FROM github_pull_requests "
                    + "WHERE github_pull_request_id IN (880001, 880002)";

    private static final String DEMO_PULL_REQUEST_COMMITS =
            "SELECT COUNT(*) FROM github_pull_request_commits pc "
                    + "JOIN github_pull_requests pr ON pr.id = pc.pull_request_id "
                    + "WHERE pr.github_pull_request_id IN (880001, 880002)";

    @Test
    void demoSeedCanBeEnabledAfterInitializationWithoutCreatingDuplicates() throws SQLException {
        String url = "jdbc:h2:mem:cnpm_demo_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";

        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            // Keep an anchor connection open while Flyway switches placeholder values.
            migrate(url, false);
            assertEquals(0, count(connection, DEMO_PULL_REQUESTS));
            assertEquals(0, count(connection, DEMO_PULL_REQUEST_COMMITS));

            // H2's IN-list checks retain Flyway's closed V8 migration session.
            // Recreate those checks on the anchor session before re-running the seed.
            refreshH2Checks(connection);

            migrate(url, true);
            assertDemoPullRequestCommits(connection);

            // Re-running after toggling the placeholder must not duplicate data.
            migrate(url, false);
            migrate(url, true);
            assertDemoPullRequestCommits(connection);
        }
    }

    private void migrate(String url, boolean demoEnabled) {
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .placeholders(Map.of("demoSeedEnabled", Boolean.toString(demoEnabled)))
                .load()
                .migrate();
    }

    private void refreshH2Checks(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE github_pull_requests DROP CONSTRAINT chk_github_pr_state");
            statement.execute("ALTER TABLE github_pull_requests ADD CONSTRAINT chk_github_pr_state "
                    + "CHECK (state IN ('OPEN', 'CLOSED', 'MERGED'))");
            statement.execute("ALTER TABLE task_commit_links DROP CONSTRAINT chk_task_commit_link_source");
            statement.execute("ALTER TABLE task_commit_links ADD CONSTRAINT chk_task_commit_link_source "
                    + "CHECK (link_source IN ('AUTO', 'MANUAL'))");
            statement.execute("ALTER TABLE task_pr_links DROP CONSTRAINT chk_task_pr_link_source");
            statement.execute("ALTER TABLE task_pr_links ADD CONSTRAINT chk_task_pr_link_source "
                    + "CHECK (link_source IN ('AUTO', 'MANUAL'))");
        }
    }

    private void assertDemoPullRequestCommits(Connection connection) throws SQLException {
        assertEquals(2, count(connection, DEMO_PULL_REQUESTS));
        assertEquals(2, count(connection, DEMO_PULL_REQUEST_COMMITS));

        String query = "SELECT pr.github_pull_request_id, pr.commit_count, "
                + "c.sha, pc.commit_order, pr.repository_id = c.repository_id AS same_repository "
                + "FROM github_pull_request_commits pc "
                + "JOIN github_pull_requests pr ON pr.id = pc.pull_request_id "
                + "JOIN github_commits c ON c.id = pc.commit_id "
                + "WHERE pr.github_pull_request_id IN (880001, 880002) "
                + "ORDER BY pr.github_pull_request_id";

        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(query)) {
            assertDemoLink(rows, 880001L, "1111111111111111111111111111111111111111");
            assertDemoLink(rows, 880002L, "2222222222222222222222222222222222222222");
            assertFalse(rows.next());
        }
    }

    private void assertDemoLink(ResultSet rows, long pullRequestId, String sha)
            throws SQLException {
        org.junit.jupiter.api.Assertions.assertTrue(rows.next());
        assertEquals(pullRequestId, rows.getLong("github_pull_request_id"));
        assertEquals(1, rows.getInt("commit_count"));
        assertEquals(sha, rows.getString("sha"));
        assertEquals(1, rows.getInt("commit_order"));
        org.junit.jupiter.api.Assertions.assertTrue(rows.getBoolean("same_repository"));
    }

    private int count(Connection connection, String query) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(query)) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
