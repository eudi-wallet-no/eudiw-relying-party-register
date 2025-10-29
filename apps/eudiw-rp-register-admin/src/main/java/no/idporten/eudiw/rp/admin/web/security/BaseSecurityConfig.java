package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthzConfig;
import no.idporten.eudiw.rp.admin.web.security.entraid.EntraIdProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnBooleanProperty("eudiw-admin-web.enable-security")
public class BaseSecurityConfig {

    private static final String[] AUTHZ_ALLOWLIST = {
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
    public SecurityFilterChain baseFilterChain(
        HttpSecurity http,
        AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver,
        LogoutSuccessHandler logoutHandler)
        throws Exception {
        PathPatternRequestMatcher.Builder matcherBuilder =
            PathPatternRequestMatcher.withDefaults();
        RequestMatcher logoutMatcher = new OrRequestMatcher(
            matcherBuilder.matcher(HttpMethod.GET, "/logout"),
            matcherBuilder.matcher(HttpMethod.POST, "/logout")
        );
        return
            http
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers(AUTHZ_ALLOWLIST)
                    .permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                    .permitAll()
                )
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
                    .authorizationEndpoint(
                        endpoint -> endpoint.authorizationRequestResolver(
                            ansattportenAuthorizationRequestResolver))
                )
                .logout(logout -> logout
                    .logoutRequestMatcher(logoutMatcher)
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
                )
                .build();
    }

    @Bean
    public CustomOidcUserService customOidcUserService(
        EntraIdProperties entraIdProperties,
        EnhetsregisteretService enhetsregisteretService) {
        return new CustomOidcUserService(entraIdProperties, enhetsregisteretService);
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
    public AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        AnsattportenAuthzConfig ansattportenAuthzConfig) {
        return new AnsattportenAuthorizationRequestResolver(
            clientRegistrationRepository, ansattportenAuthzConfig);
    }

    @Bean
    public UserAuthorityService userAuthorityService() {
        return new UserAuthorityService();
    }

    @Bean
    public List<ClientRegistration> availableClients(InMemoryClientRegistrationRepository clientRepo) {
        List<ClientRegistration> availableClients = new ArrayList<>();
        clientRepo.iterator().forEachRemaining(availableClients::add);
        return availableClients;
    }
}
