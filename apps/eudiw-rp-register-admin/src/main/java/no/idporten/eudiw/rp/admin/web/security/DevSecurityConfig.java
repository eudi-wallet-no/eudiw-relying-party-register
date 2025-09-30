package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Profile("dev | local-test")
public class DevSecurityConfig {
    @Bean
    @Primary
    public AuthorizationService authorizationService() {
        return new AuthorizationService(null) {
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
