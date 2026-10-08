package no.eudiw.rp.register.service;

import no.eudiw.rp.register.testdata.EntityGenerator;
import no.eudiw.rp.register.domain.WalletRelyingParty;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.BaseCertificateEntity;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.ErrorResponseException;
import no.eudiw.rp.register.integrations.certificateservice.CertificateServiceClient;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.WalletRelyingPartyRepository;
import no.eudiw.rp.register.testdata.CertificatesGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("junit")
class CertificateRevocationTransactionTest {

    @Autowired
    private RelyingPartyCertificateService service;

    @Autowired
    private WalletRelyingPartyRepository walletRelyingParties;

    @MockitoSpyBean
    private AccessCertificateRepository accessCertificates;

    @MockitoSpyBean
    private IssuerCertificateRepository issuerCertificates;

    @MockitoBean
    private CertificateServiceClient client;

    private RelyingPartyInstance relyingParty;
    private AccessCertificate accessCertificate;
    private IssuerCertificate issuerCertificate;

    @BeforeEach
    void setUp() {
        walletRelyingParties.deleteAll();
        var entitlement = new RelyingPartyEntitlement("test-entitlement");
        relyingParty = EntityGenerator.generateRelyingPartyInstance("Trade name", List.of(entitlement), List.of(), List.of());
        accessCertificate = new AccessCertificate("access-ca", CertificatesGenerator.generateX509Certificate(), relyingParty);
        issuerCertificate = new IssuerCertificate(CertificatesGenerator.generateX509Certificate(), "issuer-ca", entitlement);
        relyingParty.setAccessCertificates(List.of(accessCertificate));
        entitlement.addIssuerCertificate(issuerCertificate);
        walletRelyingParties.saveAndFlush(new WalletRelyingParty(
            "Entity", "123456789", false, List.of(relyingParty.getWalletRelyingPartyService())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void persistsRevocationAfterCaConfirms(boolean issuer) {
        when(client.revokeCertificate(anyString(), anyInt(), anyString())).thenReturn(HttpStatus.NO_CONTENT);

        revoke(issuer);

        assertEquals(0, storedStatus(issuer));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unexpectedCaSuccessDoesNotRevokeLocally(boolean issuer) {
        when(client.revokeCertificate(anyString(), anyInt(), anyString())).thenReturn(HttpStatus.OK);

        assertThrows(BadRequestException.class, () -> revoke(issuer));

        assertEquals(-1, storedStatus(issuer));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void caRejectionDoesNotRevokeLocally(boolean issuer) {
        when(client.revokeCertificate(anyString(), anyInt(), anyString()))
            .thenThrow(new ErrorResponseException("CA rejected request"));

        assertThrows(ErrorResponseException.class, () -> revoke(issuer));

        assertEquals(-1, storedStatus(issuer));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void caTimeoutDoesNotRevokeLocally(boolean issuer) {
        when(client.revokeCertificate(anyString(), anyInt(), anyString()))
            .thenThrow(new ResourceAccessException("Read timed out"));

        assertThrows(ResourceAccessException.class, () -> revoke(issuer));

        assertEquals(-1, storedStatus(issuer));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void databaseFailureRollsBackLocalRevocationAfterCaConfirmed(boolean issuer) {
        when(client.revokeCertificate(anyString(), anyInt(), anyString())).thenReturn(HttpStatus.NO_CONTENT);
        if (issuer) {
            doThrow(new DataIntegrityViolationException("Failed to store revocation"))
                .when(issuerCertificates).saveAndFlush(any(IssuerCertificate.class));
        } else {
            doThrow(new DataIntegrityViolationException("Failed to store revocation"))
                .when(accessCertificates).saveAndFlush(any(AccessCertificate.class));
        }

        assertThrows(DataIntegrityViolationException.class, () -> revoke(issuer));

        assertEquals(-1, storedStatus(issuer));
        BaseCertificateEntity certificate = accessCertificate;
        if (issuer) {
            certificate = issuerCertificate;
        }
        verify(client).revokeCertificate(certificate.getSerialNo(), 0, certificate.getCaId());
    }

    private void revoke(boolean issuer) {
        if (issuer) {
            service.revokeIssuerCertificate(issuerCertificate.getId(), relyingParty.getId());
        } else {
            service.revokeAccessCertificate(accessCertificate.getId(), relyingParty.getId());
        }
    }

    private int storedStatus(boolean issuer) {
        if (issuer) {
            return issuerCertificates.findById(issuerCertificate.getId()).orElseThrow().getRevocationStatus();
        }
        return accessCertificates.findById(accessCertificate.getId()).orElseThrow().getRevocationStatus();
    }
}
