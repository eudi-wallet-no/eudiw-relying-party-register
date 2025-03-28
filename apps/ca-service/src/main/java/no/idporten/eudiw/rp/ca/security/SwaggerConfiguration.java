package no.idporten.eudiw.rp.ca.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .name("API Key")
                .in(SecurityScheme.In.HEADER)
                .name("X-API-KEY");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().addSecurityItem(new SecurityRequirement().
                        addList("API Key"))
                .components(new Components().addSecuritySchemes
                        ("API Key", createAPIKeyScheme()));
    }

}
