package no.idporten.eudiw.rp.admin.web.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@ConditionalOnBooleanProperty(value = "eudiw-admin-web.enable-security", havingValue = false)
public class DevSecurityConfig {
    @Bean
    @Primary
    public PermissionsService permissionsService() {
        return new PermissionsService(null) {
            @Override
            public boolean userHasPrivilegedAccessTo(String orgno) {
                return true;
            }
            @Override
            public boolean userHasPrivilegedAccessTo(UUID id) {
                return true;
            }
        };
    }

    @Bean
    public SecurityFilterChain dummyPermitAllFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                   .build();
    }

    @Bean
    public List<ClientRegistration> availableClients() {
        return List.of();
    }
}
