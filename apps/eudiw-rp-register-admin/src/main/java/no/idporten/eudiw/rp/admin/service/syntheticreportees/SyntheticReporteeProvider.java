package no.idporten.eudiw.rp.admin.service.syntheticreportees;

import no.idporten.eudiw.rp.admin.web.security.oidcusers.ReporteeAuthority;

public interface SyntheticReporteeProvider {
    ReporteeAuthority getSyntheticReporteeAuthority(String id);
}
