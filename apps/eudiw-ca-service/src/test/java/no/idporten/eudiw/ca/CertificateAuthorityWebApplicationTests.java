package no.idporten.eudiw.ca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("When starting the Certificate Authority web application")
@ActiveProfiles("test")
@SpringBootTest
class CertificateAuthorityWebApplicationTests {

	@DisplayName("the application context should load")
	@Test
	void contextLoads() {
	}

}
