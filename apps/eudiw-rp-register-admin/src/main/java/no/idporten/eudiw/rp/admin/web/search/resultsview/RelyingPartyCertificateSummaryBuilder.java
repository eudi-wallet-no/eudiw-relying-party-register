package no.idporten.eudiw.rp.admin.web.search.resultsview;

import no.idporten.eudiw.rp.admin.service.config.RelyingPartiesServiceProperties;
import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.util.Map;

@Component
public class RelyingPartyCertificateSummaryBuilder {
    private final int daysRemainingWarning;

    public RelyingPartyCertificateSummaryBuilder(
            RelyingPartiesServiceProperties relyingPartiesServiceProperties
    ) {
        this.daysRemainingWarning = relyingPartiesServiceProperties.certificateConfig().daysRemainingWarning();
    }


    protected int getDaysRemainingWarning() {
        return daysRemainingWarning;
    }

    public RelyingPartyCertificateSummary build(RelyingPartyCertificateResource relyingPartyResource) {
        long validFromMs =  relyingPartyResource.certificate().getNotBefore().toInstant().toEpochMilli();
        long validToMs =  relyingPartyResource.certificate().getNotAfter().toInstant().toEpochMilli();
        int revocationStatus;
        if (relyingPartyResource.revocationStatus() == null) {
            revocationStatus = -1;
        } else {
            revocationStatus = relyingPartyResource.revocationStatus();
        }
        return new RelyingPartyCertificateSummary(
                relyingPartyResource.entitlement(),
                relyingPartyResource.certificate().getSerialNumber(),
                formatX500PrincipalName(relyingPartyResource.certificate().getSubjectX500Principal()),
                formatX500PrincipalName(relyingPartyResource.certificate().getIssuerX500Principal()),
                validFromMs,
                validToMs,
                relyingPartyResource.id(),
                revocationStatus,
                getStatus(revocationStatus, validFromMs, validToMs)
        );
    }

    protected RelyingPartyCertificateSummary.StatusDisplayData getStatus(int revocationStatus, long validFromMs, long validUntilMs) {
        if (revocationStatus >= 0) {
            return new RelyingPartyCertificateSummary.StatusDisplayData("Revokert", "danger", false);
        }

        long currentTime = currentTimeMillis();

        if (currentTime > validUntilMs) {
            return new RelyingPartyCertificateSummary.StatusDisplayData("Utgått", "danger", false);
        }
        if (currentTime < validFromMs) {
            return new RelyingPartyCertificateSummary.StatusDisplayData("Ikke gyldig ennå", "warning", true);
        }

        long timeRemainingMs = validUntilMs - currentTime;
        long days = timeRemainingMs / (1000 * 60 * 60 * 24);
        long hours = timeRemainingMs / (1000 * 60 * 60);
        long minutes = timeRemainingMs / (1000 * 60);

        String text;
        String style;
        if (days >= getDaysRemainingWarning()) {
            text = "Gyldig";
            style = "success";
        }
        else if (days > 0) {
            text = "Gyldig i " + days + " dager";
            style = "warning";
        }
        else if (hours > 0) {
            text = "Gyldig i " + hours + " timer";
            style = "warning";
        }
        else {
            text = "Gyldig i " + minutes + " minutter";
            style = "warning";
        }

        return new RelyingPartyCertificateSummary.StatusDisplayData(text, style, true);
    }


    private String formatX500PrincipalName(X500Principal x500Principal) {
        return x500Principal.getName(
                X500Principal.RFC1779,
                Map.of("2.5.4.97", "organizationIdentifier"));
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
