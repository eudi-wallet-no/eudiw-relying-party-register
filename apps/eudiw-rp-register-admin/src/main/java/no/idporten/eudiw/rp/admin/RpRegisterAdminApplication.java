package no.idporten.eudiw.rp.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class RpRegisterAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(RpRegisterAdminApplication.class, args);
	}

}
