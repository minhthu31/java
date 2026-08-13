package vn.edu.cnpm.projectsupport.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DatabaseFoundationContractTests {

    private static final String TEST_PASSWORD = "password";
    private static final String TEST_PASSWORD_HASH =
            "$2a$10$X5SEXqMNs8g55FRoNmLyAuT8ad6CumKvabP.fW5ieA655hYCLNgq2";

    @Test
    void seedContainsExactlyTheFourSupportedRoles() throws IOException {
        String migration = readMigration();
        Matcher matcher = Pattern.compile("WHERE r\\.code = '([A-Z_]+)'").matcher(migration);
        Set<String> roles = new java.util.HashSet<>();
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }

        assertEquals(Set.of("ADMIN", "LECTURER", "TEAM_LEADER", "TEAM_MEMBER"), roles);
        assertFalse(migration.contains("'STUDENT'"));
        assertFalse(migration.contains("'STAFF'"));
    }

    @Test
    void seedUsesBcryptHashInsteadOfPlaintextPassword() throws IOException {
        String migration = readMigration();
        assertTrue(migration.contains(TEST_PASSWORD_HASH));
        assertTrue(new BCryptPasswordEncoder().matches(TEST_PASSWORD, TEST_PASSWORD_HASH));
        assertFalse(migration.contains("'" + TEST_PASSWORD + "'"));
    }

    @Test
    void seedProvidesAllRequiredUserColumns() throws IOException {
        String migration = readMigration();
        assertTrue(migration.contains(
                "INSERT INTO users (role_id, username, email, password_hash, full_name, status)"));
        assertEquals(4, countOccurrences(migration, "INSERT INTO users"));
        assertEquals(4, countOccurrences(migration, "'ACTIVE'"));
    }

    private String readMigration() throws IOException {
        return new ClassPathResource("db/migration/V3__seed_test_users.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private int countOccurrences(String value, String token) {
        return (value.length() - value.replace(token, "").length()) / token.length();
    }
}
