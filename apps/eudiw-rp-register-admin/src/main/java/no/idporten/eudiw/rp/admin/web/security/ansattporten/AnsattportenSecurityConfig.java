package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
@Profile("selfservice")
@ConditionalOnBooleanProperty("eudiw-admin-web.enable-security")
public class AnsattportenSecurityConfig {

    @Bean
    public Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginConfigurer(
        AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver,
        AnsattportenOidcUserService ansattportenOidcUserService) {
        return oauth -> oauth
            .authorizationEndpoint(
                endpoint -> endpoint.authorizationRequestResolver(
                    ansattportenAuthorizationRequestResolver))
            .userInfoEndpoint(endp -> endp.oidcUserService(ansattportenOidcUserService));
    }

    @Bean
    public AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        AnsattportenAuthzConfig ansattportenAuthzConfig) {
        return new AnsattportenAuthorizationRequestResolver(
            clientRegistrationRepository, ansattportenAuthzConfig);
    }

    @Bean
    public AnsattportenOidcUserService ansattportenOidcUserService(
        EnhetsregisteretService enhetsregisteretService) {
        return new AnsattportenOidcUserService(enhetsregisteretService);
    }
}
