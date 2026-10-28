package no.idporten.eudiw.credential.registry.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI().info(new Info().title("Beviskatalog-API").version("1.0").description("API for å vise alle " +
                "bevisene som eksisterer i sandkasse-miljøet. Her får du informasjon som leses av fra " +
                ".well-known/openid-credential-issuer endepunktet til utstedere som er registrert i sandkassen."));
    }
}