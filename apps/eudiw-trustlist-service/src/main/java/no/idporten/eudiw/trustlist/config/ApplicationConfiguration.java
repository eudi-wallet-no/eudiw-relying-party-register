package no.idporten.eudiw.trustlist.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

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

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean
                = new FilterRegistrationBean<>( new ShallowEtagHeaderFilter());
        filterRegistrationBean.addUrlPatterns("*.xtsl", "*.sha2");
        filterRegistrationBean.setName("etagFilter");
        return filterRegistrationBean;
    }

}
