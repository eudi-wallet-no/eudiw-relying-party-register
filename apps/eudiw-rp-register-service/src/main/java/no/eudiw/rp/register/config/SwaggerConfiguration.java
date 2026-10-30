package no.eudiw.rp.register.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class SwaggerConfiguration {

    private static final Schema<?> NULL_SCHEMA = new Schema<>(SpecVersion.V31).types(Set.of("null"));

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

    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("V1")
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return GroupedOpenApi.builder()
                .group("V2")
                .pathsToMatch("/v2/**")
                .addOpenApiCustomizer(SwaggerConfiguration::addRegistrationPatchSchemas)
                .build();
    }

    private static void addRegistrationPatchSchemas(OpenAPI openApi) {
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        Map.of(
            "RelyingPartyPatch", "RelyingPartyRequest",
            "RelyingPartyServicePatch", "RelyingPartyServiceRequest",
            "RelyingPartyInstancePatch", "RelyingPartyInstanceRequest"
        ).forEach((patchSchemaName, requestSchemaName) -> {
            Map<String, Schema> patchProperties = new LinkedHashMap<>();
            schemas.get(requestSchemaName).getProperties()
                .forEach((name, schema) -> patchProperties.put((String) name, nullableSchema(schema)));
            schemas.put(patchSchemaName, new ObjectSchema()
                .description("Optional properties from " + requestSchemaName + ". Omitted properties are unchanged.")
                .properties(patchProperties));
        });

        Schema registrationPatch = schemas.get("RegistrationPatchRequest");
        registrationPatch.getProperties().replaceAll((name, schema) -> nullableSchema(schema));
    }

    private static Schema nullableSchema(Object schemaValue) {
        if (!(schemaValue instanceof Schema propertySchema)) {
            throw new IllegalStateException("OpenAPI property is not a schema");
        }
        return new Schema<>(SpecVersion.V31).anyOf(List.of(propertySchema, NULL_SCHEMA));
    }

    @Bean
    public GroupedOpenApi wrp() {
        return GroupedOpenApi.builder()
                .group("WRP")
                .pathsToMatch("/wrp/**")
                .build();
    }
}
