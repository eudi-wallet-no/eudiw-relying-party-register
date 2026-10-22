package no.eudiw.rp.register;

import no.idporten.lib.keystore.KeystoreManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@ActiveProfiles("junit")
@SpringBootTest
class RpRegisterServiceTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Autowired
	private KeystoreManager keystoreManager;

	@Test
	void contextLoads() {

	}

	@Test
	void loadsTestOnlyWrpSigningKey() {
		assertInstanceOf(
			ECPublicKey.class,
			keystoreManager.getKeyProvider("wrp-signing").publicKey()
		);
	}

	@Test
	void keepsMigratedDatabaseAndDataBetweenConnections() {
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE connection_lifecycle_test (id INTEGER PRIMARY KEY)");
		try {
			jdbc.update("INSERT INTO connection_lifecycle_test (id) VALUES (1)");

			assertEquals(1, jdbc.queryForObject(
				"SELECT id FROM connection_lifecycle_test", Integer.class));
			flyway.validate();
		} finally {
			jdbc.execute("DROP TABLE connection_lifecycle_test");
		}
	}

}
