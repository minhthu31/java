package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Stores the actual GitHub pull-request creation timestamp separately from local created_at. */
public class V12__add_github_pr_remote_created_at extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            if (!columnExists(connection, "github_pull_requests", "remote_created_at")) {
                statement.execute("ALTER TABLE github_pull_requests ADD COLUMN remote_created_at TIMESTAMP(6) NULL");
            }
            // Existing rows cannot be reconstructed historically; local creation is the safest fallback.
            statement.execute("UPDATE github_pull_requests SET remote_created_at = created_at WHERE remote_created_at IS NULL");
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table, column)) {
            if (result.next()) return true;
        }
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
            return result.next();
        }
    }
}
