package no.idporten.eudiw.rp.admin.web.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Stream;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EntraProperties entraProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectProvider<ClientRegistrationRepository> clientsProvider) throws Exception {
        if (entraProperties.enabled()) {
            PathPatternRequestMatcher.Builder paths = withDefaults();
            var logoutPost = paths.matcher(HttpMethod.POST, "/logout");
            var logoutGet  = paths.matcher(HttpMethod.GET,  "/logout");

            ClientRegistrationRepository clients = clientsProvider.getIfAvailable();
            if (clients == null) {
                throw new IllegalStateException(
                    "ClientRegistrationRepository er null, ikke start"
                );
            }

            LogoutSuccessHandler logoutHandler = buildOidcLogoutSuccessHandler(clients);

            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/",
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
                    .anyRequest().hasAnyAuthority(toAuthority(entraProperties.writeAccess()))
                )
                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
                .oauth2Login(oauth -> oauth
                    .failureHandler(new SimpleUrlAuthenticationFailureHandler("/access-denied"))
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                ).logout(logout -> logout
                    .logoutRequestMatcher(new OrRequestMatcher(logoutPost, logoutGet))
                    .clearAuthentication(true)
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(logoutHandler)
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(logoutGet));
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
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            List<String> groups = Optional.ofNullable(oidcUser.getClaimAsStringList("groups"))
                .orElseGet(List::of);

            String name =
                Optional.ofNullable(oidcUser.getClaimAsString("name"))
                    .orElse(oidcUser.getClaimAsString("preferred_username"));

            var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                requestAttributes.getRequest().getSession(true).setAttribute("USER_DISPLAY_NAME", name);
            }

            boolean hasAccess = groups.contains(entraProperties.writeAccess());
            if (!hasAccess) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"), "Du er ikke i riktig access group");
            }

            Set<GrantedAuthority> mapped = new HashSet<>(oidcUser.getAuthorities());
            mapped.add(new SimpleGrantedAuthority(toAuthority(entraProperties.writeAccess())));

            return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
        };
    }

    private LogoutSuccessHandler buildOidcLogoutSuccessHandler(ClientRegistrationRepository clients) {
        OidcClientInitiatedLogoutSuccessHandler oidc = new OidcClientInitiatedLogoutSuccessHandler(clients);
        oidc.setPostLogoutRedirectUri("{baseUrl}");

        return (request, response, authentication) -> {
            boolean isFrontChannel =
                "GET".equalsIgnoreCase(request.getMethod()) && (request.getParameter("iss") != null || request.getParameter("sid") != null);

            if (isFrontChannel) {
                response.setStatus(200);
            } else {
                oidc.onLogoutSuccess(request, response, authentication);
            }
        };
    }

    private String toAuthority(String groupId) {
        return "GROUP_" + groupId;
    }
}