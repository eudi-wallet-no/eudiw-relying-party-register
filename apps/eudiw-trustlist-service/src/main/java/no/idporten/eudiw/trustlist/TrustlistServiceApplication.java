package no.idporten.eudiw.trustlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class TrustlistServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustlistServiceApplication.class, args);
	}

}
