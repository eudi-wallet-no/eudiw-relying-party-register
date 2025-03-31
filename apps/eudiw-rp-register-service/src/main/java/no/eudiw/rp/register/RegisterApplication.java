package no.eudiw.rp.register;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
public class RegisterApplication {

	public static void main(String[] args) {
		//SpringApplication.run(RegisterApplication.class, args);

		SpringApplication springApplication = new SpringApplication(RegisterApplication.class);
		springApplication.addListeners(new PropertiesLogger());
		springApplication.run(args);
	}

}
