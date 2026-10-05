package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.exception.AdminServiceException;
import no.idporten.eudiw.rp.admin.service.enhetsregisteretservice.EnhetsregisteretService;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.StatelessPersistentSyntheticReporteeService;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenAuthorizationRequestResolver;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.AnsattportenProperties;
import no.idporten.eudiw.rp.admin.web.security.ansattporten.authzdetails.AuthorizationDetailsMapper;
import no.idporten.eudiw.rp.admin.web.security.entraid.EntraIdProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnBooleanProperty(value = "eudiw-admin-web.security.enabled", matchIfMissing = true)
public class BaseSecurityConfig {

    private static final String[] AUTHZ_ALLOWLIST = {
        "/login",
        "/error",
        "/access-denied",
        "/health",
        "/health/liveness",
        "/health/readiness",
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

    @Value("${server.servlet.session.cookie.name}")
    private String sessionCookieName;

    @Bean
    public SecurityFilterChain baseFilterChain(
        HttpSecurity http,
        CspProperties csp,
        AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver,
        LogoutSuccessHandler logoutHandler)
        throws Exception {
        PathPatternRequestMatcher.Builder matcherBuilder =
            PathPatternRequestMatcher.withDefaults();
        RequestMatcher logoutMatcher = new OrRequestMatcher(
            matcherBuilder.matcher(HttpMethod.GET, "/logout"),
            matcherBuilder.matcher(HttpMethod.POST, "/logout")
        );
        CspHeaders.configure(http, csp);
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
                .oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .failureHandler(new CustomAuthenticationFailureHandler())
                    .authorizationEndpoint(
                        endpoint -> endpoint.authorizationRequestResolver(
                            ansattportenAuthorizationRequestResolver))
                )
                .logout(logout -> logout
                    .logoutRequestMatcher(logoutMatcher)
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies(sessionCookieName)
                    .logoutSuccessHandler(logoutHandler)
                )
                .build();
    }

    @Bean
    public CustomOidcUserService customOidcUserService(
        EntraIdProperties entraIdProperties,
        AnsattportenProperties ansattportenProperties,
        EnhetsregisteretService enhetsregisteretService,
        SyntheticReporteeProvider syntheticReporteeProvider,
        AuthorizationDetailsMapper authorizationDetailsMapper) {
        return new CustomOidcUserService(
            entraIdProperties,
            ansattportenProperties,
            enhetsregisteretService,
            syntheticReporteeProvider,
            authorizationDetailsMapper);
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
    public DefaultOAuth2AuthorizationRequestResolver defaultOAuth2AuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository) {
        return new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");
    }

    @Bean
    public AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver(
        OAuth2AuthorizationRequestResolver delegateAuthzRequestResolver,
        AnsattportenProperties ansattportenProperties,
        JsonMapper jsonMapper) {
        return new AnsattportenAuthorizationRequestResolver(
            delegateAuthzRequestResolver, ansattportenProperties, jsonMapper);
    }

    @Bean
    public UserAuthorityService userAuthorityService() {
        return new UserAuthorityService();
    }

    @Bean
    @Profile("!prod")
    public SyntheticReporteeProvider syntheticReporteeService() {
        return new StatelessPersistentSyntheticReporteeService();
    }

    @Bean
    @Profile("prod")
    public SyntheticReporteeProvider explodingSyntheticReporteeProvider() {
        return _ -> {
            throw new AdminServiceException(
                "Unexpected request for synthetic reportee in environment "
                    + "where synthetic reportees never allowed!");
        };
    }

}
