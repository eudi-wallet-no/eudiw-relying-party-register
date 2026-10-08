package no.eudiw.rp.register;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RpRegisterService {
	public static void main(String[] args) {
		SpringApplication.run(RpRegisterService.class, args);
	}
}
