package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__unique_user_external_account_provider extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            if (!constraintExists(connection, "user_external_accounts", "uk_user_external_account_provider")) {
                statement.execute("ALTER TABLE user_external_accounts " + "ADD CONSTRAINT uk_user_external_account_provider UNIQUE (user_id, provider)");
            }
        }
    }

    private boolean constraintExists(Connection connection, String table, String name) throws Exception {
        try (ResultSet result = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
            while (result.next()) {
                String indexName = result.getString("INDEX_NAME");
                if (name.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
