package no.idporten.eudiw.rp.admin.web.security;

import no.idporten.eudiw.rp.admin.web.security.exception.InsufficientAuthorityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAuthorityServiceTests {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Should permit administrator actions only with administrator authority")
    void shouldPermitAdministratorActionsOnlyWithAdministratorAuthority(boolean administrator) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("user", "unused",
                List.of(new SimpleGrantedAuthority(administrator ? "ROLE_ADMIN" : "ROLE_USER"))));
        var service = new UserAuthorityService();

        if (administrator) {
            assertDoesNotThrow(service::assertUserHasAdminAuthority);
        } else {
            assertThrows(InsufficientAuthorityException.class, service::assertUserHasAdminAuthority);
        }
    }
}
