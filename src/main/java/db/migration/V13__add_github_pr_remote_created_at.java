package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;


public class V13__add_github_pr_remote_created_at extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!columnExists(connection.getMetaData(), "github_pull_requests", "remote_created_at")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE github_pull_requests ADD COLUMN remote_created_at TIMESTAMP(6) NULL");
            }
        }
    }

    private boolean columnExists(DatabaseMetaData metadata, String table, String column) throws Exception {
        try (ResultSet resultSet = metadata.getColumns(null, null, table, column)) {
            return resultSet.next();
        }
    }
}
