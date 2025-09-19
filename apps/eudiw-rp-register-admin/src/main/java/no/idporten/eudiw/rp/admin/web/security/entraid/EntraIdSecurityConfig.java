package no.idporten.eudiw.rp.admin.web.security.entraid;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class EntraIdSecurityConfig {

    private final EntraIdProperties entraIdProperties;
    private final EntraIdOidcUserService entraIdOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectProvider<ClientRegistrationRepository> clientsProvider) throws Exception {
        if (entraIdProperties.enabled()) {
            PathPatternRequestMatcher.Builder paths = withDefaults();
            var logoutPost = paths.matcher(HttpMethod.POST, "/logout");
            var logoutGet  = paths.matcher(HttpMethod.GET,  "/logout");

            ClientRegistrationRepository clients = clientsProvider.getIfAvailable();
            if (clients == null) {
                throw new IllegalStateException(
                    "ClientRegistrationRepository er null, ikke start"
                );
            }

            LogoutSuccessHandler logoutHandler = buildOidcLogoutSuccessHandler(clients);

            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
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
                    ).permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .anyRequest().hasAnyAuthority(EntraIdOidcUserService.toAuthority(entraIdProperties.writeAccess()))
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(entraIdOidcUserService))
                ).logout(logout -> logout
                    .logoutRequestMatcher(new OrRequestMatcher(logoutPost, logoutGet))
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(logoutGet));
        } else {
            http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }


    private LogoutSuccessHandler buildOidcLogoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler oidc = new OidcClientInitiatedLogoutSuccessHandler(clients);
        oidc.setPostLogoutRedirectUri("{baseUrl}");

        return (request, response, authentication) -> {
            boolean isFrontChannel =
                "GET".equalsIgnoreCase(request.getMethod()) && (request.getParameter("iss") != null || request.getParameter("sid") != null);

            if (isFrontChannel) {
                response.setStatus(200);
            } else {
                oidc.onLogoutSuccess(request, response, authentication);
            }
        };
    }
}
