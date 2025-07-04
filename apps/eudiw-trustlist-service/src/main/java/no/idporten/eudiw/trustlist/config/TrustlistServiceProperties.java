package no.idporten.eudiw.trustlist.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.trustlist.domain.TLSchemeInformation;
import no.idporten.eudiw.trustlist.domain.TLServiceProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "trustlist-service")
public class TrustlistServiceProperties {

    public KeyStoreProperties keyStore;

    private TLSchemeInformation schemeInformation;

    private TLServiceProvider serviceProvider;

}
