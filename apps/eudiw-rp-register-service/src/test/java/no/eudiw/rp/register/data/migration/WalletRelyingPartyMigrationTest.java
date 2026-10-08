package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletRelyingPartyMigrationTest {

    @Test
    void migratesExistingInstancesOnH2() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        verifyMigration(dataSource, "classpath:local-h2/db/migration");
    }

    static void verifyMigration(DataSource dataSource, String vendorLocation) {
        String[] locations = {"classpath:db/migration", vendorLocation};
        Flyway.configure().dataSource(dataSource).locations(locations).target("19").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String firstParty = UUID.randomUUID().toString();
        String secondParty = UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO legal_entity (id, name, orgno, public_sector, created_ms, last_updated_ms, active)
            VALUES (?, 'First legal name', '123456785', TRUE, 10, 20, TRUE),
                   (?, 'Second legal name', '987654321', FALSE, 30, 40, FALSE)
            """, firstParty, secondParty);
        List<String> instances = List.of(
            UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        for (int i = 0; i < instances.size(); i++) {
            jdbc.update("""
                INSERT INTO relying_party_instance
                    (id, trade_name, legal_entity_id, created_ms, last_updated_ms, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, instances.get(i), "Service " + i, i < 2 ? firstParty : secondParty,
                100 + i, 200 + i, i != 1);
        }
        String entitlement = UUID.randomUUID().toString();
        jdbc.update("""
            INSERT INTO relying_party_entitlement
                (id, relying_party_instance_id, entitlement, credential_issuer_url)
            VALUES (?, ?, 'test-entitlement', 'https://issuer.example')
            """, entitlement, instances.getFirst());
        jdbc.update("""
            INSERT INTO relying_party_eaa (id, relying_party_instance_id, namespace, intent)
            VALUES (?, ?, 'namespace', 'intent')
            """, UUID.randomUUID().toString(), instances.getFirst());
        jdbc.update("""
            INSERT INTO access_certificate
                (id, relying_party_instance_id, certificate_pem, subject_dn, serial_no, issuer, ca_id)
            VALUES (?, ?, 'access-pem', 'subject', '1', 'issuer', 'access')
            """, UUID.randomUUID().toString(), instances.getFirst());
        jdbc.update("""
            INSERT INTO issuer_certificate
                (id, entitlement_id, certificate_pem, subject_dn, serial_no, issuer, ca_id)
            VALUES (?, ?, 'issuer-pem', 'subject', '2', 'issuer', 'issuer')
            """, UUID.randomUUID().toString(), entitlement);

        var partiesBefore = jdbc.queryForList("""
            SELECT id, name AS legal_name, orgno, public_sector AS is_psb,
                   created_ms, last_updated_ms, active FROM legal_entity ORDER BY id
            """);
        var instancesBefore = jdbc.queryForList("""
            SELECT id, created_ms, last_updated_ms, active FROM relying_party_instance ORDER BY id
            """);
        var servicesBefore = jdbc.queryForList("""
            SELECT id, trade_name AS service_trade_name, legal_entity_id AS wallet_relying_party_id,
                   created_ms, last_updated_ms FROM relying_party_instance ORDER BY id
            """);
        List<String> unchangedTables = List.of(
            "relying_party_entitlement", "relying_party_eaa", "access_certificate", "issuer_certificate");
        var childrenBefore = unchangedTables.stream()
            .map(table -> jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"))
            .toList();

        Flyway.configure().dataSource(dataSource).locations(locations).load().migrate();

        assertEquals("20", Flyway.configure().dataSource(dataSource).locations(locations)
            .load().info().current().getVersion().getVersion());
        assertEquals(partiesBefore, jdbc.queryForList("SELECT * FROM wallet_relying_party ORDER BY id"));
        assertEquals(instancesBefore, jdbc.queryForList("""
            SELECT id, created_ms, last_updated_ms, active FROM relying_party_instance ORDER BY id
            """));
        assertEquals(servicesBefore, jdbc.queryForList("SELECT * FROM wallet_relying_party_service ORDER BY id"));
        assertEquals(instances.size(), jdbc.queryForObject(
            "SELECT COUNT(*) FROM wallet_relying_party_service", Integer.class));
        assertEquals(instances.size(), jdbc.queryForObject("""
            SELECT COUNT(*) FROM relying_party_instance i
            JOIN wallet_relying_party_service s ON s.id = i.wallet_relying_party_service_id
            WHERE i.id = s.id
            """, Integer.class));
        assertTrue(jdbc.queryForList("""
            SELECT wallet_relying_party_service_id FROM relying_party_instance
            GROUP BY wallet_relying_party_service_id HAVING COUNT(*) <> 1
            """).isEmpty());
        for (int i = 0; i < unchangedTables.size(); i++) {
            assertEquals(childrenBefore.get(i),
                jdbc.queryForList("SELECT * FROM " + unchangedTables.get(i) + " ORDER BY id"));
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE relying_party_instance SET wallet_relying_party_service_id = NULL"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE relying_party_instance SET wallet_relying_party_service_id = ?",
                UUID.randomUUID().toString()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE wallet_relying_party_service SET wallet_relying_party_id = ?",
                UUID.randomUUID().toString()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.update("DELETE FROM wallet_relying_party WHERE id = ?", firstParty));
        assertThrows(org.springframework.dao.InvalidDataAccessResourceUsageException.class,
            () -> jdbc.queryForList("SELECT trade_name, legal_entity_id FROM relying_party_instance"));
    }
}
