package no.idporten.eudiw.rp.admin.web.security.ansattporten;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@Profile("selfservice")
@Order(0)
@RequiredArgsConstructor
public class AnsattportenSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        LogoutSuccessHandler logoutHandler,
        AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver,
        AnsattportenOidcUserService ansattportenOidcUserService
    ) throws Exception {
        PathPatternRequestMatcher logoutPostMatcher =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/logout");

        return
            http
                .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .authorizationEndpoint(
                        endpoint -> endpoint.authorizationRequestResolver(
                            ansattportenAuthorizationRequestResolver))
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                    .userInfoEndpoint(endp -> endp.oidcUserService(ansattportenOidcUserService))
                    .loginPage("/login")
                )
                .logout(logout -> logout
                    .logoutRequestMatcher(logoutPostMatcher)
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
                )
                .build();
    }

    @Bean
    public AnsattportenAuthorizationRequestResolver ansattportenAuthorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository,
        AnsattportenAuthzConfig ansattportenAuthzConfig) {
        return new AnsattportenAuthorizationRequestResolver(
            clientRegistrationRepository, ansattportenAuthzConfig);
    }

    @Bean
    public AnsattportenOidcUserService ansattportenOidcUserService() {
        return new AnsattportenOidcUserService();
    }
}
