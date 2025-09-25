package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(-1)
@RequiredArgsConstructor
@Profile("!(dev | local-test)")
public class BaseSecurityConfig {

    private static final String[] UNAUTHENTICATED_ALLOWLIST = {
        "/",
        "/error",
        "/access-denied",
        "/health",
        "/info",
        "/prometheus",
        "/version",
        "/css/**",
        "/js/**",
        "/images/**",
        "/webjars/**",
        "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain baseFilterChain(HttpSecurity http) throws Exception {
        return
            http
                .securityMatcher(UNAUTHENTICATED_ALLOWLIST)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(UNAUTHENTICATED_ALLOWLIST)
                    .permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                )
                .build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler logoutHandler =
            new OidcClientInitiatedLogoutSuccessHandler(clients);
        logoutHandler.setPostLogoutRedirectUri("{baseUrl}");

        return (request, response, authentication) -> {
            boolean isFrontChannel =
                "GET".equalsIgnoreCase(request.getMethod()) && (request.getParameter("iss") != null || request.getParameter("sid") != null);

            if (isFrontChannel) {
                response.setStatus(200);
            } else {
                logoutHandler.onLogoutSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public AuthorizationService authorizationService(RelyingPartiesService relyingPartiesService) {
        return new AuthorizationService(relyingPartiesService);
    }
}
