package no.eudiw.rp.register.api.v2.wrp;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import no.eudiw.rp.register.config.WrpApiProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WrpApiPropertiesTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsAbsoluteHttpAndHttpsUrlsWithHosts() {
        WrpApiProperties properties = new WrpApiProperties(
            "http://localhost:9200",
            "https://registry.example.test/registrar",
            "https://registry.example.test/registrar/wrp/jwks",
            "wrp-signing",
            "wrp-key"
        );

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void stillRejectsBlankUrlsAsRequiredProperties() {
        WrpApiProperties properties = propertiesWith("issuer", " ");

        assertTrue(validator.validate(properties).stream()
            .anyMatch(violation -> violation.getPropertyPath().toString().equals("issuer")));
    }

    @ParameterizedTest
    @MethodSource("invalidUriValues")
    void rejectsNonAbsoluteHttpUrlsOrUrlsWithoutHost(String property, String value, String expectedMessage) {
        WrpApiProperties properties = propertiesWith(property, value);

        assertTrue(validator.validate(properties).stream()
            .anyMatch(violation -> violation.getMessage().equals(expectedMessage)));
    }

    private static Stream<Arguments> invalidUriValues() {
        return Stream.of("issuer", "registryUri", "jwksUrl")
            .flatMap(property -> Stream.of(
                Arguments.of(property, "relative/path", messageFor(property)),
                Arguments.of(property, "ftp://registry.example.test/key", messageFor(property)),
                Arguments.of(property, "https:///missing-host", messageFor(property)),
                Arguments.of(property, "https://bad host/path", messageFor(property))
            ));
    }

    private static WrpApiProperties propertiesWith(String property, String value) {
        String issuer = property.equals("issuer") ? value : "https://registry.example.test";
        String registryUri = property.equals("registryUri") ? value : "https://registry.example.test/registrar";
        String jwksUrl = property.equals("jwksUrl")
            ? value
            : "https://registry.example.test/registrar/wrp/jwks";
        return new WrpApiProperties(issuer, registryUri, jwksUrl, "wrp-signing", "wrp-key");
    }

    private static String messageFor(String property) {
        return switch (property) {
            case "issuer" -> "wrp_issuer_must_be_absolute_http_uri";
            case "registryUri" -> "wrp_registry_uri_must_be_absolute_http_uri";
            case "jwksUrl" -> "wrp_jwks_url_must_be_absolute_http_uri";
            default -> throw new IllegalArgumentException("Unexpected property: " + property);
        };
    }
}
