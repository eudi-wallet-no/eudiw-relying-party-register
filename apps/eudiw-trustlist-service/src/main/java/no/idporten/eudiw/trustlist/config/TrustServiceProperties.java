package no.idporten.eudiw.trustlist.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application properties.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "trust-service")
public class TrustServiceProperties {

    public KeyStoreProperties keyStore;

    private SchemeInformationProperties schemeInformation;

}
