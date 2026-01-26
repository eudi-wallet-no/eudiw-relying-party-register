package no.eudiw.rp.register.service.certificateservice;

import lombok.RequiredArgsConstructor;
import no.eudiw.rp.register.api.resource.certificates.IssuerCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCsrResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificateResource;
import no.eudiw.rp.register.api.resource.certificates.RelyingPartyCertificatesResource;
import no.eudiw.rp.register.data.entity.certificates.AccessCertificate;
import no.eudiw.rp.register.data.entity.certificates.IssuerCertificate;
import no.eudiw.rp.register.data.entity.certificates.PKCS10CertificationRequestConverter;
import no.eudiw.rp.register.data.entity.certificates.X509CertificateConverter;
import no.eudiw.rp.register.data.entity.*;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyEntitlement;
import no.eudiw.rp.register.data.entity.relyingparty.RelyingPartyInstance;
import no.eudiw.rp.register.data.repository.EntitlementRepository;
import no.eudiw.rp.register.data.repository.IssuerCertificateRepository;
import no.eudiw.rp.register.data.repository.AccessCertificateRepository;
import no.eudiw.rp.register.data.repository.RelyingPartyInstanceRepository;
import no.eudiw.rp.register.service.Converter;
import no.eudiw.rp.register.service.exception.NotFoundException;
import no.eudiw.rp.register.exception.RegisterServiceException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RelyingPartyCertificateService {

    private final RelyingPartyInstanceRepository relyingPartyRepository;

    private final EntitlementRepository entitlementRepository;
    private final AccessCertificateRepository accessCertificateRepository;
    private final IssuerCertificateRepository issuerCertificateRepository;

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
            throw new NotFoundException("Certificate holder does not exist");
        }
        return converter.toResource(
            accessCertificateRepository
                .findByIdAndRelyingPartyId(certificateId, relyingPartyId)
                .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
    }

    @Transactional(readOnly = true)
    public RelyingPartyCertificateResource getIssuerCertificate(UUID certificateId) {
        return converter.toResource(
            issuerCertificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("Certificate does not exist"))
        );
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

        RelyingPartyEntitlement relyingPartyEntitlement = relyingParty.getRelyingPartyEntitlement(csrResource.entitlement())
            .orElseThrow(() -> new NotFoundException("RelyingPartyEntitlement does not exist"));

        Entitlement entitlement = entitlementRepository.findByEntitlement(csrResource.entitlement())
            .orElseThrow(() -> new NotFoundException("Entitlement does not exist"));

        if (entitlement.getCaId() == null) {
            throw new RegisterServiceException("Entitlement does not have a CA ID");
        }

        X509Certificate certificate = getCertificateFromCa(
            csrResource.csr(),
            relyingParty.getLegalEntity().getOrgno(),
            relyingParty.getLegalEntity().getName(),
            relyingParty.getTradeName(),
            entitlement.getCaId());

        IssuerCertificate certificateEntity =
            new IssuerCertificate(certificate, relyingPartyEntitlement);

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
                "access");

        AccessCertificate certificateEntity =
            new AccessCertificate(certificate, relyingParty);

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
}
