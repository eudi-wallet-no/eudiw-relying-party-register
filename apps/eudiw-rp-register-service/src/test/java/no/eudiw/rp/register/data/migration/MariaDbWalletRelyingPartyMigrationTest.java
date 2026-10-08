package no.eudiw.rp.register.data.migration;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

@Testcontainers(disabledWithoutDocker = true)
class MariaDbWalletRelyingPartyMigrationTest {

    @Container
    private static final MariaDBContainer database = new MariaDBContainer("mariadb:11.4");

    @Test
    void migratesExistingInstancesOnMariaDb() {
        WalletRelyingPartyMigrationTest.verifyMigration(new DriverManagerDataSource(
            database.getJdbcUrl(), database.getUsername(), database.getPassword()),
            "classpath:mariadb/db/migration");
    }
}
