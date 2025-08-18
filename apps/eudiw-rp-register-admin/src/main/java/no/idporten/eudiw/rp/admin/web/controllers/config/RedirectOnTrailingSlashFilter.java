package no.idporten.eudiw.rp.admin.web.controllers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.UrlHandlerFilter;

import java.io.IOException;

@Component
public class RedirectOnTrailingSlashFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        UrlHandlerFilter redirectOnTrailingSlashFilter =
            UrlHandlerFilter.trailingSlashHandler("/**")
                            .redirect(HttpStatus.MOVED_PERMANENTLY)
                            .build();
        redirectOnTrailingSlashFilter.doFilter(request, response, filterChain);
    }
}
