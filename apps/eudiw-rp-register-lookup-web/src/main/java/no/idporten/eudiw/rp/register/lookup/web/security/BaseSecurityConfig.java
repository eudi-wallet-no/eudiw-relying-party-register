package no.idporten.eudiw.rp.register.lookup.web.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnBooleanProperty(value = "eudiw-rp-register-lookup-web.security.enabled", matchIfMissing = true)
public class BaseSecurityConfig {

    @Bean
    public SecurityFilterChain baseFilterChain(HttpSecurity http, CspProperties csp) {
        CspHeaders.configure(http, csp);
        return http.build();
    }

}
