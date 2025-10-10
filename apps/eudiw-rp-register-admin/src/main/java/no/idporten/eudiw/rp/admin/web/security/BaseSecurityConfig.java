package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.service.RelyingPartiesService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnBooleanProperty("eudiw-admin-web.enable-security")
public class BaseSecurityConfig {

    private static final String[] SECURITY_IGNORE_LIST = {
        "/login",
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
        "/favicon.ico",
        "/inter/**"
    };

    @Bean
    public WebSecurityCustomizer ignoringCustomizer() {
        return (web) -> web.ignoring()
                           .requestMatchers(SECURITY_IGNORE_LIST)
                           .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Bean
    public SecurityFilterChain baseFilterChain(
        HttpSecurity http,
        Customizer<OAuth2LoginConfigurer<HttpSecurity>> myOauth2LoginConfigurer,
        LogoutSuccessHandler logoutHandler)
        throws Exception {
        return
            http
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated()
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                )
                .oauth2Login(myOauth2LoginConfigurer)
                .logout(logout -> logout
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
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
    public PermissionsService permissionsService(RelyingPartiesService relyingPartiesService) {
        return new PermissionsService(relyingPartiesService);
    }

    @Bean
    public List<ClientRegistration> availableClients(InMemoryClientRegistrationRepository clientRepo) {
        List<ClientRegistration> availableClients = new ArrayList<>();
        clientRepo.iterator().forEachRemaining(availableClients::add);
        return availableClients;
    }
}
