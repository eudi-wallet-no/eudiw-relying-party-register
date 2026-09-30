package no.idporten.eudiw.rp.admin.web.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "eudiw-admin-web.csp")
public record CspProperties(@NotNull Mode mode, @NotBlank String policy) {

    public enum Mode {
        REPORT_ONLY, ENFORCE
    }
}
