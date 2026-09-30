package no.idporten.eudiw.rp.admin.web.security.dev;

import no.idporten.eudiw.rp.admin.service.syntheticreportees.StatelessPersistentSyntheticReporteeService;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.web.security.UserAuthorityService;
import no.idporten.eudiw.rp.admin.web.security.CspHeaders;
import no.idporten.eudiw.rp.admin.web.security.CspProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnBooleanProperty(value = "eudiw-admin-web.security.enabled", havingValue = false)
public class DevSecurityConfig {

    private static final String[] AUTHZ_ALLOWLIST = {
        "/login",
        "/error",
        "/access-denied",
        "/css/**",
        "/images/**",
        "/favicon.ico",
        "/inter/**"
    };

    @Bean
    public SecurityFilterChain devSecurityFilterChain(
        HttpSecurity http,
        CspProperties csp,
        AuthenticationProvider devAuthenticationProvider)
        throws Exception {
        CspHeaders.configure(http, csp);
        return http
                   .authorizeHttpRequests(authz -> authz
                       .requestMatchers(AUTHZ_ALLOWLIST)
                       .permitAll()
                       .anyRequest().authenticated()
                   )
                   .authenticationProvider(devAuthenticationProvider)
                   .formLogin(form -> form.loginPage("/login"))
                   .csrf(AbstractHttpConfigurer::disable)
                   .build();
    }

    @Bean
    public SyntheticReporteeProvider syntheticReporteeProvider() {
        return new StatelessPersistentSyntheticReporteeService();
    }

    @Bean
    public AuthenticationProvider devAuthenticationProvider(
        SyntheticReporteeProvider syntheticReporteeProvider) {
        return new DevAuthenticationProvider(syntheticReporteeProvider);
    }

    @Bean
    public UserAuthorityService userAuthorityService() {
        return new UserAuthorityService();
    }
}
