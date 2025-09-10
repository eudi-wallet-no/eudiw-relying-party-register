package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EntraProperties entraProperties;

    private String toAuthority(String groupId) {
        return "GROUP_" + groupId;
    }

    private String[] requiredAuthorities() {
        return Stream.of(entraProperties.readAccess(), entraProperties.writeAccess())
            .filter(Objects::nonNull)
            .map(this::toAuthority)
            .toArray(String[]::new);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (entraProperties.enabled()) {
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/error",
                        "/access-denied",
                        "/health",
                        "/info",
                        "/prometheus",
                        "/version",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/favicon.ico"
                    ).permitAll()
                    .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                    .anyRequest().hasAnyAuthority(requiredAuthorities())
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                );
        } else {
            http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            List<String> groups = Optional.ofNullable(oidcUser.getClaimAsStringList("groups"))
                .orElseGet(List::of);

            Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
            for (String g : groups) {
                mapped.add(new SimpleGrantedAuthority(toAuthority(g)));
            }

            String name =
                Optional.ofNullable(oidcUser.getClaimAsString("name"))
                    .orElse(oidcUser.getClaimAsString("preferred_username"));

            var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                requestAttributes.getRequest().getSession(true).setAttribute("USER_DISPLAY_NAME", name);
            }

            boolean inAllowedGroup = groups.stream()
                .anyMatch(List.of(
                    entraProperties.readAccess(),
                    entraProperties.writeAccess()
                )::contains);

            if (!inAllowedGroup) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"), "Du er ikke i riktig access group");
            }

            return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
        };
    }

}