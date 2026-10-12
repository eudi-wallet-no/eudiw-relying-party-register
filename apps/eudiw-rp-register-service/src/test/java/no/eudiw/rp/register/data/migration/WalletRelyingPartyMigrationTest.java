package no.eudiw.rp.register.data.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletRelyingPartyMigrationTest {

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
