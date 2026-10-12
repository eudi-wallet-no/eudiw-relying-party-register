package no.idporten.eudiw.rp.register.lookup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RpRegisterLookupWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(RpRegisterLookupWebApplication.class, args);
	}

}
