package no.eudiw.rp.register.service.certificateservice;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.entity.certificates.*;
import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.data.repository.AccessCertificateRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.service.Converter;
import no.eudiw.rp.register.service.EntitlementService;
import no.eudiw.rp.register.service.certificateservice.config.RelyingPartyCertificateServiceProperties;
import no.eudiw.rp.register.service.exception.BadRequestException;
import no.eudiw.rp.register.service.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
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

    private final RestClient caRestClient;
    private final Converter converter;

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getCertificatesForRelyingParty(UUID relyingPartyId) {
        return new RelyingPartyCertificatesResource(
                relyingPartyRepository
                        .findById(relyingPartyId)
                        .orElseThrow(() -> new NotFoundException("Relying party not found"))
                        .getAccessCertificates()
                        .stream()
                        .map(converter::toResource)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getCertificate(UUID certificateId, UUID relyingPartyId) {
        if (!relyingPartyRepository.existsById(relyingPartyId)) {
            throw new NotFoundException("Access certificate holder does not exist");
        }
        return converter.toResource(
                accessCertificateRepository
                        .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                        .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getIssuerCertificate(UUID certificateId, UUID relyingPartyId) {

        Optional<RelyingPartyInstance> rp = relyingPartyRepository.findById(relyingPartyId);
        if (rp.isEmpty()) {
            throw new NotFoundException("Issuer certificate holder does not exist", "Failed to find relying party with uuid=%s for certificateId uuid=%s ".formatted(relyingPartyId, certificateId));
        }
        IssuerCertificate issuerCert = issuerCertificateRepository.findById(certificateId).orElseThrow(() -> new NotFoundException("Certificate does not exist", "Certificate with uuid %s does not exist in database".formatted(certificateId)));
        List<RelyingPartyEntitlement> relyingPartyEntitlements = rp.get().getRelyingPartyEntitlements();
        if (relyingPartyEntitlements == null || issuerCert.getEntitlement() == null) {
            throw new NotFoundException("No entitlements found for relying party for issuer certificate or in certificate", "No entitlements found for relying party with uuid %s for certificate uuid %s".formatted(relyingPartyId, certificateId));
        }
        boolean sameEntitlement = relyingPartyEntitlements.stream().anyMatch(e -> e.getEntitlement().equals(issuerCert.getEntitlement().getEntitlement()));
        if (!sameEntitlement) {
            throw new NotFoundException("Requested certificate does not belong to the specified relying party", "CertificateId uuid=%s with entitlement %s does not match entitlement in relying party with uuid=%s for ".formatted(certificateId, issuerCert.getEntitlement().getEntitlement(), relyingPartyId));
        }
        return converter.toResource(issuerCert);

    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificatesResource getAllIssuerCertificatesFromRelyingParty(UUID relyingPartyId) {
        return new RelyingPartyCertificatesResource(
                relyingPartyRepository
                        .findById(relyingPartyId)
                        .orElseThrow(() -> new NotFoundException("Relying party not found"))
                        .getIssuerCertificates()
                        .stream()
                        .map(converter::toResource)
                        .toList()
        );
    }

    @Transactional
    public RelyingPartyCertificateResource requestIssuerCertificate(
            UUID relyingPartyId,
            IssuerCsrResource csrResource) {
        RelyingPartyInstance relyingParty = getRelyingParty(relyingPartyId);

        RelyingPartyEntitlement relyingPartyEntitlement =
                relyingParty.getRelyingPartyEntitlement(csrResource.entitlement())
                        .orElseThrow(() -> new NotFoundException("RelyingPartyEntitlement does not exist"));

        String caId = entitlementService.getDefaultCaForEntitlementUri(csrResource.entitlement());

        if (caId == null) {
            throw new RegisterServiceException("Entitlement does not have a CA ID");
        }

        X509Certificate certificate = getCertificateFromCa(
                csrResource.csr(),
                relyingParty.getLegalEntity().getOrgno(),
                relyingParty.getLegalEntity().getName(),
                relyingParty.getTradeName(),
                caId);

        IssuerCertificate certificateEntity =
                new IssuerCertificate(certificate, caId, relyingPartyEntitlement);

        relyingPartyEntitlement.addIssuerCertificate(certificateEntity);

        return converter.toResource(issuerCertificateRepository.saveAndFlush(certificateEntity));
    }

    @Transactional
    public RelyingPartyCertificateResource requestAccessCertificateForRelyingParty(
            UUID relyingPartyId, RelyingPartyCsrResource csrResource) {
        RelyingPartyInstance relyingParty = getRelyingParty(relyingPartyId);

        X509Certificate certificate = getCertificateFromCa(
                csrResource.csr(),
                relyingParty.getLegalEntity().getOrgno(),
                relyingParty.getLegalEntity().getName(),
                relyingParty.getTradeName(),
                properties.accessCertificateCaId()
                );

        AccessCertificate certificateEntity =
                new AccessCertificate(properties.accessCertificateCaId(), certificate, relyingParty);

        accessCertificateRepository.saveAndFlush(certificateEntity);

        return converter.toResource(certificateEntity);
    }

    private RelyingPartyInstance getRelyingParty(UUID relyingPartyId) {
        return relyingPartyRepository
                .findByIdAndActiveTrue(relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate registree does not exist or is inactive"));
    }

    private X509Certificate getCertificateFromCa(
            PKCS10CertificationRequest csr,
            String orgno,
            String legalName,
            String tradeName,
            String caId) {
        String csrPemStr = PKCS10CertificationRequestConverter.convert(csr);
        String certificatePemStr =
                caRestClient.post()
                        .uri("/" + caId)
                        .body(RelyingPartyCertificateRequest
                                .builder()
                                .orgno(orgno)
                                .tradeName(tradeName)
                                .legalName(legalName)
                                .csr(csrPemStr)
                                .build())
                        .retrieve()
                        .toEntity(String.class)
                        .getBody();

        if (certificatePemStr == null) {
            throw new RegisterServiceException("Null body in ca-service success response");
        }

        return X509CertificateConverter.convert(certificatePemStr);
    }

    public HttpStatusCode revokeAccessCertificate(UUID certificateId, UUID relyingPartyId) {
        if (accessCertificateRepository.findByIdAndRelyingPartyId(certificateId, relyingPartyId).isEmpty()) {
            throw new NotFoundException("Certificate id and certificate holder does not match, or one of them " +
                                        "does not exist");
        }
        if (accessCertificateRepository.findById(certificateId).get().getRevocationStatus() >= 0) {
            throw new RegisterServiceException("Certificate has already been revoked");
        }
        HttpStatusCode statusCode = revocationContactWithCa(accessCertificateRepository.findById(
                certificateId).get().getSerialNo(), 0, accessCertificateRepository.findById(certificateId)
                .get().getCaId());
        if (statusCode == HttpStatus.valueOf(204)) {
            Optional<AccessCertificate> cert = accessCertificateRepository.findById(certificateId);
            cert.get().revoke(0);
            accessCertificateRepository.saveAndFlush(cert.get());
            return statusCode;
        }
        log.warn("Error when certificate with id {} was tried revoked through CA. Status code from CA is {}",
                certificateId, statusCode);
        throw new BadRequestException("Error when in contact with CA");
    }

    public HttpStatusCode revokeIssuerCertificate(UUID certificateId, UUID relyingPartyId) {
        if (!relyingPartyRepository.existsById(relyingPartyId)) {
            throw new NotFoundException("Certificate holder with id " + relyingPartyId + " does not exist");
        }
        if (!issuerCertificateRepository.existsById(certificateId)) {
            throw new NotFoundException("Certificate with id " + certificateId + " does not exist");
        }
        if (issuerCertificateRepository.findById(certificateId).get().getRevocationStatus() >= 0) {
            throw new RegisterServiceException("Certificate has already been revoked");
        }
        HttpStatusCode statusCode = revocationContactWithCa(issuerCertificateRepository.findById(
                certificateId).get().getSerialNo(), 0, issuerCertificateRepository.findById(certificateId)
                .get().getCaId());
        if (statusCode == HttpStatus.valueOf(204)) {
            Optional<IssuerCertificate> cert = issuerCertificateRepository.findById(certificateId);
            cert.get().revoke(0);
            issuerCertificateRepository.saveAndFlush(cert.get());
            return statusCode;
        }
        log.warn("Error when certificate with id {} was tried revoked through CA. Status code from CA is {}",
                certificateId, statusCode);
        throw new BadRequestException("Error when in contact with CA");
    }


    private HttpStatusCode revocationContactWithCa(String serialNumber, int reason, String caId) {
        return caRestClient.put()
                .uri("/" + caId)
                .body(RevocationRequest
                        .builder()
                        .serialNumber(serialNumber)
                        .reason(reason)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .getStatusCode();
    }
}
