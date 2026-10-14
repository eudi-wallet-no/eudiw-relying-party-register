package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class WalletRelyingPartySchemaTest {

    @Container
    private static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

    private Flyway flyway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpDatabase() {
        var dataSource = new DriverManagerDataSource(
            database.getJdbcUrl(), database.getUsername(), database.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:mariadb/db/migration")
            .cleanDisabled(false)
            .load();
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void validatesCurrentSchemaWithoutReapplyingMigrations() {
        flyway.validate();

        assertEquals(0, flyway.migrate().migrationsExecuted);
    }

    @Test
    void movesTheExistingNameToTradeNameAndSeedsTs5PartyData() {
        flyway.clean();
        Flyway.configure()
            .configuration(flyway.getConfiguration())
            .target("21")
            .load()
            .migrate();
        jdbc.update("""
            INSERT INTO wallet_relying_party (id, legal_name, orgno, is_psb)
            VALUES ('party-1', 'Party 1', '123456785', TRUE)
            """);

        flyway.migrate();

        assertEquals("Party 1", jdbc.queryForObject(
            "SELECT trade_name FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[\"Party 1\"]", jdbc.queryForObject(
            "SELECT legal_name FROM legal_person WHERE wallet_relying_party_id = 'party-1'",
            String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT policy FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT identifier FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT postal_address FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT email FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT phone FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT info_uri FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals(2, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME IN (
                  'legal_person',
                  'wallet_relying_party_x5c'
              )
            """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party_identifier'
            """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'legal_person_legal_name'
            """, Integer.class));
        assertEquals(1, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'legal_person'
              AND COLUMN_NAME = 'legal_name'
              AND IS_NULLABLE = 'NO'
            """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party_policy'
            """, Integer.class));
        assertEquals(6, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'wallet_relying_party'
              AND COLUMN_NAME IN ('policy', 'identifier', 'postal_address', 'email', 'phone', 'info_uri')
              AND IS_NULLABLE = 'NO'
            """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME IN (
                  'wallet_relying_party_postal_address',
                  'wallet_relying_party_email',
                  'wallet_relying_party_phone',
                  'wallet_relying_party_info_uri'
              )
            """, Integer.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME IN (
                  'legal_person_legal_names',
                  'wallet_relying_party_identifiers',
                  'wallet_relying_party_policies',
                  'wallet_relying_party_postal_addresses',
                  'wallet_relying_party_emails',
                  'wallet_relying_party_phones',
                  'wallet_relying_party_info_uris'
              )
            """, Integer.class));
        assertEquals("NO", jdbc.queryForObject(
            "SELECT country FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("WalletRelyingParty", jdbc.queryForObject(
            "SELECT provider_type FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals(0, jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME IN (
                  'supervisory_authority',
                  'supervisory_authority_emails',
                  'supervisory_authority_phones',
                  'supervisory_authority_form_uris'
              )
            """, Integer.class));
        assertEquals("YES", jdbc.queryForObject("""
            SELECT IS_NULLABLE FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wallet_relying_party' AND COLUMN_NAME = 'trade_name'
            """, String.class));

        jdbc.update("DELETE FROM wallet_relying_party WHERE id = 'party-1'");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM legal_person", Integer.class));
    }

    @Test
    void enforcesCertificateOwnershipUniquenessAndCascadingDeletion() {
        jdbc.update("""
            INSERT INTO wallet_relying_party
                (id, trade_name, orgno, is_psb, policy, identifier, postal_address, email, phone, info_uri)
            VALUES ('party-1', 'Party 1', '123456785', TRUE, '[]', '[]', '[]', '[]', '[]', '[]')
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
        jdbc.update("""
            INSERT INTO relying_party_certificates (
                id, type, relying_party_instance_id, entitlement_id, issuer, ca_id,
                certificate_pem, subject_dn, serial_no, valid_from_ms, valid_until_ms, revocation_status
            ) VALUES
                ('access-1', 'ACCESS', 'instance-1', NULL, 'issuer', 'ca', 'pem',
                    'subject', 'same-serial', 10, 20, -1),
                ('issuer-1', 'ISSUER', NULL, 'entitlement-1', 'issuer', 'ca', 'pem',
                    'subject', 'same-serial', 30, 40, 0)
            """);

        assertEquals(2, jdbc.queryForObject(
            "SELECT COUNT(*) FROM relying_party_certificates", Integer.class));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
            UPDATE relying_party_certificates
            SET relying_party_instance_id = NULL, entitlement_id = 'entitlement-1'
            WHERE id = 'access-1'
            """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
            UPDATE relying_party_certificates SET entitlement_id = 'missing' WHERE id = 'issuer-1'
            """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
            UPDATE relying_party_certificates SET type = 'OTHER' WHERE id = 'issuer-1'
            """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
            UPDATE relying_party_certificates
            SET type = 'ACCESS', relying_party_instance_id = 'instance-1', entitlement_id = NULL
            WHERE id = 'issuer-1'
            """));

        jdbc.update("DELETE FROM relying_party_entitlement WHERE id = 'entitlement-1'");
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM relying_party_certificates", Integer.class));
        jdbc.update("DELETE FROM relying_party_instance WHERE id = 'instance-1'");
        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM relying_party_certificates", Integer.class));
    }
}
