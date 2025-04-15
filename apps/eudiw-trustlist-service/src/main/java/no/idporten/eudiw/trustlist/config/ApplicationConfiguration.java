package no.idporten.eudiw.trustlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creating additional application beans.
 */
@Configuration
public class ApplicationConfiguration {

    @Bean
    public KeyStoreProvider tslKeyStoreProvider(TrustServiceProperties trustServiceProperties) {
        return new KeyStoreProvider(trustServiceProperties.getKeyStore());
    }

    @Bean
    public KeyProvider tslKeyProvider(KeyStoreProvider tslKeyStoreProvider, TrustServiceProperties trustServiceProperties) {
        return new KeyProvider(
                tslKeyStoreProvider.getKeyStore(),
                trustServiceProperties.getKeyStore().keyAlias(),
                trustServiceProperties.getKeyStore().password());
    }

}
