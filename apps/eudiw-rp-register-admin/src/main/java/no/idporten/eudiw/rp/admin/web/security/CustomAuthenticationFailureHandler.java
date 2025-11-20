package no.idporten.eudiw.rp.admin.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.rp.admin.web.security.exception.CustomAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest _unused,
                                        HttpServletResponse response,
                                        AuthenticationException e) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        String redirectErrorParameter;
        if (e instanceof CustomAuthenticationException e2) {
            redirectErrorParameter = e2.getErrorCode();
            log.info("Invalid authentication request (error code={})", redirectErrorParameter, e2);
        }
        else if (e instanceof OAuth2AuthenticationException e2) {
            redirectErrorParameter = e2.getError().getErrorCode();
            log.warn("OAuth2 error (oauth error code={})", redirectErrorParameter, e2);
        }
        else {
            redirectErrorParameter = "500";
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            log.error("Unexpected authentication failure", e);
        }

        response.sendRedirect("/login?error=%s".formatted(redirectErrorParameter));
    }
}
