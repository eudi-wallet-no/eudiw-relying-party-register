package no.idporten.eudiw.ca.security;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Filter for API key authentication.  Checks API key header for included requests.
 */
public class APIKeyAuthenticationFilter extends OncePerRequestFilter {

    public final static String API_KEY_HEADER_NAME = "X-API-KEY";

    private final ApiKeySecurityProperties apiKeySecurityProperties;
    private final List<PathPatternRequestMatcher> excludeMatchers;

    protected APIKeyAuthenticationFilter(ApiKeySecurityProperties apiKeySecurityProperties, String... excludePaths) {
        super();
        this.apiKeySecurityProperties = apiKeySecurityProperties;
        this.excludeMatchers = createMatchers(excludePaths);
    }

    private List<PathPatternRequestMatcher> createMatchers(String... paths) {
        return Stream.of(paths).map(path -> PathPatternRequestMatcher.withDefaults().matcher(path)).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            if (excludeMatchers.stream().anyMatch(matcher -> matcher.matches(request))) {
                filterChain.doFilter(request, response);
                return;
            }
            final String apiKey = request.getHeader(API_KEY_HEADER_NAME);
            if (StringUtils.isEmpty(apiKey)) {
                throw new APIKeyAuthenticationException("Missing API key");
            }
            if (!Objects.equals(apiKey, apiKeySecurityProperties.apiKey())) {
                throw new APIKeyAuthenticationException("Invalid API key");
            }
            Authentication authentication = new APIKeyAuthenticationToken(apiKey, AuthorityUtils.NO_AUTHORITIES);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
    }

}
