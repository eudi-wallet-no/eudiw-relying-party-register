package no.idporten.eudiw.rp.admin.web.security.oidcusers;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public class SelfServiceOidcUser extends BaseOidcUser {

    private final List<SelfServiceReportee> reportees;

    public SelfServiceOidcUser(Collection<? extends GrantedAuthority> authorities,
                               OidcIdToken idToken,
                               OidcUserInfo userInfo,
                               List<SelfServiceReportee> reportees) {
        super(authorities, idToken, userInfo);
        this.reportees = reportees;
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
    @Override
    public boolean hasPrivilegedAccessTo(String orgno) {
        return reportees.stream()
                        .anyMatch(reportee -> Objects.equals(reportee.orgno(), orgno));
    }
}
