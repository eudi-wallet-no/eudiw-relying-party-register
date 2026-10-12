package no.eudiw.rp.register.service;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.domain.certificates.AccessCertificate;
import no.eudiw.rp.register.domain.certificates.IssuerCertificate;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.domain.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.repository.AccessCertificateRepository;
import no.eudiw.rp.register.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.integrations.certificateservice.CertificateServiceClient;
import no.eudiw.rp.register.integrations.certificateservice.config.RelyingPartyCertificateServiceProperties;
import no.eudiw.rp.register.exception.BadRequestException;
import no.eudiw.rp.register.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyCertificateService {

    private static final Logger log = LoggerFactory.getLogger(RelyingPartyCertificateService.class);

    private final RelyingPartyCertificateServiceProperties properties;
    private final RelyingPartyInstanceRepository relyingPartyRepository;
    private final AccessCertificateRepository accessCertificateRepository;
    private final IssuerCertificateRepository issuerCertificateRepository;

    private final EntitlementService entitlementService;

    private final CertificateServiceClient certificateServiceClient;

    @Transactional(readOnly = true)
    public List<AccessCertificate> getCertificatesForRelyingParty(UUID relyingPartyId) {
        return relyingPartyRepository
            .findById(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("Relying party not found"))
            .getAccessCertificates();
    }

    @Transactional(readOnly = true)
    public AccessCertificate getCertificate(UUID certificateId, UUID relyingPartyId) {
        if (!relyingPartyRepository.existsById(relyingPartyId)) {
            throw new NotFoundException("Access certificate holder does not exist");
        }
        return accessCertificateRepository
            .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
            .orElseThrow(() -> new NotFoundException("Certificate does not exist"));
    }

    @Transactional(readOnly = true)
    public IssuerCertificate getIssuerCertificate(UUID certificateId, UUID relyingPartyId) {
        RelyingPartyInstance rp = relyingPartyRepository.findById(relyingPartyId)
                .orElseThrow(() -> new NotFoundException(
                        "Issuer certificate holder does not exist",
                        "Failed to find relying party with uuid=%s for certificateId uuid=%s "
                                .formatted(relyingPartyId, certificateId)));
        IssuerCertificate issuerCert = issuerCertificateRepository.findById(certificateId).orElseThrow(() -> new NotFoundException("Certificate does not exist", "Certificate with uuid %s does not exist in database".formatted(certificateId)));
        List<RelyingPartyEntitlement> relyingPartyEntitlements = rp.getRelyingPartyEntitlements();
        if (relyingPartyEntitlements == null || issuerCert.getEntitlement() == null) {
            throw new NotFoundException("No entitlements found for relying party for issuer certificate or in certificate", "No entitlements found for relying party with uuid %s for certificate uuid %s".formatted(relyingPartyId, certificateId));
        }
        boolean sameEntitlement = relyingPartyEntitlements.stream().anyMatch(e -> e.getEntitlement().equals(issuerCert.getEntitlement().getEntitlement()));
        if (!sameEntitlement) {
            throw new NotFoundException("Requested certificate does not belong to the specified relying party", "CertificateId uuid=%s with entitlement %s does not match entitlement in relying party with uuid=%s for ".formatted(certificateId, issuerCert.getEntitlement().getEntitlement(), relyingPartyId));
        }
        return issuerCert;
    }

    @Transactional(readOnly = true)
    public List<IssuerCertificate> getAllIssuerCertificatesFromRelyingParty(UUID relyingPartyId) {
        return relyingPartyRepository
            .findById(relyingPartyId)
            .orElseThrow(() -> new NotFoundException("Relying party not found"))
            .getIssuerCertificates();
    }

    @Transactional
    public IssuerCertificate requestIssuerCertificate(
            UUID relyingPartyId,
            PKCS10CertificationRequest csr,
            String entitlement) {
        RelyingPartyInstance relyingParty = getRelyingParty(relyingPartyId);

        RelyingPartyEntitlement relyingPartyEntitlement =
                relyingParty.getRelyingPartyEntitlement(entitlement)
                        .orElseThrow(() -> new NotFoundException("RelyingPartyEntitlement does not exist"));

        String caId = entitlementService.getDefaultCaForEntitlementUri(entitlement);

        if (caId == null) {
            throw new RegisterServiceException("Entitlement does not have a CA ID");
        }

        X509Certificate certificate = certificateServiceClient.requestCertificate(
                csr,
                relyingParty.getWalletRelyingPartyService().getWalletRelyingParty().getOrgno(),
                relyingParty.getWalletRelyingPartyService().getWalletRelyingParty().getLegalName(),
                relyingParty.getWalletRelyingPartyService().getServiceTradeName(),
                caId);

        IssuerCertificate certificateEntity =
                new IssuerCertificate(certificate, caId, relyingPartyEntitlement);

        relyingPartyEntitlement.addIssuerCertificate(certificateEntity);

        return issuerCertificateRepository.saveAndFlush(certificateEntity);
    }

    @Transactional
    public AccessCertificate requestAccessCertificateForRelyingParty(
            UUID relyingPartyId, PKCS10CertificationRequest csr) {
        RelyingPartyInstance relyingParty = getRelyingParty(relyingPartyId);

        X509Certificate certificate = certificateServiceClient.requestCertificate(
                csr,
                relyingParty.getWalletRelyingPartyService().getWalletRelyingParty().getOrgno(),
                relyingParty.getWalletRelyingPartyService().getWalletRelyingParty().getLegalName(),
                relyingParty.getWalletRelyingPartyService().getServiceTradeName(),
                properties.accessCertificateCaId()
                );

        AccessCertificate certificateEntity =
                new AccessCertificate(properties.accessCertificateCaId(), certificate, relyingParty);

        return accessCertificateRepository.saveAndFlush(certificateEntity);
    }

    private RelyingPartyInstance getRelyingParty(UUID relyingPartyId) {
        return relyingPartyRepository
                .findByIdAndActiveTrue(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate registree does not exist or is inactive"));
    }

    @Transactional
    public void revokeAccessCertificate(UUID certificateId, UUID relyingPartyId) {
        AccessCertificate certificate = accessCertificateRepository
                .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException(
                        "Certificate id and certificate holder does not match, or one of them does not exist"));

        if (certificate.getRevocationStatus() >= 0) {
            throw new RegisterServiceException("Certificate has already been revoked");
        }

        HttpStatusCode statusCode =
                certificateServiceClient.revokeCertificate(certificate.getSerialNo(), 0, certificate.getCaId());
        if (statusCode == HttpStatus.NO_CONTENT) {
            certificate.revoke(0);
            accessCertificateRepository.saveAndFlush(certificate);
            return;
        }
        log.warn("Error when certificate with id {} was tried revoked through CA. Status code from CA is {}",
                certificateId, statusCode);
        throw new BadRequestException("Error when in contact with CA");
    }

    @Transactional
    public void revokeIssuerCertificate(UUID certificateId, UUID relyingPartyId) {
        if (!relyingPartyRepository.existsById(relyingPartyId)) {
            throw new NotFoundException("Certificate holder with id " + relyingPartyId + " does not exist");
        }
        IssuerCertificate certificate = issuerCertificateRepository
                .findByIdAndEntitlementRelyingPartyInstanceId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException(
                        "Certificate id and certificate holder does not match, or one of them does not exist"));

        if (certificate.getRevocationStatus() >= 0) {
            throw new RegisterServiceException("Certificate has already been revoked");
        }

        HttpStatusCode statusCode =
                certificateServiceClient.revokeCertificate(certificate.getSerialNo(), 0, certificate.getCaId());
        if (statusCode == HttpStatus.NO_CONTENT) {
            certificate.revoke(0);
            issuerCertificateRepository.saveAndFlush(certificate);
            return;
        }
        log.warn("Error when certificate with id {} was tried revoked through CA. Status code from CA is {}",
                certificateId, statusCode);
        throw new BadRequestException("Error when in contact with CA");
    }

}
