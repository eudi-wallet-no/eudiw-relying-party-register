package no.eudiw.rp.register.security;

import no.eudiw.rp.register.exception.AppExceptionFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ApiKeySecurityConfig {

    private final ApiKeySecurityProperties apiKeySecurityProperties;
    private final AppExceptionFilter appExceptionFilter;

    @Autowired
    public ApiKeySecurityConfig(ApiKeySecurityProperties properties,
                                AppExceptionFilter appExceptionFilter) {
        this.apiKeySecurityProperties = properties;
        this.appExceptionFilter = appExceptionFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        APIKeyAuthenticationFilter apiKeyAuthenticationFilter =
                new APIKeyAuthenticationFilter(apiKeySecurityProperties,
                        apiKeySecurityProperties.excludePaths());
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(apiKeySecurityProperties.includePaths()).authenticated()
                                .anyRequest().permitAll())
                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(appExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}