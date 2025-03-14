package no.eudiw.rp.register.security;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.security")
public record ApiKeySecurityProperties(@NotEmpty String apiKey,
                                       @NotEmpty String[] includePaths,
                                       @NotEmpty String[] excludePaths) {
}
