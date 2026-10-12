package no.idporten.eudiw.rp.register.lookup.web.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@ConditionalOnBooleanProperty(value = "eudiw-rp-register-lookup-web.security.enabled", matchIfMissing = true)
public class BaseSecurityConfig {

    @Bean
    public SecurityFilterChain baseFilterChain(HttpSecurity http, CspProperties csp) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        configureCspHeader(http, csp);
        return http.build();
    }

    private static void configureCspHeader(HttpSecurity http, CspProperties csp) {
        http.headers(headers -> headers.contentSecurityPolicy(policy -> {
            policy.policyDirectives(csp.policy());
            if (csp.mode() == CspProperties.Mode.REPORT_ONLY) {
                policy.reportOnly();
            }
        }));
    }

}
