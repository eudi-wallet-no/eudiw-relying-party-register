package no.idporten.eudiw.rp.admin.web.security.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;

@Configuration
@Profile("local-security-test")
public class SecurityTestConfig {

    // fallback ClientRegistrationRepository, since no such bean exists if there
    // are no oauth providers set in Spring security properties, and it is required
    // by some of the beans in the base security config.
    @Bean
    @ConditionalOnMissingBean
    public ClientRegistrationRepository dummyTestClientRegistrationRepository() {
        return _ -> null;
    }

    @Bean
    @Primary
    public OAuth2AuthorizationRequestResolver mockOAuth2AuthorizationRequestResolver() {
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                String authRequestUriPrefix = "/oauth2/authorization/";
                String requestUri = request.getRequestURI();
                if (!requestUri.startsWith(authRequestUriPrefix)) {
                    return null;
                }
                String registrationId = requestUri.replaceFirst(authRequestUriPrefix, "");
                return OAuth2AuthorizationRequest
                           .authorizationCode()
                           .authorizationUri("dummy-auth-uri")
                           .clientId("dummy-client-id")
                           .state("dummy-state")
                           .attributes(Map.of("registration_id", registrationId))
                           .build();
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                HttpServletRequest _unused, String _unused2) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
