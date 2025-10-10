package no.idporten.eudiw.rp.admin.web.security.entraid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;

@Configuration
@Profile("admin")
@ConditionalOnBooleanProperty("eudiw-admin-web.enable-security")
public class EntraIdSecurityConfig {

    @Bean
    public Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginConfigurer(
        EntraIdOidcUserService entraIdOidcUserService) {
        return oauth -> oauth.userInfoEndpoint(
            userInfo -> userInfo.oidcUserService(entraIdOidcUserService));
    }
}
