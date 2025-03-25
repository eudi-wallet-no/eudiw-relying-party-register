package no.idporten.eudiw.rp.ca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class RpCertificateAuthorityWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(RpCertificateAuthorityWebApplication.class, args);
		addBouncyCastleProvider();
	}

	/**
	 * Bootstrap Bouncy Castle.
	 */
	private static void addBouncyCastleProvider() {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}

}
