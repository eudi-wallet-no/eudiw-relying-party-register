package no.idporten.eudiw.rp.admin.web.search.resultsview;

import no.idporten.eudiw.rp.admin.web.resource.certificates.RelyingPartyCertificateResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("When getting status from relying party certificate")
@ExtendWith(MockitoExtension.class)
public class RelyingPartyCertificateSummaryTests {
    private RelyingPartyCertificateSummaryBuilder  relyingPartyCertificateSummaryBuilder;
    private RelyingPartyCertificateResource relyingPartyResource;
    private X509Certificate certificate;
    private long dateNow;
    X500Principal principal = new X500Principal("CN=Test User, OU=Engineering, O=Test Corp, C=US");

    @BeforeEach
    public void setup() {
        int daysRemainingWarning = 30;
        relyingPartyCertificateSummaryBuilder = new RelyingPartyCertificateSummaryBuilder(daysRemainingWarning);
        dateNow = System.currentTimeMillis();

        relyingPartyResource = mock(RelyingPartyCertificateResource.class);
        certificate = mock(X509Certificate.class);

        when(relyingPartyResource.id()).thenReturn(UUID.randomUUID());
        when(relyingPartyResource.certificate()).thenReturn(certificate);
        when(relyingPartyResource.entitlement()).thenReturn("Entitlement");
        when(certificate.getNotBefore()).thenReturn(new Date(dateNow - 1000000));
        when(certificate.getSubjectX500Principal()).thenReturn(principal);
        when(certificate.getIssuerX500Principal()).thenReturn(principal);
    }

    @Test
    @DisplayName("then is should display danger color and expired text for revocationStatus -1")
    public void shouldBeSuccess() {
        when(certificate.getNotAfter()).thenReturn(new Date(dateNow - 1));
        when( relyingPartyResource.revocationStatus()).thenReturn(-1);

        var result = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);

        var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Utgått",  "danger");
        assertEquals(expected, result.status());
    }


    @Test
    @DisplayName("then is should display danger color and revoked text for revocationStatus 0 or higher")
    public void shouldBeError() {
        when(certificate.getNotAfter()).thenReturn(new Date(dateNow + 1000000));
        when( relyingPartyResource.revocationStatus()).thenReturn(0);

        var result1 = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);

        when( relyingPartyResource.revocationStatus()).thenReturn(1);
        var result2 = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);

        var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Revokert",  "danger");

        assertEquals(expected, result1.status());
        assertEquals(expected, result2.status());
    }

    @Nested
    @DisplayName("when expiration date is close")
    public class WhenAlmostOutOfTime {
        @Test
        @DisplayName("then is should display warning color and x days remaining text for revocationStatus -1")
        public void shouldBeDaysLeft() {
            when(certificate.getNotAfter()).thenReturn(new Date(dateNow + 30L * 24 * 60 * 60 * 1000));
            when( relyingPartyResource.revocationStatus()).thenReturn(-1);

            var result = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);


            var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Gyldig i 29 dager",  "warning");

            assertEquals(expected, result.status());
        }

        @Test
        @DisplayName("then is should display warning color and x hours remaining text for revocationStatus -1")
        public void shouldBeHoursLeft() {
            when(certificate.getNotAfter()).thenReturn(new Date(dateNow + 24 * 60 * 60 * 1000));
            when( relyingPartyResource.revocationStatus()).thenReturn(-1);

            var result = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);


            var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Gyldig i 23 timer",  "warning");

            assertEquals(expected, result.status());
        }

        @Test
        @DisplayName("then is should display warning color and x minuts remaining text for revocationStatus -1")
        public void shouldBeMinutesLeft() {
            when(certificate.getNotAfter()).thenReturn(new Date(dateNow + 60 * 60 * 1000));
            when( relyingPartyResource.revocationStatus()).thenReturn(-1);

            var result = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);


            var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Gyldig i 59 minutter",  "warning");

            assertEquals(expected, result.status());
        }
    }


    @Test
    @DisplayName("then is should display success color and valid text for revocationStatus -1")
    public void shouldBeOutOfTime() {
        when(certificate.getNotAfter()).thenReturn(new Date(dateNow + 31L * 24 * 60 * 60 * 1000));
        when( relyingPartyResource.revocationStatus()).thenReturn(-1);

        var result = relyingPartyCertificateSummaryBuilder.build(relyingPartyResource);


        var expected = new RelyingPartyCertificateSummary.StatusDisplayData("Gyldig",  "success");

        assertEquals(expected, result.status());
    }
}
