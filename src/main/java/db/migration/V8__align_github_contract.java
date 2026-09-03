package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Aligns the GitHub persistence model with the CNPM-88 contract.
 *
 * The migration deliberately keeps legacy remote identifiers nullable because
 * rows created before CNPM-88 cannot be reconstructed from local data. New
 * GitHub sync/upsert code must populate those identifiers.
 */
public class V8__align_github_contract extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = isMySql(connection.getMetaData());

        try (Statement statement = connection.createStatement()) {
            addColumn(statement, "github_repositories", "node_id VARCHAR(255) NULL");
            addColumn(statement, "github_repositories", "name VARCHAR(255) NULL");
            addColumn(statement, "github_repositories", "owner_github_user_id BIGINT NULL");
            addColumn(statement, "github_repositories", "owner_login VARCHAR(100) NULL");
            addColumn(statement, "github_repositories", "private_repository BOOLEAN NOT NULL DEFAULT FALSE");
            addColumn(statement, "github_repositories", "archived BOOLEAN NOT NULL DEFAULT FALSE");
            addColumn(statement, "github_repositories", "remote_updated_at TIMESTAMP(6) NULL");

            statement.executeUpdate("""
                    UPDATE github_repositories
                    SET name = CASE
                            WHEN LOCATE('/', full_name) > 0 THEN SUBSTRING(full_name, LOCATE('/', full_name) + 1)
                            ELSE full_name END,
                        owner_login = CASE
                            WHEN LOCATE('/', full_name) > 0 THEN SUBSTRING(full_name, 1, LOCATE('/', full_name) - 1)
                            ELSE full_name END
                    WHERE name IS NULL OR owner_login IS NULL
                    """);
            alterNotNull(statement, mysql, "github_repositories", "name", "VARCHAR(255)");
            alterNotNull(statement, mysql, "github_repositories", "owner_login", "VARCHAR(100)");

            addColumn(statement, "github_commits", "author_github_user_id BIGINT NULL");
            addColumn(statement, "github_commits", "author_login VARCHAR(100) NULL");
            addColumn(statement, "github_commits", "git_author_name VARCHAR(255) NULL");
            addColumn(statement, "github_commits", "git_author_email VARCHAR(320) NULL");
            addColumn(statement, "github_commits", "parent_shas TEXT NULL");
            statement.executeUpdate("UPDATE github_commits SET additions = COALESCE(additions, 0), deletions = COALESCE(deletions, 0)");
            alterNotNull(statement, mysql, "github_commits", "additions", "INT DEFAULT 0");
            alterNotNull(statement, mysql, "github_commits", "deletions", "INT DEFAULT 0");

            addColumn(statement, "github_pull_requests", "github_pull_request_id BIGINT NULL");
            addColumn(statement, "github_pull_requests", "body TEXT NULL");
            addColumn(statement, "github_pull_requests", "author_github_user_id BIGINT NULL");
            addColumn(statement, "github_pull_requests", "author_login VARCHAR(100) NULL");
            addColumn(statement, "github_pull_requests", "head_sha VARCHAR(64) NULL");
            addColumn(statement, "github_pull_requests", "draft BOOLEAN NOT NULL DEFAULT FALSE");
            addColumn(statement, "github_pull_requests", "merge_commit_sha VARCHAR(64) NULL");
            addColumn(statement, "github_pull_requests", "commit_count INT NULL");
            addColumn(statement, "github_pull_requests", "additions INT NOT NULL DEFAULT 0");
            addColumn(statement, "github_pull_requests", "deletions INT NOT NULL DEFAULT 0");
            addColumn(statement, "github_pull_requests", "changed_files INT NULL");
            addColumn(statement, "github_pull_requests", "closed_at TIMESTAMP(6) NULL");

            statement.executeUpdate("""
                    UPDATE github_pull_requests
                    SET state = CASE
                            WHEN merged_at IS NOT NULL THEN 'MERGED'
                            WHEN UPPER(state) = 'OPEN' THEN 'OPEN'
                            ELSE 'CLOSED' END,
                        additions = COALESCE(additions, 0),
                        deletions = COALESCE(deletions, 0),
                        closed_at = CASE
                            WHEN UPPER(state) IN ('CLOSED', 'MERGED') THEN COALESCE(closed_at, merged_at)
                            ELSE closed_at END
                    """);
            alterNotNull(statement, mysql, "github_pull_requests", "additions", "INT DEFAULT 0");
            alterNotNull(statement, mysql, "github_pull_requests", "deletions", "INT DEFAULT 0");

            addUniqueConstraint(statement, "github_pull_requests", "uk_github_pull_request_id", "github_pull_request_id");

            addColumn(statement, "task_commit_links", "matched_from VARCHAR(30) NULL");
            addColumn(statement, "task_pr_links", "matched_from VARCHAR(30) NULL");

            statement.executeUpdate("""
                    UPDATE task_commit_links
                    SET link_source = CASE WHEN UPPER(link_source) = 'AUTO' THEN 'AUTO' ELSE 'MANUAL' END
                    WHERE link_source IS NULL OR UPPER(link_source) NOT IN ('AUTO', 'MANUAL')
                    """);
            statement.executeUpdate("""
                    UPDATE task_pr_links
                    SET link_source = CASE WHEN UPPER(link_source) = 'AUTO' THEN 'AUTO' ELSE 'MANUAL' END
                    WHERE link_source IS NULL OR UPPER(link_source) NOT IN ('AUTO', 'MANUAL')
                    """);

            addCheckConstraint(statement, "task_commit_links", "chk_task_commit_link_source",
                    "link_source IN ('AUTO', 'MANUAL')");
            addCheckConstraint(statement, "task_pr_links", "chk_task_pr_link_source",
                    "link_source IN ('AUTO', 'MANUAL')");
            addCheckConstraint(statement, "github_pull_requests", "chk_github_pr_state",
                    "state IN ('OPEN', 'CLOSED', 'MERGED')");
        }
    }

    private boolean isMySql(DatabaseMetaData metadata) throws Exception {
        String product = metadata.getDatabaseProductName();
        return product != null && product.toLowerCase().contains("mysql");
    }

    private void addColumn(Statement statement, String table, String definition) throws Exception {
        String column = definition.substring(0, definition.indexOf(' '));
        if (!columnExists(statement.getConnection(), table, column)) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + definition);
        }
    }

    private void addUniqueConstraint(Statement statement, String table, String name, String column) throws Exception {
        if (!constraintExists(statement.getConnection(), table, name)) {
            statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " UNIQUE (" + column + ")");
        }
    }

    private void addCheckConstraint(Statement statement, String table, String name, String expression) throws Exception {
        if (!constraintExists(statement.getConnection(), table, name)) {
            statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + name + " CHECK (" + expression + ")");
        }
    }

    private void alterNotNull(Statement statement, boolean mysql, String table, String column, String type) throws Exception {
        if (mysql) {
            statement.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + type + " NOT NULL");
        } else {
            statement.execute("ALTER TABLE " + table + " ALTER COLUMN " + column + " SET NOT NULL");
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table, column)) {
            if (result.next()) {
                return true;
            }
        }
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
            return result.next();
        }
    }

    private boolean constraintExists(Connection connection, String table, String name) throws Exception {
        try (ResultSet result = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (result.next()) {
                if (name.equalsIgnoreCase(result.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
