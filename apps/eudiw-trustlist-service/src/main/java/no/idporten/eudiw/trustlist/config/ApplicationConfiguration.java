package no.idporten.eudiw.trustlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creating additional application beans.
 */
@Configuration
public class ApplicationConfiguration {

    @Bean
    public KeyStoreProvider tslKeyStoreProvider(TrustlistServiceProperties trustlistServiceProperties) {
        return new KeyStoreProvider(trustlistServiceProperties.getKeyStore());
    }

    @Bean
    public KeyProvider tslKeyProvider(KeyStoreProvider tslKeyStoreProvider, TrustlistServiceProperties trustlistServiceProperties) {
        return new KeyProvider(
                tslKeyStoreProvider.getKeyStore(),
                trustlistServiceProperties.getKeyStore().keyAlias(),
                trustlistServiceProperties.getKeyStore().password());
    }

}
