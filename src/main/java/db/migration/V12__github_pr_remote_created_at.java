package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V12__github_pr_remote_created_at extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        boolean mysql = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        try (Statement statement = connection.createStatement()) {
            addColumn(statement, "github_pull_requests",
                    "remote_created_at TIMESTAMP(6) NULL");

            // Do not copy local created_at into remote_created_at.
            // Existing rows remain NULL until a GitHub synchronization
            // provides the real creation time from GitHub.
        }
    }

    private void addColumn(Statement statement, String table, String definition) throws Exception {
        String column = definition.substring(0, definition.indexOf(' '));
        if (!columnExists(statement.getConnection(), table, column)) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + definition);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table, column)) {
            if (result.next()) {
                return true;
            }
        }
        try (ResultSet result = connection.getMetaData().getColumns(
                null, null, table.toUpperCase(), column.toUpperCase())) {
            return result.next();
        }
    }
}
