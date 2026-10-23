package no.eudiw.rp.register.api.v2.wrp;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service.wrp")
public record WrpApiProperties(
    @NotBlank String issuer,
    @NotBlank String registryUri,
    @NotBlank String jwksUrl,
    @NotBlank String signingKeystore,
    @NotBlank String signingKeyId
) {

    private static final List<String> ALLOWED_SCHEMES = List.of("http", "https");

    @AssertTrue(message = "wrp_issuer_must_be_absolute_http_uri")
    public boolean isIssuerValid() {
        return isHttpUri(issuer);
    }

    @AssertTrue(message = "wrp_registry_uri_must_be_absolute_http_uri")
    public boolean isRegistryUriValid() {
        return isHttpUri(registryUri);
    }

    @AssertTrue(message = "wrp_jwks_url_must_be_absolute_http_uri")
    public boolean isJwksUrlValid() {
        return isHttpUri(jwksUrl);
    }

    private static boolean isHttpUri(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute()
                && uri.getHost() != null
                && ALLOWED_SCHEMES.contains(uri.getScheme());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
