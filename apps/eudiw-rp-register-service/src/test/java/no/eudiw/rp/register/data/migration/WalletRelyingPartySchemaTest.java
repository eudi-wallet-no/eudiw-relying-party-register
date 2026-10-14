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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void enforcesCertificateOwnershipUniquenessAndCascadingDeletion() {
        jdbc.update("""
            INSERT INTO wallet_relying_party (id, legal_name, orgno, is_psb)
            VALUES ('party-1', 'Party 1', '123456785', TRUE)
            """);
        jdbc.update("""
            INSERT INTO wallet_relying_party_service
                (id, service_trade_name, wallet_relying_party_id, created_ms, last_updated_ms)
            VALUES ('service-1', 'Service 1', 'party-1', 1, 2)
            """);
        assertEquals("[]", jdbc.queryForObject(
            "SELECT sub_entitlements FROM wallet_relying_party_service WHERE id = 'service-1'", String.class));
        assertEquals("[]", jdbc.queryForObject(
            "SELECT provides_attestations FROM wallet_relying_party_service WHERE id = 'service-1'", String.class));
        assertNull(jdbc.queryForObject(
            "SELECT srv_description FROM wallet_relying_party_service WHERE id = 'service-1'", String.class));
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
