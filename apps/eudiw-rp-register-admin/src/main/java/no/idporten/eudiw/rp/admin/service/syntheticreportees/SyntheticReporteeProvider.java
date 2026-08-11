package no.idporten.eudiw.rp.admin.service.syntheticreportees;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.AuthorizedPartyAuthority;

public interface SyntheticReporteeProvider {
    AuthorizedPartyAuthority getSyntheticReporteeAuthority(String id);
}
