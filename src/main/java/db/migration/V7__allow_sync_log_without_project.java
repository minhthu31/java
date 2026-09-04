package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V7__allow_sync_log_without_project extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        DatabaseMetaData metadata = connection.getMetaData();
        String product = metadata.getDatabaseProductName();

        try (var statement = connection.createStatement()) {
            if (product != null && product.toLowerCase().contains("mysql")) {
                statement.execute("ALTER TABLE sync_logs MODIFY COLUMN project_id BIGINT NULL");
            } else {
                statement.execute("ALTER TABLE sync_logs ALTER COLUMN project_id BIGINT NULL");
            }
        }
    }
}
