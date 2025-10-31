package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnBooleanProperty(value = "eudiw-admin-web.security.enabled", havingValue = false)
public class DevSecurityConfig {
    @Bean
    public UserAuthorityService userAuthorityService() {
        return new UserAuthorityService() {
            @Override
            public boolean userHasAdminAuthority() { return true; }
            @Override
            public boolean userHasAccessTo(String orgno) { return true; }
            @Override
            public void assertUserHasAccessTo(String orgno) { }
            @Override
            public ReporteeAuthority getReporteeAuthority() {
                return new ReporteeAuthority("123123123", "Test-org", true);
            }
        };
    }

    @Bean
    public SecurityFilterChain dummyPermitAllFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                   .csrf(AbstractHttpConfigurer::disable)
                   .build();
    }
}
