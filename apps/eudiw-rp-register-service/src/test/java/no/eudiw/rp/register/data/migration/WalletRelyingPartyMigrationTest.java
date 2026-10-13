package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletRelyingPartyMigrationTest {

    @Test
    void mergesAccessAndIssuerCertificatesWithoutChangingTheirOwnersOrIdentifiers() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        migrate(dataSource, "20");

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

        migrate(dataSource, "21");

        assertEquals(2, jdbc.queryForObject(
            "SELECT COUNT(*) FROM relying_party_certificates", Integer.class));
        assertEquals("ACCESS", jdbc.queryForObject(
            "SELECT type FROM relying_party_certificates WHERE id = 'access-1'", String.class));
        assertEquals("instance-1", jdbc.queryForObject(
            "SELECT relying_party_instance_id FROM relying_party_certificates WHERE id = 'access-1'",
            String.class));
        assertEquals("access-pem", jdbc.queryForObject(
            "SELECT certificate_pem FROM relying_party_certificates WHERE id = 'access-1'", String.class));
        assertEquals("ISSUER", jdbc.queryForObject(
            "SELECT type FROM relying_party_certificates WHERE id = 'issuer-1'", String.class));
        assertEquals("entitlement-1", jdbc.queryForObject(
            "SELECT entitlement_id FROM relying_party_certificates WHERE id = 'issuer-1'", String.class));
        assertEquals("issuer-pem", jdbc.queryForObject(
            "SELECT certificate_pem FROM relying_party_certificates WHERE id = 'issuer-1'", String.class));
        assertEquals(0, jdbc.queryForObject(
            "SELECT revocation_status FROM relying_party_certificates WHERE id = 'issuer-1'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'RELYING_PARTY_CERTIFICATES'",
            Integer.class));
        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE lower(table_name) IN "
                + "('access_certificate', 'issuer_certificate')",
            Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("""
            INSERT INTO relying_party_certificates (
                id, type, relying_party_instance_id, entitlement_id, issuer, ca_id, certificate_pem,
                subject_dn, serial_no, valid_from_ms, valid_until_ms, revocation_status
            ) VALUES ('wrong-owner', 'ACCESS', NULL, 'entitlement-1', 'issuer', 'ca', 'pem',
                'subject', 'another-serial', 1, 2, -1)
            """));
    }

    @Test
    void givesEachInstanceItsOwnServiceAtTheSameWalletRelyingParty() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        migrate(dataSource, "19");
        jdbc.update("""
            INSERT INTO legal_entity (id, name, orgno)
            VALUES ('party-1', 'Party 1', '123456785'),
                   ('party-2', 'Party 2', '987654321')
            """);
        jdbc.update("""
            INSERT INTO relying_party_instance (id, trade_name, legal_entity_id)
            VALUES ('instance-1', 'Service 1', 'party-1'),
                   ('instance-2', 'Service 2', 'party-1'),
                   ('instance-3', 'Service 3', 'party-2')
            """);

        migrate(dataSource, "20");

        List<InstanceService> instanceServices = jdbc.query("""
            SELECT i.id, s.service_trade_name, s.wallet_relying_party_id
            FROM relying_party_instance i
            JOIN wallet_relying_party_service s ON s.id = i.wallet_relying_party_service_id
            ORDER BY i.id
            """, (row, rowNum) -> new InstanceService(row.getString(1), row.getString(2), row.getString(3)));
        assertEquals(List.of(
            new InstanceService("instance-1", "Service 1", "party-1"),
            new InstanceService("instance-2", "Service 2", "party-1"),
            new InstanceService("instance-3", "Service 3", "party-2")
        ), instanceServices);
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM wallet_relying_party_service", Integer.class));
    }

    @Test
    void addsTs5PartyDataWithoutChangingExistingServiceTables() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        migrate(dataSource, "21");
        jdbc.update("""
            INSERT INTO wallet_relying_party (id, legal_name, orgno, is_psb)
            VALUES ('party-1', 'Party 1', '123456785', TRUE)
            """);
        jdbc.update("""
            INSERT INTO wallet_relying_party_service
                (id, service_trade_name, wallet_relying_party_id, created_ms, last_updated_ms)
            VALUES ('service-1', 'Service 1', 'party-1', 1, 2)
            """);

        migrate(dataSource, "23");

        assertEquals("party-1", jdbc.queryForObject(
            "SELECT wallet_relying_party_id FROM legal_person WHERE id = 'party-1'", String.class));
        assertEquals("Digitaliseringsdirektoratet", jdbc.queryForObject(
            "SELECT name FROM supervisory_authority WHERE wallet_relying_party_id = 'party-1'", String.class));
        assertEquals("NO", jdbc.queryForObject(
            "SELECT country FROM supervisory_authority WHERE wallet_relying_party_id = 'party-1'", String.class));
        assertEquals("Service 1", jdbc.queryForObject(
            "SELECT service_trade_name FROM wallet_relying_party_service WHERE id = 'service-1'",
            String.class));
        assertEquals("NO", jdbc.queryForObject(
            "SELECT country FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("WalletRelyingParty", jdbc.queryForObject(
            "SELECT provider_type FROM wallet_relying_party WHERE id = 'party-1'", String.class));
        assertEquals("Party 1", jdbc.queryForObject(
            "SELECT legal_name FROM wallet_relying_party WHERE id = 'party-1'", String.class));

        jdbc.update("DELETE FROM wallet_relying_party_service WHERE id = 'service-1'");
        jdbc.update("DELETE FROM wallet_relying_party WHERE id = 'party-1'");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM legal_person", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM supervisory_authority", Integer.class));
    }

    private static void migrate(DataSource dataSource, String targetVersion) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration", "classpath:local-h2/db/migration")
            .target(targetVersion)
            .load()
            .migrate();
    }

    private record InstanceService(String instanceId, String serviceTradeName, String walletRelyingPartyId) { }
}
