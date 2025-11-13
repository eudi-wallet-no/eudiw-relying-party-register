package no.idporten.eudiw.rp.admin.web.security.dev;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.rp.admin.service.syntheticreportees.SyntheticReporteeProvider;
import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class DevAuthenticationProvider implements AuthenticationProvider {

    private final SyntheticReporteeProvider syntheticReporteeProvider;

    @Override
    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {

        Set<GrantedAuthority> authorities = new HashSet<>(authentication.getAuthorities());
        String name = authentication.getName();

        if (name.equalsIgnoreCase("admin")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        else {
            ReporteeAuthority reportee =
                syntheticReporteeProvider.getSyntheticReporteeAuthority(name);
            authorities.add(reportee);
            name = "%s - %s".formatted(name, reportee.name());
        }

        String authName = name;
        AuthenticatedPrincipal principal = () -> authName;
        return new UsernamePasswordAuthenticationToken(
            principal, authentication.getCredentials(), authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
