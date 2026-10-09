package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
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

    private static void migrate(DataSource dataSource, String targetVersion) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:mariadb/db/migration")
            .target(targetVersion)
            .load()
            .migrate();
    }
}
