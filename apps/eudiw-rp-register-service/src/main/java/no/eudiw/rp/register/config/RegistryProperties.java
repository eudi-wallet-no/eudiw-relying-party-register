package no.eudiw.rp.register.config;

import no.eudiw.rp.register.exception.RegisterServiceException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "eudiw-rp-register-service")
public record RegistryProperties(String registryUri) {

    public String requireRegistryUri() {
        if (registryUri == null || registryUri.isBlank()) {
            throw new RegisterServiceException("Public registry URI is not configured");
        }
        try {
            URI uri = URI.create(registryUri);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new RegisterServiceException("Public registry URI must be an absolute URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new RegisterServiceException("Public registry URI is invalid");
        }
        return registryUri;
    }
}
