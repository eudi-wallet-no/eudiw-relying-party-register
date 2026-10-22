package no.eudiw.rp.register.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "eudiw-rp-register-service")
public record RegistryProperties(
    @NotBlank(message = "registry-uri (REGISTRY_URI) must be configured; it is advertised in WalletRelyingParty resources")
    String registryUri
) {

    @AssertTrue(message = "registry-uri (REGISTRY_URI) must be an absolute URL")
    public boolean isRegistryUriAbsolute() {
        if (registryUri == null) {
            return false;
        }
        try {
            URI uri = URI.create(registryUri);
            return uri.isAbsolute() && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
