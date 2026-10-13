package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "MARIADB_MIGRATION_TEST_URL", matches = ".+")
class MariaDbWalletRelyingPartyMigrationTest {

    @Test
    void mergesCertificatesWhenTheDatabaseCollationDiffersFromExistingForeignKeys() {
        String url = System.getenv("MARIADB_MIGRATION_TEST_URL");
        String username = System.getenv("MARIADB_MIGRATION_TEST_USERNAME");
        String password = System.getenv("MARIADB_MIGRATION_TEST_PASSWORD");
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        String schema = "certificate_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + schema + "` COLLATE utf8mb3_general_ci");
        try {
            DataSource dataSource = new DriverManagerDataSource(url + schema, username, password);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            migrate(dataSource, "20");
            insertCertificateOwners(jdbc);
            jdbc.update("""
                INSERT INTO access_certificate (
                    id, relying_party_instance_id, certificate_pem, subject_dn, serial_no,
                    valid_from_ms, valid_until_ms, issuer, ca_id, revocation_status
                ) VALUES ('access-1', 'instance-1', 'access-pem', 'access-subject', 'same-serial',
                    10, 20, 'access-issuer', 'access-ca', -1)
                """);
            jdbc.update("""
                INSERT INTO issuer_certificate (
                    id, entitlement_id, issuer, ca_id, certificate_pem, subject_dn, serial_no,
                    valid_from_ms, valid_until_ms, revocation_status
                ) VALUES ('issuer-1', 'entitlement-1', 'issuer-issuer', 'issuer-ca', 'issuer-pem',
                    'issuer-subject', 'same-serial', 30, 40, 0)
                """);
            admin.execute("ALTER DATABASE `" + schema
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci");

            migrate(dataSource, "21");

            assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM relying_party_certificates", Integer.class));
            assertEquals("instance-1", jdbc.queryForObject(
                "SELECT relying_party_instance_id FROM relying_party_certificates WHERE id = 'access-1'",
                String.class));
            assertEquals("entitlement-1", jdbc.queryForObject(
                "SELECT entitlement_id FROM relying_party_certificates WHERE id = 'issuer-1'",
                String.class));
            assertEquals("utf8mb3_general_ci", jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_certificates'
                    AND COLUMN_NAME = 'entitlement_id'
                """, String.class));
            assertEquals("utf8mb3_general_ci", jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_certificates'
                    AND COLUMN_NAME = 'relying_party_instance_id'
                """, String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("""
                    UPDATE relying_party_certificates
                    SET entitlement_id = 'missing'
                    WHERE id = 'issuer-1'
                    """));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("""
                    UPDATE relying_party_certificates
                    SET type = 'OTHER'
                    WHERE id = 'issuer-1'
                    """));
        } finally {
            admin.execute("DROP DATABASE `" + schema + "`");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"utf8mb3_general_ci", "utf8mb4_uca1400_ai_ci"})
    void preservesForeignKeyCollationsWhenDatabaseDefaultsDiffer(String collation) {
        String url = System.getenv("MARIADB_MIGRATION_TEST_URL");
        String username = System.getenv("MARIADB_MIGRATION_TEST_USERNAME");
        String password = System.getenv("MARIADB_MIGRATION_TEST_PASSWORD");
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + schema + "` COLLATE " + collation);
        try {
            DataSource dataSource = new DriverManagerDataSource(url + schema, username, password);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            migrate(dataSource, "19");
            jdbc.update("INSERT INTO legal_entity (id, name, orgno) VALUES ('party-1', 'Party 1', '123456785')");
            jdbc.update("INSERT INTO relying_party_instance (id, trade_name, legal_entity_id) VALUES ('instance-1', 'Service 1', 'party-1')");
            admin.execute("ALTER DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_uca1400_ai_ci");

            migrate(dataSource, "20");

            assertEquals("Service 1", jdbc.queryForObject("""
                SELECT s.service_trade_name FROM relying_party_instance i
                JOIN wallet_relying_party_service s ON s.id = i.wallet_relying_party_service_id
                WHERE i.id = 'instance-1' AND s.wallet_relying_party_id = 'party-1'
                """, String.class));
            assertEquals(collation, jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party_service'
                    AND COLUMN_NAME = 'wallet_relying_party_id'
                """, String.class));
            assertEquals(collation, jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'relying_party_instance'
                    AND COLUMN_NAME = 'wallet_relying_party_service_id'
                """, String.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE relying_party_instance SET wallet_relying_party_service_id = 'missing'"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE wallet_relying_party_service SET wallet_relying_party_id = 'missing'"));
        } finally {
            admin.execute("DROP DATABASE `" + schema + "`");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"utf8mb3_general_ci", "utf8mb4_uca1400_ai_ci"})
    void addsTs5SupportTablesWithForeignKeysMatchingExistingCollation(String collation) {
        String url = System.getenv("MARIADB_MIGRATION_TEST_URL");
        String username = System.getenv("MARIADB_MIGRATION_TEST_USERNAME");
        String password = System.getenv("MARIADB_MIGRATION_TEST_PASSWORD");
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + schema + "` COLLATE " + collation);
        try {
            DataSource dataSource = new DriverManagerDataSource(url + schema, username, password);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            migrate(dataSource, "21");
            jdbc.update("""
                INSERT INTO wallet_relying_party (id, legal_name, orgno, is_psb)
                VALUES ('party-1', 'Party 1', '123456785', TRUE)
                """);

            migrate(dataSource, "23");

            assertEquals("party-1", jdbc.queryForObject(
                "SELECT wallet_relying_party_id FROM legal_person WHERE id = 'party-1'", String.class));
            assertEquals("Digitaliseringsdirektoratet", jdbc.queryForObject(
                "SELECT name FROM supervisory_authority WHERE wallet_relying_party_id = 'party-1'",
                String.class));
            assertEquals("NO", jdbc.queryForObject(
                "SELECT country FROM wallet_relying_party WHERE id = 'party-1'", String.class));
            assertEquals("WalletRelyingParty", jdbc.queryForObject(
                "SELECT provider_type FROM wallet_relying_party WHERE id = 'party-1'", String.class));
            assertEquals(collation, jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party_identifiers'
                    AND COLUMN_NAME = 'wallet_relying_party_id'
                """, String.class));
            assertEquals(collation, jdbc.queryForObject("""
                SELECT COLLATION_NAME FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'supervisory_authority'
                    AND COLUMN_NAME = 'wallet_relying_party_id'
                """, String.class));
            jdbc.update("""
                INSERT INTO wallet_relying_party_identifiers
                    (wallet_relying_party_id, position, identifier_type, identifier_value)
                VALUES ('party-1', 0, 'urn:test:identifier', '123456785')
                """);
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("""
                    INSERT INTO wallet_relying_party_identifiers
                        (wallet_relying_party_id, position, identifier_type, identifier_value)
                    VALUES ('missing', 0, 'urn:test:identifier', 'missing')
                    """));
        } finally {
            admin.execute("DROP DATABASE `" + schema + "`");
        }
    }

    private static void migrate(DataSource dataSource, String targetVersion) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:mariadb/db/migration")
            .target(targetVersion)
            .load()
            .migrate();
    }

    private static void insertCertificateOwners(JdbcTemplate jdbc) {
        jdbc.update("""
            INSERT INTO wallet_relying_party (id, legal_name, orgno, is_psb)
            VALUES ('party-1', 'Party 1', '123456785', TRUE)
            """);
        jdbc.update("""
            INSERT INTO wallet_relying_party_service
                (id, service_trade_name, wallet_relying_party_id, created_ms, last_updated_ms)
            VALUES ('service-1', 'Service 1', 'party-1', 1, 2)
            """);
        jdbc.update("""
            INSERT INTO relying_party_instance
                (id, wallet_relying_party_service_id, created_ms, last_updated_ms, active)
            VALUES ('instance-1', 'service-1', 1, 2, TRUE)
            """);
        jdbc.update("""
            INSERT INTO relying_party_entitlement
                (id, relying_party_instance_id, entitlement)
            VALUES ('entitlement-1', 'instance-1', 'urn:test:issuer')
            """);
    }
}
