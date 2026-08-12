package no.idporten.eudiw.credential.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication
public class CredentialRegisteryApplication {

	public static void main(String[] args) {
		SpringApplication.run(CredentialRegisteryApplication.class, args);
	}

}
