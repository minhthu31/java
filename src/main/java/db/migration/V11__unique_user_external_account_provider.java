package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V11__unique_user_external_account_provider extends BaseJavaMigration {

    private static final String TABLE_USER_EXTERNAL_ACCOUNTS = "user_external_accounts";
    private static final String UNIQUE_CONSTRAINT = "uk_user_external_account_provider";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        removeDuplicateUserExternalAccounts(connection);

        if (!constraintExists(connection,TABLE_USER_EXTERNAL_ACCOUNTS, UNIQUE_CONSTRAINT)) {

            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE user_external_accounts " + "ADD CONSTRAINT uk_user_external_account_provider " + "UNIQUE (user_id, provider)");
            }
        }
    }

    private void removeDuplicateUserExternalAccounts(Connection connection)
            throws Exception {

        String duplicateGroupsSql = "SELECT user_id, provider, MIN(id) AS keep_id " + "FROM user_external_accounts "
                        + "GROUP BY user_id, provider " + "HAVING COUNT(*) > 1";

        try (Statement statement = connection.createStatement();
             ResultSet groups = statement.executeQuery(duplicateGroupsSql)) {

            while (groups.next()) {
                long userId = groups.getLong("user_id");
                String provider = groups.getString("provider");
                long keepId = groups.getLong("keep_id");

                removeDuplicateAccounts(connection, userId, provider, keepId);
            }
        }
    }

    private void removeDuplicateAccounts(Connection connection, long userId, String provider, long keepId) throws Exception {

        String duplicateIdsSql = "SELECT id " + "FROM user_external_accounts " + "WHERE user_id = ? " 
            + "AND provider = ? "+ "AND id <> ? " + "ORDER BY id";

        try (PreparedStatement selectStatement = connection.prepareStatement(duplicateIdsSql)) {

            selectStatement.setLong(1, userId);
            selectStatement.setString(2, provider);
            selectStatement.setLong(3, keepId);

            try (ResultSet duplicates = selectStatement.executeQuery()) {
                while (duplicates.next()) {
                    long duplicateId = duplicates.getLong("id");

                    updateCommitReferences(connection, duplicateId, keepId);

                    updatePullRequestReferences( connection, duplicateId, keepId);

                    deleteDuplicateAccount(connection, duplicateId);
                }
            }
        }
    }

    private void updateCommitReferences(Connection connection, long duplicateId, long keepId) throws Exception {

        String sql ="UPDATE github_commits " + "SET author_external_account_id = ? " + "WHERE author_external_account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, keepId);
            statement.setLong(2, duplicateId);
            statement.executeUpdate();
        }
    }

    private void updatePullRequestReferences(Connection connection, long duplicateId, long keepId) throws Exception {

        String sql = "UPDATE github_pull_requests " + "SET author_external_account_id = ? " + "WHERE author_external_account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, keepId);
            statement.setLong(2, duplicateId);
            statement.executeUpdate();
        }
    }


    private void deleteDuplicateAccount(Connection connection, long duplicateId) throws Exception {

        String sql = "DELETE FROM user_external_accounts " + "WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, duplicateId);
            statement.executeUpdate();
        }
    }

    private boolean constraintExists(Connection connection,String table, String name) throws Exception {
        try (ResultSet resultSet =connection.getMetaData().getIndexInfo( null, null, table, false,false)) {

            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                if (name.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }

        return false;
    }
}