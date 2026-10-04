package com.antonlappa.rechnungapp.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Liquibase changeset {@code 012-clear-foreign-logo-paths}.
 * <p>
 * Liquibase has already applied the changeset at startup, so the test inserts
 * fresh rows and re-executes the changeset's SQL (loaded from the classpath)
 * against the real PostgreSQL database. Rolled back after completion.
 */
@SpringBootTest
@Transactional
class ClearForeignLogoPathsMigrationTest {

    private static final String CHANGESET_ID = "012-clear-foreign-logo-paths";
    private static final String CHANGESET_FILE = "db/changelog/" + CHANGESET_ID + ".sql";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertProfile(UUID userId, String logoPath) {
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, first_name, last_name, role)
                VALUES (?, ?, 'hash', 'Test', 'User', 'USER')
                """, userId, "migration-" + userId + "@example.com");
        jdbcTemplate.update("""
                INSERT INTO company_profiles (id, user_id, company_name, owner_name, address, logo_path)
                VALUES (?, ?, 'Testfirma GmbH', 'Max Mustermann', 'Musterstraße 1', ?)
                """, UUID.randomUUID(), userId, logoPath);
    }

    private String logoPathOf(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT logo_path FROM company_profiles WHERE user_id = ?", String.class, userId);
    }

    private static String changesetSql() throws IOException {
        String content = new ClassPathResource(CHANGESET_FILE).getContentAsString(StandardCharsets.UTF_8);
        return content.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
    }

    @Test
    @DisplayName("changeset is registered and applied by Liquibase")
    void changesetIsApplied() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog WHERE id = ?", Integer.class, CHANGESET_ID);

        assertEquals(1, applied);
    }

    @Test
    @DisplayName("clears foreign logo keys and leaves own keys and NULLs untouched")
    void clearsOnlyForeignKeys() throws IOException {
        UUID userWithForeignKey = UUID.randomUUID();
        UUID userWithArbitraryKey = UUID.randomUUID();
        UUID userWithoutLogo = UUID.randomUUID();
        UUID userWithOwnKey = UUID.randomUUID();
        insertProfile(userWithForeignKey, "logos/" + UUID.randomUUID() + "/logo.png");
        insertProfile(userWithArbitraryKey, "some/other/object.png");
        insertProfile(userWithoutLogo, null);
        insertProfile(userWithOwnKey, "logos/" + userWithOwnKey + "/logo.png");

        jdbcTemplate.execute(changesetSql());

        assertNull(logoPathOf(userWithForeignKey));
        assertNull(logoPathOf(userWithArbitraryKey));
        assertNull(logoPathOf(userWithoutLogo));
        assertEquals("logos/" + userWithOwnKey + "/logo.png", logoPathOf(userWithOwnKey));
    }
}
