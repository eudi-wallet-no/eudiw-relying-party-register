package no.idporten.eudiw.trustlist.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.trustlist.domain.Tsl612;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
@ConfigurationProperties(prefix = "trustlist-service")
public class TrustlistServiceProperties {

    @NotBlank
    private String environmentName;

    @Valid
    @NotNull
    public KeyStoreProperties keyStore;

    @Valid
    @NotNull
    private Tsl612 tsl612;
}
