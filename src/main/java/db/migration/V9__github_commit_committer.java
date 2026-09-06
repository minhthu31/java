package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds Git author/committer details required by the GitHub commit snapshot contract. */
public class V9__github_commit_committer extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            addColumn(statement, "github_commits", "git_committer_name VARCHAR(255) NULL");
            addColumn(statement, "github_commits", "git_committer_email VARCHAR(320) NULL");
            addColumn(statement, "github_commits", "committer_at TIMESTAMP(6) NULL");
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
