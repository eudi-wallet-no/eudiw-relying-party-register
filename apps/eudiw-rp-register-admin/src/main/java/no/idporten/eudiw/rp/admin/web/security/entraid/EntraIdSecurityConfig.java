package no.idporten.eudiw.rp.admin.web.security.entraid;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@Profile("admin")
@Order(0)
@RequiredArgsConstructor
public class EntraIdSecurityConfig {

    private final EntraIdProperties entraIdProperties;
    private final EntraIdOidcUserService entraIdOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        LogoutSuccessHandler logoutHandler) throws Exception {

        if (entraIdProperties.enabled()) {
            PathPatternRequestMatcher.Builder paths = withDefaults();
            var logoutPost = paths.matcher(HttpMethod.POST, "/logout");
            var logoutGet  = paths.matcher(HttpMethod.GET,  "/logout");

            http
                .authorizeHttpRequests(auth -> auth
                    .anyRequest()
                    .hasAnyAuthority(EntraIdOidcUserService.toAuthority(entraIdProperties.writeAccess()))
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(entraIdOidcUserService))
                )
                .logout(logout -> logout
                    .logoutRequestMatcher(new OrRequestMatcher(logoutPost, logoutGet))
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(logoutGet));
        }
        return http.build();
    }
}
